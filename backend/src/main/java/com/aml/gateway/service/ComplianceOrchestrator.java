package com.aml.gateway.service;

import com.aml.gateway.agent.AmlAgent;
import com.aml.gateway.domain.AmlDecision;
import com.aml.gateway.domain.FheResult;
import com.aml.gateway.domain.RiskLevel;
import com.aml.gateway.domain.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceOrchestrator {

    private final AmlAgent amlAgent;
    private final FheService fheService;

    /**
     * Orchestrates full AML compliance evaluation.
     * Every transaction — regardless of risk level — goes through the AI agent.
     * This ensures explainability, auditability, and consistent reasoning
     * across all compliance decisions.
     */
    public AmlDecision evaluate(TransactionRequest request) {

        log.info("ComplianceOrchestrator: evaluating transaction — account: [{}], riskLevel: [{}]",
                request.getOriginAccount(), request.getRiskLevel());

        // All transactions routed through AI agent — no shortcuts
        log.info("ComplianceOrchestrator: routing to AML AI Agent — riskLevel: {}", request.getRiskLevel());

        String agentVerdict;
        try {
            agentVerdict = amlAgent.evaluateTransaction(
                    request.getOriginAccount(),
                    request.getTimestamp().toString(),
                    request.getRiskLevel().name(),
                    request.getCiphertextAmount()
            );
        } catch (Exception e) {
            log.error("ComplianceOrchestrator: AI agent invocation failed — {}", e.getMessage(), e);
            return buildErrorDecision(request, "AML agent unavailable: " + e.getMessage());
        }

        log.info("ComplianceOrchestrator: agent returned verdict — [{}]", agentVerdict);

        return parseAgentVerdict(agentVerdict, request);
    }

    private AmlDecision parseAgentVerdict(String agentResponse, TransactionRequest request) {

        String upperResponse = agentResponse.toUpperCase();

        // FHE check performed for HIGH and CRITICAL risk
        boolean isFheRisk = request.getRiskLevel() == RiskLevel.HIGH
                || request.getRiskLevel() == RiskLevel.CRITICAL;

        FheResult fheResult = null;
        if (isFheRisk) {
            fheResult = fheService.encryptedGreaterThanCheck(request.getCiphertextAmount());
        }

        String verdict = extractVerdict(upperResponse);
        String reason  = extractReason(agentResponse);

        return AmlDecision.builder()
                .verdict(verdict)
                .message(reason)
                .originAccount(request.getOriginAccount())
                .riskLevel(request.getRiskLevel())
                .fheCheckPerformed(isFheRisk)
                .fheResult(fheResult)
                .processedAt(LocalDateTime.now())
                .build();
    }

    private String extractVerdict(String upperResponse) {
        if (upperResponse.contains("VERDICT: SAFE"))    return "SAFE";
        if (upperResponse.contains("VERDICT: FLAGGED")) return "FLAGGED";
        if (upperResponse.contains("VERDICT: REVIEW"))  return "REVIEW";
        log.warn("ComplianceOrchestrator: could not parse verdict — defaulting to REVIEW");
        return "REVIEW";
    }

    private String extractReason(String response) {
        int idx = response.indexOf("REASON:");
        if (idx >= 0) return response.substring(idx + "REASON:".length()).trim();
        return response;
    }

    private AmlDecision buildErrorDecision(TransactionRequest request, String errorMessage) {
        return AmlDecision.builder()
                .verdict("ERROR")
                .message(errorMessage)
                .originAccount(request.getOriginAccount())
                .riskLevel(request.getRiskLevel())
                .fheCheckPerformed(false)
                .fheResult(null)
                .processedAt(LocalDateTime.now())
                .build();
    }
}