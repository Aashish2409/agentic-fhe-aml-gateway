package com.aml.gateway.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AmlDecision — The API response object returned to the calling client.
 *
 * <p>Contains the compliance verdict, audit metadata, and — when an FHE
 * check was performed — the opaque comparison result.</p>
 *
 * <p>Note: The transaction amount (ciphertext) is intentionally EXCLUDED
 * from this response to avoid any accidental echo of sensitive data.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmlDecision {

    // ----------------------------------------------------------------
    // Verdict
    // ----------------------------------------------------------------

    /**
     * Final AML compliance verdict.
     * <ul>
     *   <li>{@code SAFE}    — transaction is within allowed parameters.</li>
     *   <li>{@code FLAGGED} — transaction exceeded encrypted AML threshold.</li>
     *   <li>{@code REVIEW}  — human review required (CRITICAL risk level).</li>
     *   <li>{@code ERROR}   — processing failure; see message field.</li>
     * </ul>
     */
    private String verdict;

    /** Human-readable explanation of how the verdict was reached. */
    private String message;

    // ----------------------------------------------------------------
    // Audit trail
    // ----------------------------------------------------------------

    /** The originating account from the request (mirrored for audit). */
    private String originAccount;

    /** Risk level that was evaluated. */
    private RiskLevel riskLevel;

    /** Whether the AI agent invoked the FHE computation tool. */
    private boolean fheCheckPerformed;

    /**
     * Result of the FHE encrypted comparison, present only when
     * {@link #fheCheckPerformed} is {@code true}.
     * Amount plaintext is NEVER included.
     */
    private FheResult fheResult;

    /** Timestamp at which the gateway processed this request. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;

    /** Gateway version for debugging and audit purposes. */
    @Builder.Default
    private String gatewayVersion = "1.0.0";
}
