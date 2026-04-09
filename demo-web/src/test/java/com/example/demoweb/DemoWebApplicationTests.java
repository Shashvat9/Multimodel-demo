package com.example.demoweb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoWebApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    @Test
    void pingEndpointShouldBePublic() {
        ResponseEntity<String> response = new RestTemplate().getForEntity(
                "http://localhost:" + environment.getProperty("local.server.port") + "/ping",
                String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void logoutShouldRequireAuthentication() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> new RestTemplate().postForEntity(
                        "http://localhost:" + environment.getProperty("local.server.port") + "/api/auth/logout",
                        null,
                        String.class
                )
        );
        assertTrue(Set.of(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN).contains(exception.getStatusCode()));
    }
}
