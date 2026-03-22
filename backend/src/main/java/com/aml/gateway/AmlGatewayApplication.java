package com.aml.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication(
        scanBasePackages = {
                "com.aml.gateway.controller",
                "com.aml.gateway.service",
                "com.aml.gateway.tools",
                "com.aml.gateway.config"
        }
)
public class AmlGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmlGatewayApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("""
                ╔══════════════════════════════════════════════════════╗
                ║       Agentic-FHE AML Gateway — STARTED             ║
                ╠══════════════════════════════════════════════════════╣
                ║  REST API  : http://localhost:8080/api/v1/aml        ║
                ║  FHE Mode  : MOCK (prototype)                        ║
                ║  AI Agent  : LangChain4j                             ║
                ╚══════════════════════════════════════════════════════╝
                """);
    }
}