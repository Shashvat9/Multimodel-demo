package com.example.demoservice.userindex;

import com.example.demorepository.entity.User;
import com.example.demorepository.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class UserInMemoryIndexService implements Closeable {

    private final UserRepository userRepository;
    private final Analyzer analyzer = new StandardAnalyzer();
    private final Directory directory = new ByteBuffersDirectory();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, IndexedUser> usersByUsername = new ConcurrentHashMap<>();

    private IndexWriter indexWriter;
    private DirectoryReader directoryReader;

    public UserInMemoryIndexService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void initialize() {
        lock.writeLock().lock();
        try {
            this.indexWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer));
            rebuildIndexInternal();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize in-memory Lucene user index.", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<IndexedUser> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(normalize(username)));
    }

    public List<IndexedUser> searchByUsernameOrEmailPrefix(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }

        lock.readLock().lock();
        try {
            if (directoryReader == null) {
                return List.of();
            }

            String normalizedQuery = normalize(query);
            BooleanQuery luceneQuery = new BooleanQuery.Builder()
                    .add(new PrefixQuery(new Term("username", normalizedQuery)), BooleanClause.Occur.SHOULD)
                    .add(new PrefixQuery(new Term("email", normalizedQuery)), BooleanClause.Occur.SHOULD)
                    .build();

            IndexSearcher searcher = new IndexSearcher(directoryReader);
            ScoreDoc[] hits = searcher.search(luceneQuery, limit).scoreDocs;
            List<IndexedUser> result = new ArrayList<>(hits.length);
            for (ScoreDoc hit : hits) {
                Document document = searcher.doc(hit.doc);
                IndexedUser indexedUser = usersByUsername.get(document.get("usernameStoredNormalized"));
                if (indexedUser != null) {
                    result.add(indexedUser);
                }
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to search users in in-memory Lucene index.", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void indexUser(User user) {
        lock.writeLock().lock();
        try {
            String normalizedUsername = normalize(user.getUsername());
            usersByUsername.put(normalizedUsername, IndexedUser.fromUser(user));

            indexWriter.updateDocument(new Term("username", normalizedUsername), toDocument(user));
            indexWriter.commit();
            refreshReaderInternal();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to index user in in-memory Lucene index.", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void rebuildIndexInternal() throws IOException {
        usersByUsername.clear();
        indexWriter.deleteAll();

        for (User user : userRepository.findAll()) {
            usersByUsername.put(normalize(user.getUsername()), IndexedUser.fromUser(user));
            indexWriter.addDocument(toDocument(user));
        }

        indexWriter.commit();
        refreshReaderInternal();
    }

    private void refreshReaderInternal() throws IOException {
        if (directoryReader == null) {
            directoryReader = DirectoryReader.open(indexWriter);
            return;
        }

        DirectoryReader updatedReader = DirectoryReader.openIfChanged(directoryReader, indexWriter);
        if (updatedReader != null) {
            directoryReader.close();
            directoryReader = updatedReader;
        }
    }

    private static Document toDocument(User user) {
        Document document = new Document();
        document.add(new StringField("username", normalize(user.getUsername()), Field.Store.NO));
        document.add(new StringField("email", normalize(user.getEmail()), Field.Store.NO));
        document.add(new StoredField("usernameStoredNormalized", normalize(user.getUsername())));
        return document;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    @Override
    @PreDestroy
    public void close() {
        lock.writeLock().lock();
        try {
            if (directoryReader != null) {
                directoryReader.close();
            }
            if (indexWriter != null) {
                indexWriter.close();
            }
            analyzer.close();
            directory.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close in-memory Lucene user index resources.", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
