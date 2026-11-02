package com.devops.taskmanager.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Shows which build is running, so you can see each pipeline deployment land. */
@RestController
public class InfoController {

    private final String version;
    private final String buildNumber;
    private final String environment;

    public InfoController(@Value("${app.version}") String version,
                          @Value("${app.build-number}") String buildNumber,
                          @Value("${app.environment}") String environment) {
        this.version = version;
        this.buildNumber = buildNumber;
        this.environment = environment;
    }

    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of(
                "app", "DevOps Task Manager",
                "version", version,
                "build", buildNumber,
                "environment", environment);
    }
}
