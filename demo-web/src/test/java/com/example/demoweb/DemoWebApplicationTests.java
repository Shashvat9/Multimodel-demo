package com.example.demoweb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

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
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void logoutShouldRequireAuthentication() {
        ResponseEntity<String> response = new RestTemplate().postForEntity(
                "http://localhost:" + environment.getProperty("local.server.port") + "/api/auth/logout",
                null,
                String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
