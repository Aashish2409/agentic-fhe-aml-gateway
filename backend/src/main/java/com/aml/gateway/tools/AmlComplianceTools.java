package com.aml.gateway.tools;

import com.aml.gateway.domain.FheResult;
import com.aml.gateway.service.FheService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  AmlComplianceTools — LangChain4j Tool Definitions              │
 * ├──────────────────────────────────────────────────────────────────┤
 * │  These methods are exposed to the AML AI Agent as callable      │
 * │  tools via LangChain4j's @Tool annotation.                      │
 * │                                                                  │
 * │  The agent (LLM) decides WHEN to call each tool based on the   │
 * │  transaction metadata in its context window.                    │
 * │                                                                  │
 * │  Design principle: Tools are the ONLY bridge between the AI     │
 * │  agent and the FHE computation layer. The agent never receives  │
 * │  or processes the raw ciphertext — it only passes the opaque   │
 * │  reference string through.                                       │
 * └──────────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmlComplianceTools {

    private final FheService fheService;

    // ----------------------------------------------------------------
    // Tool: FHE Encrypted Threshold Check
    // ----------------------------------------------------------------

    /**
     * Performs an FHE-encrypted AML threshold comparison.
     *
     * <p>Called by the AI agent when riskLevel is HIGH or CRITICAL.
     * The ciphertext is passed opaquely — the agent does not know
     * (and cannot infer) the underlying monetary amount.</p>
     *
     * <p>The tool returns a human-readable result string which the agent
     * incorporates into its final compliance verdict.</p>
     *
     * @param ciphertext the FHE-encrypted transaction amount from the client
     * @return a compliance signal string: "EXCEEDS_THRESHOLD" or "WITHIN_THRESHOLD"
     */
    @Tool("""
            Perform a privacy-preserving AML threshold check using Fully Homomorphic Encryption.
            This tool compares the encrypted transaction amount against the encrypted AML threshold
            WITHOUT decrypting either value. Use this tool when riskLevel is HIGH or CRITICAL.
            Pass the exact ciphertextAmount from the transaction metadata as the parameter.
            Returns: EXCEEDS_THRESHOLD (flag the transaction) or WITHIN_THRESHOLD (transaction is safe).
            """)
    public String checkThresholdFhe(String ciphertext) {

        log.info("AmlComplianceTools: AI agent invoked checkThresholdFhe — routing to FHE engine");

        try {
            // Validate the ciphertext structure before processing
            fheService.validateCiphertext(ciphertext);

            // Execute the encrypted comparison — amount is NEVER decrypted
            FheResult result = fheService.encryptedGreaterThanCheck(ciphertext);

            String complianceSignal = result.isEncryptedComparisonResult()
                    ? "EXCEEDS_THRESHOLD"
                    : "WITHIN_THRESHOLD";

            log.info("AmlComplianceTools: FHE check complete — signal: {} (computed in {}ms)",
                    complianceSignal, result.getComputationTimeMs());

            return complianceSignal;

        } catch (Exception e) {
            log.error("AmlComplianceTools: FHE check failed for ciphertext — error: {}", e.getMessage());
            // Return a safe-fail signal; the orchestrator will handle escalation
            return "FHE_CHECK_FAILED";
        }
    }

    // ----------------------------------------------------------------
    // Tool: Regulatory Escalation Flag
    // ----------------------------------------------------------------

    /**
     * Escalates a transaction for manual human review.
     *
     * <p>The AI agent may call this when riskLevel is CRITICAL or when
     * the FHE check returns EXCEEDS_THRESHOLD with additional context
     * suggesting unusual patterns (multiple same-day submissions, etc.).</p>
     *
     * @param originAccount the account to flag for review
     * @param reason        the agent's stated reason for escalation
     * @return confirmation string for the agent's context
     */
    @Tool("""
            Escalate a transaction for manual human compliance review.
            Use when riskLevel is CRITICAL or when additional suspicious patterns are detected.
            Provide the account identifier and a clear reason for escalation.
            Returns confirmation that the escalation has been recorded.
            """)
    public String escalateForManualReview(String originAccount, String reason) {

        log.warn("AmlComplianceTools: MANUAL REVIEW ESCALATED — account: [{}] reason: [{}]",
                originAccount, reason);

        // In production: write to an audit database, send to compliance team queue, etc.
        // For prototype: log the escalation and return confirmation
        return String.format(
                "ESCALATION_RECORDED: Account %s has been queued for manual review. Reason: %s",
                originAccount, reason
        );
    }
}
