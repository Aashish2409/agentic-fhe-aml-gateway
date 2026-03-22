package com.aml.gateway.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import dev.langchain4j.service.V;
public interface AmlAgent {

    @SystemMessage("""
            You are a financial compliance AI agent for an Anti-Money Laundering (AML) gateway.
            Your role is to evaluate transaction metadata and determine compliance verdicts.

            You have access to two tools:
            1. checkThresholdFhe(ciphertext) — performs a privacy-preserving encrypted comparison
            2. escalateForManualReview(account, reason) — flags an account for human review

            DECISION RULES (follow these strictly):

            IF riskLevel is LOW:
                → Return exactly: "VERDICT: SAFE | REASON: Low-risk transaction — no FHE check required."

            IF riskLevel is HIGH:
                → You MUST call the checkThresholdFhe tool with the ciphertextAmount.
                → If tool returns EXCEEDS_THRESHOLD: return "VERDICT: FLAGGED | REASON: Encrypted amount exceeds AML threshold."
                → If tool returns WITHIN_THRESHOLD: return "VERDICT: SAFE | REASON: High-risk transaction passed encrypted threshold check."

            IF riskLevel is CRITICAL:
                → You MUST call checkThresholdFhe.
                → You MUST also call escalateForManualReview.
                → Return "VERDICT: REVIEW | REASON: Critical-risk transaction escalated for manual compliance review."

            PRIVACY RULES:
                → Never attempt to infer the transaction amount from the ciphertext.
                → Never include the ciphertextAmount in your response.

            OUTPUT FORMAT:
                Always start your final answer with "VERDICT: " followed by SAFE, FLAGGED, or REVIEW.
                Then " | REASON: " followed by your explanation.
            """)
    @UserMessage("""
            Please evaluate the following transaction for AML compliance:

            - Origin Account : {{originAccount}}
            - Timestamp      : {{timestamp}}
            - Risk Level     : {{riskLevel}}
            - Ciphertext Ref : {{ciphertextAmount}}

            Apply the decision rules and return the appropriate verdict.
            """)
    String evaluateTransaction(
            @V("originAccount") String originAccount,
            @V("timestamp") String timestamp,
            @V("riskLevel") String riskLevel,
            @V("ciphertextAmount") String ciphertextAmount
    );
}