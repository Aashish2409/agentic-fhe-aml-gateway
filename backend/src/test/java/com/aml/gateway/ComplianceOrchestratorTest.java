package com.aml.gateway;

import com.aml.gateway.agent.AmlAgent;
import com.aml.gateway.domain.AmlDecision;
import com.aml.gateway.domain.FheResult;
import com.aml.gateway.domain.RiskLevel;
import com.aml.gateway.domain.TransactionRequest;
import com.aml.gateway.service.ComplianceOrchestrator;
import com.aml.gateway.service.FheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ComplianceOrchestrator.
 *
 * <p>The AI agent and FHE service are mocked to isolate orchestration logic.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComplianceOrchestrator — Unit Tests")
class ComplianceOrchestratorTest {

    @Mock
    private AmlAgent amlAgent;

    @Mock
    private FheService fheService;

    @InjectMocks
    private ComplianceOrchestrator orchestrator;

    private TransactionRequest lowRiskRequest;
    private TransactionRequest highRiskRequest;
    private TransactionRequest criticalRiskRequest;

    @BeforeEach
    void setUp() {
        lowRiskRequest = TransactionRequest.builder()
                .originAccount("ACC-001")
                .timestamp(LocalDateTime.of(2026, 1, 10, 10, 0, 0))
                .riskLevel(RiskLevel.LOW)
                .ciphertextAmount("ENC_SMALL_AMOUNT")
                .build();

        highRiskRequest = TransactionRequest.builder()
                .originAccount("ACC-002")
                .timestamp(LocalDateTime.of(2026, 1, 10, 14, 30, 0))
                .riskLevel(RiskLevel.HIGH)
                .ciphertextAmount("ENC_LARGE_TX_123")
                .build();

        criticalRiskRequest = TransactionRequest.builder()
                .originAccount("ACC-003")
                .timestamp(LocalDateTime.of(2026, 1, 10, 3, 0, 0))
                .riskLevel(RiskLevel.CRITICAL)
                .ciphertextAmount("ENC_EXCEED_LIMIT")
                .build();
    }

    // ----------------------------------------------------------------
    // LOW risk fast path
    // ----------------------------------------------------------------

    @Test
    @DisplayName("LOW risk: fast-path returns SAFE without calling agent")
    void lowRisk_returnsSafeWithoutAgentCall() {
        AmlDecision decision = orchestrator.evaluate(lowRiskRequest);

        assertThat(decision.getVerdict()).isEqualTo("SAFE");
        assertThat(decision.isFheCheckPerformed()).isFalse();
        assertThat(decision.getFheResult()).isNull();
        assertThat(decision.getOriginAccount()).isEqualTo("ACC-001");
        assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.LOW);

        // AI agent must NOT be called for LOW risk
        verifyNoInteractions(amlAgent);
    }

    // ----------------------------------------------------------------
    // HIGH risk path
    // ----------------------------------------------------------------

    @Test
    @DisplayName("HIGH risk + agent returns FLAGGED → decision is FLAGGED")
    void highRisk_agentReturnsFlagged_decisionIsFlagged() {
        given(amlAgent.evaluateTransaction(anyString(), anyString(), anyString(), anyString()))
                .willReturn("VERDICT: FLAGGED | REASON: Encrypted amount exceeds AML threshold.");
        given(fheService.encryptedGreaterThanCheck(anyString()))
                .willReturn(FheResult.flagged(55L));

        AmlDecision decision = orchestrator.evaluate(highRiskRequest);

        assertThat(decision.getVerdict()).isEqualTo("FLAGGED");
        assertThat(decision.isFheCheckPerformed()).isTrue();
        assertThat(decision.getFheResult()).isNotNull();
        assertThat(decision.getFheResult().isEncryptedComparisonResult()).isTrue();
        assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    @DisplayName("HIGH risk + agent returns SAFE → decision is SAFE")
    void highRisk_agentReturnsSafe_decisionIsSafe() {
        given(amlAgent.evaluateTransaction(anyString(), anyString(), anyString(), anyString()))
                .willReturn("VERDICT: SAFE | REASON: High-risk transaction passed encrypted threshold check.");
        given(fheService.encryptedGreaterThanCheck(anyString()))
                .willReturn(FheResult.withinLimits(30L));

        AmlDecision decision = orchestrator.evaluate(highRiskRequest);

        assertThat(decision.getVerdict()).isEqualTo("SAFE");
        assertThat(decision.isFheCheckPerformed()).isTrue();
    }

    // ----------------------------------------------------------------
    // CRITICAL risk path
    // ----------------------------------------------------------------

    @Test
    @DisplayName("CRITICAL risk → decision is REVIEW, FHE performed")
    void criticalRisk_decisionIsReview() {
        given(amlAgent.evaluateTransaction(anyString(), anyString(), anyString(), anyString()))
                .willReturn("VERDICT: REVIEW | REASON: Critical-risk transaction escalated for manual review.");
        given(fheService.encryptedGreaterThanCheck(anyString()))
                .willReturn(FheResult.flagged(70L));

        AmlDecision decision = orchestrator.evaluate(criticalRiskRequest);

        assertThat(decision.getVerdict()).isEqualTo("REVIEW");
        assertThat(decision.isFheCheckPerformed()).isTrue();
        assertThat(decision.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
    }

    // ----------------------------------------------------------------
    // Error handling
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Agent throws exception → decision is ERROR")
    void agentThrowsException_returnsErrorDecision() {
        given(amlAgent.evaluateTransaction(anyString(), anyString(), anyString(), anyString()))
                .willThrow(new RuntimeException("LLM API timeout"));

        AmlDecision decision = orchestrator.evaluate(highRiskRequest);

        assertThat(decision.getVerdict()).isEqualTo("ERROR");
        assertThat(decision.getMessage()).contains("LLM API timeout");
        assertThat(decision.isFheCheckPerformed()).isFalse();
    }

    @Test
    @DisplayName("Unparseable agent response → defaults to REVIEW")
    void unparseableAgentResponse_defaultsToReview() {
        given(amlAgent.evaluateTransaction(anyString(), anyString(), anyString(), anyString()))
                .willReturn("I cannot determine the verdict.");
        given(fheService.encryptedGreaterThanCheck(anyString()))
                .willReturn(FheResult.withinLimits(20L));

        AmlDecision decision = orchestrator.evaluate(highRiskRequest);

        // Conservative compliance: ambiguous verdict → REVIEW
        assertThat(decision.getVerdict()).isEqualTo("REVIEW");
    }

    // ----------------------------------------------------------------
    // Audit trail
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Decision always contains processedAt timestamp")
    void decision_alwaysContainsProcessedAt() {
        AmlDecision decision = orchestrator.evaluate(lowRiskRequest);
        assertThat(decision.getProcessedAt()).isNotNull();
        assertThat(decision.getGatewayVersion()).isEqualTo("1.0.0");
    }
}
