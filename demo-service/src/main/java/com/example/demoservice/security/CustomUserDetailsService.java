package com.example.demoservice.security;

import com.example.demorepository.entity.User;
import com.example.demorepository.repository.UserRepository;
import com.example.demoservice.userindex.IndexedUser;
import com.example.demoservice.userindex.UserInMemoryIndexService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserInMemoryIndexService userInMemoryIndexService;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserInMemoryIndexService userInMemoryIndexService) {
        this.userRepository = userRepository;
        this.userInMemoryIndexService = userInMemoryIndexService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        IndexedUser indexedUser = userInMemoryIndexService.findByUsername(username)
                .orElseGet(() -> {
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
                    userInMemoryIndexService.indexUser(user);
                    return IndexedUser.fromUser(user);
                });

        return org.springframework.security.core.userdetails.User.builder()
                .username(indexedUser.username())
                .password(indexedUser.password())
                .authorities(List.of(new SimpleGrantedAuthority(indexedUser.role())))
                .build();
    }
}
