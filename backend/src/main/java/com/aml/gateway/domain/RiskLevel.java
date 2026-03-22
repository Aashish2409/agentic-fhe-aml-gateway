package com.aml.gateway.domain;

/**
 * Represents the risk classification of a submitted transaction.
 *
 * <p>Risk level is provided unencrypted by the submitting institution
 * and is used by the AI agent to decide whether to invoke the FHE
 * threshold check tool. The actual transaction amount remains encrypted.</p>
 *
 * <ul>
 *   <li>{@link #LOW}      — Agent returns SAFE without FHE computation.</li>
 *   <li>{@link #MEDIUM}   — Agent may or may not invoke FHE (model discretion).</li>
 *   <li>{@link #HIGH}     — Agent MUST invoke FHE encrypted threshold check.</li>
 *   <li>{@link #CRITICAL} — Agent MUST invoke FHE; triggers additional audit flag.</li>
 * </ul>
 */
public enum RiskLevel {

    /** Transaction passes initial screening — no FHE check required. */
    LOW,

    /** Borderline transaction — AI agent decides based on context. */
    MEDIUM,

    /**
     * Elevated-risk transaction.
     * The AI agent system prompt mandates an FHE threshold comparison.
     */
    HIGH,

    /**
     * Maximum risk classification.
     * FHE check is mandatory; result is escalated for manual review.
     */
    CRITICAL
}
