package com.aml.gateway;

import com.aml.gateway.domain.FheResult;
import com.aml.gateway.service.FheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FheService.
 *
 * <p>Tests verify that:
 * <ul>
 *   <li>The FHE mock returns correct comparison results for trigger keywords.</li>
 *   <li>The service never throws for valid ciphertexts.</li>
 *   <li>Factory methods on FheResult produce correct state.</li>
 *   <li>Ciphertext validation works as expected.</li>
 * </ul>
 */
@DisplayName("FheService — Unit Tests")
class FheServiceTest {

    private FheService fheService;

    @BeforeEach
    void setUp() {
        fheService = new FheService();
        // Inject property values that would normally come from @Value
        ReflectionTestUtils.setField(fheService, "mockDelayMs", 1L);  // Fast for tests
        ReflectionTestUtils.setField(fheService, "encryptedThresholdLabel", "THRESHOLD_ENC_XYZ");
    }

    // ----------------------------------------------------------------
    // encryptedGreaterThanCheck — result correctness
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Normal ciphertext → WITHIN_THRESHOLD (false)")
    void normalCiphertext_returnsWithinThreshold() {
        FheResult result = fheService.encryptedGreaterThanCheck("ENC_ABC123");

        assertThat(result.isEncryptedComparisonResult()).isFalse();
        assertThat(result.getComparisonLabel()).isEqualTo("WITHIN_THRESHOLD");
        assertThat(result.getComputationTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ENC_LARGE_AMOUNT", "ENC_BIG_TX", "ENC_EXCEED_LIMIT", "ENC_OVER_THRESHOLD", "ENC_FLAG_ME"})
    @DisplayName("Trigger-keyword ciphertexts → EXCEEDS_THRESHOLD (true)")
    void triggerKeywordCiphertext_returnsExceedsThreshold(String ciphertext) {
        FheResult result = fheService.encryptedGreaterThanCheck(ciphertext);

        assertThat(result.isEncryptedComparisonResult()).isTrue();
        assertThat(result.getComparisonLabel()).isEqualTo("EXCEEDS_THRESHOLD");
    }

    @Test
    @DisplayName("Case-insensitive trigger detection")
    void caseInsensitiveTrigger() {
        FheResult lower = fheService.encryptedGreaterThanCheck("enc_large_amount");
        FheResult mixed = fheService.encryptedGreaterThanCheck("enc_Large_Amount");

        assertThat(lower.isEncryptedComparisonResult()).isTrue();
        assertThat(mixed.isEncryptedComparisonResult()).isTrue();
    }

    // ----------------------------------------------------------------
    // FheResult factory methods
    // ----------------------------------------------------------------

    @Test
    @DisplayName("FheResult.flagged() produces correct state")
    void fheResultFlagged_hasCorrectState() {
        FheResult flagged = FheResult.flagged(42L);

        assertThat(flagged.isEncryptedComparisonResult()).isTrue();
        assertThat(flagged.getComparisonLabel()).isEqualTo("EXCEEDS_THRESHOLD");
        assertThat(flagged.getComputationTimeMs()).isEqualTo(42L);
    }

    @Test
    @DisplayName("FheResult.withinLimits() produces correct state")
    void fheResultWithinLimits_hasCorrectState() {
        FheResult safe = FheResult.withinLimits(99L);

        assertThat(safe.isEncryptedComparisonResult()).isFalse();
        assertThat(safe.getComparisonLabel()).isEqualTo("WITHIN_THRESHOLD");
        assertThat(safe.getComputationTimeMs()).isEqualTo(99L);
    }

    // ----------------------------------------------------------------
    // Ciphertext validation
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Valid ciphertext passes validation")
    void validCiphertext_passesValidation() {
        assertThatNoException()
                .isThrownBy(() -> fheService.validateCiphertext("ENC_VALID123"));
    }

    @Test
    @DisplayName("Null ciphertext throws IllegalArgumentException")
    void nullCiphertext_throwsIllegalArgument() {
        assertThatThrownBy(() -> fheService.validateCiphertext(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or blank");
    }

    @Test
    @DisplayName("Blank ciphertext throws IllegalArgumentException")
    void blankCiphertext_throwsIllegalArgument() {
        assertThatThrownBy(() -> fheService.validateCiphertext("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Too-short ciphertext throws IllegalArgumentException")
    void tooShortCiphertext_throwsIllegalArgument() {
        assertThatThrownBy(() -> fheService.validateCiphertext("ENC"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too short");
    }
}
