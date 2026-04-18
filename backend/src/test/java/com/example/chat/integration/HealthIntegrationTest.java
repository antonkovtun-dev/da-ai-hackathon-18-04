package com.example.chat.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthIntegrationTest extends IntegrationTestBase {

    @Test
    void health_returns_200_and_status_up() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "up");
    }
}
