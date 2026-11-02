package com.devops.taskmanager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: starts the real server on a random port and calls it over HTTP.
 * Run by the maven-failsafe-plugin in the "verify" phase.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationIT {

    @Autowired
    private TestRestTemplate http;

    @Test
    void healthEndpointIsUp() {
        ResponseEntity<String> res = http.getForEntity("/actuator/health", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void homePageIsServed() {
        ResponseEntity<String> res = http.getForEntity("/", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("DevOps Task Manager");
    }
}
