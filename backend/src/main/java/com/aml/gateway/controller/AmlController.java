package com.aml.gateway.controller;

import com.aml.gateway.domain.AmlDecision;
import com.aml.gateway.domain.TransactionRequest;
import com.aml.gateway.service.ComplianceOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/aml")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AmlController {

    private final ComplianceOrchestrator complianceOrchestrator;

    @PostMapping(value = "/analyze", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AmlDecision> analyzeTransaction(@Valid @RequestBody TransactionRequest request) {
        log.info("AmlController: POST /analyze — account: [{}], riskLevel: [{}]",
                request.getOriginAccount(), request.getRiskLevel());
        AmlDecision decision = complianceOrchestrator.evaluate(request);
        log.info("AmlController: verdict: [{}] for account: [{}]",
                decision.getVerdict(), request.getOriginAccount());
        return ResponseEntity.ok(decision);
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "gateway", "Agentic OPE AML Gateway",
                "version", "1.0.0",
                "capabilities", Map.of(
                        "encryptionScheme", "Order Preserving Encryption (OPE)",
                        "aiAgent", "LangChain4j + Groq Llama",
                        "privacyGuarantee", "Transaction amounts never decrypted"
                )
        ));
    }
}
