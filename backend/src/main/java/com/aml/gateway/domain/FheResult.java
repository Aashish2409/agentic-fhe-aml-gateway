package com.aml.gateway.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  FheResult — Outcome of an FHE homomorphic comparison           │
 * ├──────────────────────────────────────────────────────────────────┤
 * │  The only information that leaves the FHE computation domain    │
 * │  is a single boolean: did the encrypted amount exceed the       │
 * │  encrypted threshold?                                            │
 * │                                                                  │
 * │  The plaintext amount is NEVER revealed — not to the AI agent,  │
 * │  not to the REST layer, not to logs.                            │
 * │                                                                  │
 * │  How Zama TFHE achieves this in production:                     │
 * │  1. Server holds an encrypted threshold T_enc.                  │
 * │  2. Client submits encrypted amount A_enc.                      │
 * │  3. TFHE evaluates: result_enc = A_enc > T_enc  (homomorphic). │
 * │  4. Server decrypts only result_enc → boolean.                  │
 * │     At no point is A_enc or T_enc decrypted individually.      │
 * └──────────────────────────────────────────────────────────────────┘
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FheResult {

    /**
     * Result of the encrypted greater-than comparison:
     * {@code true}  → encrypted amount EXCEEDS the AML threshold → FLAG
     * {@code false} → encrypted amount is BELOW the threshold    → within limits
     *
     * <p>This boolean is the ONLY value produced by the FHE circuit
     * that is visible to the application layer.</p>
     */
    private boolean encryptedComparisonResult;

    /**
     * Human-readable label for logging and API response construction.
     * Derived from {@link #encryptedComparisonResult}.
     */
    private String comparisonLabel;

    /**
     * Simulated execution time of the FHE circuit in milliseconds.
     * In production, TFHE bootstrapping can take 10–100 ms per gate.
     * Surfaced here for observability and capacity planning.
     */
    private long computationTimeMs;

    // ---------------------------------------------------------------
    // Factory helpers
    // ---------------------------------------------------------------

    /** Convenience factory: amount exceeded the threshold. */
    public static FheResult flagged(long computationTimeMs) {
        return FheResult.builder()
                .encryptedComparisonResult(true)
                .comparisonLabel("EXCEEDS_THRESHOLD")
                .computationTimeMs(computationTimeMs)
                .build();
    }

    /** Convenience factory: amount is within the threshold. */
    public static FheResult withinLimits(long computationTimeMs) {
        return FheResult.builder()
                .encryptedComparisonResult(false)
                .comparisonLabel("WITHIN_THRESHOLD")
                .computationTimeMs(computationTimeMs)
                .build();
    }
}
