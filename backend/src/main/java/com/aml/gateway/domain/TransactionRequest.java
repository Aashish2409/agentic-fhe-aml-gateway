package com.aml.gateway.domain;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank(message = "originAccount must not be blank")
    private String originAccount;

    @NotNull(message = "timestamp must not be null")
    private LocalDateTime timestamp;

    @JsonSetter("timestamp")
    public void setTimestampFromString(String value) {
        if (value == null) return;
        try {
            // handles "2026-03-20T08:22:00.000Z" (with Z)
            this.timestamp = OffsetDateTime.parse(value).toLocalDateTime();
        } catch (Exception e) {
            // handles "2026-03-20T08:22:00" (without Z)
            this.timestamp = LocalDateTime.parse(value.replace("Z", ""));
        }
    }

    @NotNull(message = "riskLevel must not be null")
    private RiskLevel riskLevel;

    @NotBlank(message = "ciphertextAmount must not be blank")
    private String ciphertextAmount;
}
