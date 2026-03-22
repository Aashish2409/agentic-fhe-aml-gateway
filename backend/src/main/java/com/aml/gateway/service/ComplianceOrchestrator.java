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

/**
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║               ComplianceOrchestrator — Core Business Logic       ║
 * ╠═══════════════════════════════════════════════════════════════════╣
 * ║                                                                   ║
 * ║  Orchestration Responsibilities:                                  ║
 * ║  1. Validate the incoming TransactionRequest.                    ║
 * ║  2. Delegate evaluation to the AML AI Agent.                    ║
 * ║  3. Parse the agent's response into a structured AmlDecision.   ║
 * ║  4. Optionally retrieve the FheResult for the API response.     ║
 * ║  5. Return a complete AmlDecision to the REST controller.       ║
 * ║                                                                   ║
 * ║  Architecture note:                                               ║
 * ║  The orchestrator is deliberately thin — business rules live     ║
 * ║  in the agent's system prompt, and computation lives in         ║
 * ║  FheService. The orchestrator only coordinates.                  ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceOrchestrator {

    private final AmlAgent amlAgent;
    private final FheService fheService;

    // ----------------------------------------------------------------
    // Public orchestration method
    // ----------------------------------------------------------------

    /**
     * Orchestrates the full AML compliance evaluation workflow.
     *
     * <p>Flow:
     * <ol>
     *   <li>Log the incoming request (metadata only — ciphertext is masked).</li>
     *   <li>For LOW risk: short-circuit without LLM call for efficiency.</li>
     *   <li>For HIGH/CRITICAL: delegate to AI agent (agent calls FHE tool internally).</li>
     *   <li>Parse the agent's verdict string into a structured {@link AmlDecision}.</li>
     * </ol>
     *
     * @param request the validated transaction request
     * @return a fully populated {@link AmlDecision}
     */
    public AmlDecision evaluate(TransactionRequest request) {

        log.info("ComplianceOrchestrator: evaluating transaction — account: [{}], riskLevel: [{}]",
                request.getOriginAccount(), request.getRiskLevel());

        // ---------------------------------------------------------
        // Fast path: LOW risk transactions — skip LLM call entirely
        // This is a deterministic business rule, not an AI decision.
        // Saves latency (~500ms LLM round-trip) for the majority of traffic.
        // ---------------------------------------------------------
        if (request.getRiskLevel() == RiskLevel.LOW) {
            log.info("ComplianceOrchestrator: LOW risk fast path — returning SAFE immediately");
            return buildSafeDecision(request, "Low-risk transaction auto-approved — FHE check not required.");
        }

        // ---------------------------------------------------------
        // Standard path: delegate to AI agent
        // The agent will decide whether to invoke the FHE tool
        // based on the system prompt rules.
        // ---------------------------------------------------------
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

        // ---------------------------------------------------------
        // Parse agent verdict and construct the API response
        // ---------------------------------------------------------
        return parseAgentVerdict(agentVerdict, request);
    }

    // ----------------------------------------------------------------
    // Verdict parsing
    // ----------------------------------------------------------------

    /**
     * Parses the structured verdict string from the AI agent.
     *
     * <p>Expected format: {@code "VERDICT: X | REASON: Y"}</p>
     * where X is SAFE, FLAGGED, or REVIEW.
     *
     * <p>For HIGH/CRITICAL risk paths where FHE was invoked, we re-run
     * the FHE check to populate the FheResult in the response.
     * Note: this second call is cheap (mock), and in production would
     * be replaced by passing the result through the agent context.</p>
     */
    private AmlDecision parseAgentVerdict(String agentResponse, TransactionRequest request) {

        String upperResponse = agentResponse.toUpperCase();
        boolean isFheRisk = request.getRiskLevel() == RiskLevel.HIGH
                || request.getRiskLevel() == RiskLevel.CRITICAL;

        // Determine if FHE was likely performed based on risk level
        // (agent is instructed to always call it for HIGH/CRITICAL)
        FheResult fheResult = null;
        if (isFheRisk) {
            fheResult = fheService.encryptedGreaterThanCheck(request.getCiphertextAmount());
        }

        // Extract verdict keyword from agent response
        String verdict = extractVerdict(upperResponse);
        String reason = extractReason(agentResponse);

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

    /**
     * Extracts the SAFE / FLAGGED / REVIEW verdict keyword from the agent response.
     * Falls back to "REVIEW" if parsing fails (conservative compliance stance).
     */
    private String extractVerdict(String upperResponse) {
        if (upperResponse.contains("VERDICT: SAFE")) return "SAFE";
        if (upperResponse.contains("VERDICT: FLAGGED")) return "FLAGGED";
        if (upperResponse.contains("VERDICT: REVIEW")) return "REVIEW";

        // If verdict is ambiguous, default to REVIEW (safer for compliance)
        log.warn("ComplianceOrchestrator: could not parse verdict from agent response — defaulting to REVIEW");
        return "REVIEW";
    }

    /**
     * Extracts the REASON text from the agent's formatted response string.
     */
    private String extractReason(String response) {
        int reasonIdx = response.indexOf("REASON:");
        if (reasonIdx >= 0) {
            return response.substring(reasonIdx + "REASON:".length()).trim();
        }
        // Fallback: return the whole response as the message
        return response;
    }

    // ----------------------------------------------------------------
    // Response builders
    // ----------------------------------------------------------------

    private AmlDecision buildSafeDecision(TransactionRequest request, String message) {
        return AmlDecision.builder()
                .verdict("SAFE")
                .message(message)
                .originAccount(request.getOriginAccount())
                .riskLevel(request.getRiskLevel())
                .fheCheckPerformed(false)
                .fheResult(null)
                .processedAt(LocalDateTime.now())
                .build();
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
