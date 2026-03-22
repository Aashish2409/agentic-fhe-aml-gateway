package com.aml.gateway;

import com.aml.gateway.domain.AmlDecision;
import com.aml.gateway.domain.RiskLevel;
import com.aml.gateway.domain.TransactionRequest;
import com.aml.gateway.service.ComplianceOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web layer integration tests for AmlController.
 *
 * <p>Uses @WebMvcTest to test the controller slice in isolation.
 * The ComplianceOrchestrator is mocked — no AI or FHE calls are made.</p>
 */
@WebMvcTest(controllers = com.aml.gateway.controller.AmlController.class)
@DisplayName("AmlController — Web Layer Tests")
class AmlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComplianceOrchestrator complianceOrchestrator;

    // ----------------------------------------------------------------
    // POST /api/v1/aml/analyze — happy paths
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Valid HIGH risk request → 200 OK with FLAGGED verdict")
    void validHighRiskRequest_returns200WithFlaggedVerdict() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .originAccount("ACC-123")
                .timestamp(LocalDateTime.of(2026, 1, 10, 10, 0, 0))
                .riskLevel(RiskLevel.HIGH)
                .ciphertextAmount("ENC_ABC123")
                .build();

        AmlDecision mockDecision = AmlDecision.builder()
                .verdict("FLAGGED")
                .message("Encrypted amount exceeds AML threshold.")
                .originAccount("ACC-123")
                .riskLevel(RiskLevel.HIGH)
                .fheCheckPerformed(true)
                .processedAt(LocalDateTime.now())
                .build();

        given(complianceOrchestrator.evaluate(any(TransactionRequest.class)))
                .willReturn(mockDecision);

        mockMvc.perform(post("/api/v1/aml/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.verdict").value("FLAGGED"))
                .andExpect(jsonPath("$.originAccount").value("ACC-123"))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.fheCheckPerformed").value(true));
    }

    @Test
    @DisplayName("Valid LOW risk request → 200 OK with SAFE verdict")
    void validLowRiskRequest_returns200WithSafeVerdict() throws Exception {
        TransactionRequest request = TransactionRequest.builder()
                .originAccount("ACC-456")
                .timestamp(LocalDateTime.of(2026, 1, 10, 9, 0, 0))
                .riskLevel(RiskLevel.LOW)
                .ciphertextAmount("ENC_DEF456")
                .build();

        AmlDecision mockDecision = AmlDecision.builder()
                .verdict("SAFE")
                .message("Low-risk transaction auto-approved.")
                .originAccount("ACC-456")
                .riskLevel(RiskLevel.LOW)
                .fheCheckPerformed(false)
                .processedAt(LocalDateTime.now())
                .build();

        given(complianceOrchestrator.evaluate(any(TransactionRequest.class)))
                .willReturn(mockDecision);

        mockMvc.perform(post("/api/v1/aml/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAFE"))
                .andExpect(jsonPath("$.fheCheckPerformed").value(false));
    }

    // ----------------------------------------------------------------
    // POST /api/v1/aml/analyze — validation failures
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Missing originAccount → 400 Bad Request")
    void missingOriginAccount_returns400() throws Exception {
        String badJson = """
                {
                  "timestamp": "2026-01-10T10:00:00",
                  "riskLevel": "HIGH",
                  "ciphertextAmount": "ENC_ABC123"
                }
                """;

        mockMvc.perform(post("/api/v1/aml/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.originAccount").exists());
    }

    @Test
    @DisplayName("Missing ciphertextAmount → 400 Bad Request")
    void missingCiphertext_returns400() throws Exception {
        String badJson = """
                {
                  "originAccount": "ACC-123",
                  "timestamp": "2026-01-10T10:00:00",
                  "riskLevel": "HIGH"
                }
                """;

        mockMvc.perform(post("/api/v1/aml/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ciphertextAmount").exists());
    }

    @Test
    @DisplayName("Invalid riskLevel value → 400 Bad Request")
    void invalidRiskLevel_returns400() throws Exception {
        String badJson = """
                {
                  "originAccount": "ACC-123",
                  "timestamp": "2026-01-10T10:00:00",
                  "riskLevel": "EXTREME",
                  "ciphertextAmount": "ENC_ABC123"
                }
                """;

        mockMvc.perform(post("/api/v1/aml/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------
    // GET /api/v1/aml/health
    // ----------------------------------------------------------------

    @Test
    @DisplayName("Health endpoint → 200 OK with UP status")
    void healthEndpoint_returns200WithUpStatus() throws Exception {
        mockMvc.perform(get("/api/v1/aml/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.gateway").value("Agentic-FHE AML Gateway"))
                .andExpect(jsonPath("$.capabilities.fheEngine").exists())
                .andExpect(jsonPath("$.capabilities.privacyGuarantee").exists());
    }
}
