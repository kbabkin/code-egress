package com.bt.code.egress;

import org.springframework.boot.SpringApplication;

/**
 * Same as App with default scan project from test resources.
 */
public class TestApp {
    public static void main(String[] args) {
        System.setProperty("scan.project", "target/scan-project");
        System.setProperty("scan.config", "src/test/resources/config");

        SpringApplication.run(App.class, args);
    }
}
