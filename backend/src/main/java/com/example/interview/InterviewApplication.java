package com.example.interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point — AI Interview practice application.
 *
 * Architecture: Java Backend owns schema `app`, business rules, JWT auth.
 * Python AI Service owns schema `rag`, embeddings, LLM calls.
 * Frontend (React/Vite) talks only to this service via /api/v1.
 *
 * SRS §10.1, NFR-12, NFR-30.
 */
@SpringBootApplication
public class InterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewApplication.class, args);
    }
}
