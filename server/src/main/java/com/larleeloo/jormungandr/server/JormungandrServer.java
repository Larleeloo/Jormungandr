package com.larleeloo.jormungandr.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Jörmungandr Multiplayer Server
 *
 * A WebSocket-based game server that replaces the Google Apps Script backend
 * with real-time (<25ms) communication for up to 25 concurrent players.
 *
 * Designed to run on a single Google Cloud E2 (or AWS t4g) micro instance.
 */
@SpringBootApplication
@EnableScheduling
public class JormungandrServer {

    public static void main(String[] args) {
        SpringApplication.run(JormungandrServer.class, args);
    }
}
