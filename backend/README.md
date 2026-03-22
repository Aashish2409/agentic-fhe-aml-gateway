# Agentic-FHE AML Gateway

> **Privacy-preserving Anti-Money Laundering gateway** combining  
> Fully Homomorphic Encryption (FHE) with Agentic AI orchestration.  
> Transaction amounts are **NEVER decrypted** — compliance analysis runs entirely on ciphertext.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Quick Start](#quick-start)
5. [Configuration](#configuration)
6. [API Reference](#api-reference)
7. [Privacy Model](#privacy-model)
8. [FHE Integration Guide (Production)](#fhe-integration-guide-production)
9. [AI Agent Behaviour](#ai-agent-behaviour)
10. [Running Tests](#running-tests)
11. [Example Requests](#example-requests)

---

## Architecture Overview

```
Client (Bank)
    │
    │  POST /api/v1/aml/analyze
    │  { originAccount, timestamp, riskLevel, ciphertextAmount }
    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AmlController (REST)                          │
│             Input validation via @Valid + Jakarta               │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│               ComplianceOrchestrator (Service)                   │
│  • LOW risk  → fast-path SAFE (no LLM call)                     │
│  • HIGH/CRITICAL → delegate to AI Agent                         │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│            AmlAgent (LangChain4j @AiService)                    │
│  • GPT-4o-mini with deterministic temperature=0                 │
│  • System prompt encodes AML decision rules                     │
│  • Tool-calling capability decides when to invoke FHE           │
└──────────┬────────────────────────────────────────────┬─────────┘
           │ calls tool                                  │ calls tool
           ▼                                             ▼
┌─────────────────────────┐               ┌───────────────────────┐
│  checkThresholdFhe()    │               │ escalateForManualReview│
│  (AmlComplianceTools)   │               │ (AmlComplianceTools)  │
└──────────┬──────────────┘               └───────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                        FheService                                │
│  encryptedGreaterThanCheck(ciphertext)                          │
│                                                                  │
│  Mock (prototype):  Deterministic simulation                    │
│  Production:        Zama TFHE JNI native call                   │
│                                                                  │
│  Returns: FheResult { boolean encryptedComparisonResult }       │
│  ⚠️  Amount plaintext is NEVER produced                         │
└─────────────────────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────────────────────┐
│                       API Response                               │
│  AmlDecision {                                                   │
│    verdict: "SAFE" | "FLAGGED" | "REVIEW" | "ERROR"            │
│    fheCheckPerformed: boolean                                    │
│    fheResult: { encryptedComparisonResult, computationTimeMs }  │
│    ⚠️  ciphertextAmount EXCLUDED from response                  │
│  }                                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| AI Agent | LangChain4j 0.36.2 |
| LLM Provider | OpenAI GPT-4o-mini (swappable) |
| FHE Engine | Zama TFHE (mock for prototype) |
| Build Tool | Maven |
| Utilities | Lombok, Jackson |

---

## Project Structure

```
agentic-fhe-aml-gateway/
├── pom.xml
├── README.md
├── examples/
│   ├── request-high-risk.json
│   ├── request-low-risk.json
│   ├── response-flagged.json
│   └── response-safe.json
└── src/
    ├── main/
    │   ├── java/com/aml/gateway/
    │   │   ├── AmlGatewayApplication.java      ← Spring Boot entry point
    │   │   ├── agent/
    │   │   │   └── AmlAgent.java               ← LangChain4j @AiService interface
    │   │   ├── config/
    │   │   │   ├── AmlAgentConfig.java          ← LLM + Agent bean wiring
    │   │   │   └── JacksonConfig.java           ← ObjectMapper configuration
    │   │   ├── controller/
    │   │   │   ├── AmlController.java           ← REST endpoints
    │   │   │   └── GlobalExceptionHandler.java  ← Error handling
    │   │   ├── domain/
    │   │   │   ├── AmlDecision.java             ← API response model
    │   │   │   ├── FheResult.java               ← FHE comparison result
    │   │   │   ├── RiskLevel.java               ← Risk enum (LOW/MEDIUM/HIGH/CRITICAL)
    │   │   │   └── TransactionRequest.java      ← API request model
    │   │   ├── service/
    │   │   │   ├── ComplianceOrchestrator.java  ← Business logic coordinator
    │   │   │   └── FheService.java              ← FHE engine (mock + production guide)
    │   │   └── tools/
    │   │       └── AmlComplianceTools.java      ← LangChain4j @Tool definitions
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/aml/gateway/
            ├── AmlControllerTest.java           ← Web layer tests (@WebMvcTest)
            ├── ComplianceOrchestratorTest.java  ← Unit tests (Mockito)
            └── FheServiceTest.java              ← Unit tests (JUnit 5)
```

---

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- OpenAI API key (or substitute Ollama for local LLM)

### 1. Clone and configure

```bash
git clone <repo-url>
cd agentic-fhe-aml-gateway
```

### 2. Set your OpenAI API key

```bash
export OPENAI_API_KEY=sk-your-key-here
```

**Alternative: Use Ollama (no API key required)**

Edit `src/main/resources/application.yml` and update `AmlAgentConfig.java`:
```yaml
# application.yml — replace OpenAI config with:
ollama:
  base-url: http://localhost:11434
  model: mistral
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The gateway starts at `http://localhost:8080`.

### 4. Test with cURL

**HIGH risk transaction (triggers FHE check):**
```bash
curl -X POST http://localhost:8080/api/v1/aml/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "originAccount": "ACC-123",
    "timestamp": "2026-01-10T10:00:00",
    "riskLevel": "HIGH",
    "ciphertextAmount": "ENC_ABC123"
  }'
```

**LOW risk transaction (fast-path SAFE):**
```bash
curl -X POST http://localhost:8080/api/v1/aml/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "originAccount": "ACC-456",
    "timestamp": "2026-01-10T09:30:00",
    "riskLevel": "LOW",
    "ciphertextAmount": "ENC_DEF456"
  }'
```

**Simulate a FLAGGED transaction (mock FHE trigger):**
```bash
curl -X POST http://localhost:8080/api/v1/aml/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "originAccount": "ACC-789",
    "timestamp": "2026-01-10T23:45:00",
    "riskLevel": "HIGH",
    "ciphertextAmount": "ENC_LARGE_TX_EXCEED"
  }'
```
> The mock FHE service flags ciphertexts containing: `LARGE`, `BIG`, `EXCEED`, `OVER`, `FLAG`

---

## Configuration

### application.yml key settings

| Property | Default | Description |
|----------|---------|-------------|
| `langchain4j.open-ai.chat-model.api-key` | `${OPENAI_API_KEY}` | OpenAI API key |
| `langchain4j.open-ai.chat-model.model-name` | `gpt-4o-mini` | LLM model to use |
| `langchain4j.open-ai.chat-model.temperature` | `0.0` | Deterministic mode for compliance |
| `fhe.service.mock-delay-ms` | `50` | Simulated FHE computation delay |
| `fhe.service.mode` | `mock` | `mock` (prototype) or `native` (production) |
| `server.port` | `8080` | HTTP port |

---

## API Reference

### POST /api/v1/aml/analyze

**Request Body:**
```json
{
  "originAccount": "ACC-123",
  "timestamp": "2026-01-10T10:00:00",
  "riskLevel": "HIGH",
  "ciphertextAmount": "ENC_ABC123"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `originAccount` | String | ✅ | Account identifier |
| `timestamp` | ISO-8601 | ✅ | Transaction timestamp |
| `riskLevel` | Enum | ✅ | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `ciphertextAmount` | String | ✅ | FHE-encrypted transaction amount |

**Response (FLAGGED):**
```json
{
  "verdict": "FLAGGED",
  "message": "Encrypted amount exceeds AML threshold.",
  "originAccount": "ACC-123",
  "riskLevel": "HIGH",
  "fheCheckPerformed": true,
  "fheResult": {
    "encryptedComparisonResult": true,
    "comparisonLabel": "EXCEEDS_THRESHOLD",
    "computationTimeMs": 52
  },
  "processedAt": "2026-01-10T10:00:01",
  "gatewayVersion": "1.0.0"
}
```

**Response (SAFE — LOW risk):**
```json
{
  "verdict": "SAFE",
  "message": "Low-risk transaction auto-approved — FHE check not required.",
  "originAccount": "ACC-456",
  "riskLevel": "LOW",
  "fheCheckPerformed": false,
  "fheResult": null,
  "processedAt": "2026-01-10T09:30:00",
  "gatewayVersion": "1.0.0"
}
```

**Verdict values:**
| Verdict | Meaning |
|---------|---------|
| `SAFE` | Transaction approved |
| `FLAGGED` | Exceeds encrypted threshold — block/report |
| `REVIEW` | Manual compliance review required |
| `ERROR` | Gateway processing failure |

### GET /api/v1/aml/health
Returns gateway status and capabilities.

---

## Privacy Model

```
┌─────────────────────────────────────────────────────────────────────┐
│  What the server KNOWS         │  What the server NEVER LEARNS      │
├────────────────────────────────┼────────────────────────────────────┤
│  • originAccount               │  • The actual monetary amount      │
│  • timestamp                   │  • Whether amount is $1 or $1M     │
│  • riskLevel                   │  • Any intermediate computation    │
│  • Opaque ciphertext blob      │    values from the FHE circuit     │
│  • Boolean comparison result   │                                    │
│    (exceeds threshold? Y/N)    │                                    │
└─────────────────────────────────────────────────────────────────────┘
```

**How FHE preserves privacy:**
1. Client encrypts amount using server's public key → `ciphertextAmount`
2. Server holds `encryptedThreshold` (never decrypted individually)
3. FHE circuit evaluates: `result_enc = ciphertextAmount > encryptedThreshold`
4. Only `result_enc` (a boolean) is decrypted
5. Neither operand's plaintext is ever revealed

---

## FHE Integration Guide (Production)

To integrate real Zama TFHE replace `FheService.encryptedGreaterThanCheck()`:

```java
// 1. Add native dependency to pom.xml:
// <dependency>
//   <groupId>com.zama</groupId>
//   <artifactId>tfhe-jni</artifactId>
//   <version>0.6.0</version>
// </dependency>

// 2. Load server key (generated offline, stored securely)
TfheServerKey serverKey = TfheServerKey.load(
    Files.readAllBytes(Path.of("/secrets/tfhe-server.key"))
);

// 3. Deserialize client ciphertext
FheUint64 amountEnc = FheUint64.deserialize(
    Base64.getDecoder().decode(ciphertextAmount)
);

// 4. Load pre-computed encrypted threshold
FheUint64 thresholdEnc = FheUint64.load(encryptedThresholdBytes);

// 5. Homomorphic comparison (NO DECRYPTION of operands)
FheBool resultEnc = amountEnc.gt(thresholdEnc, serverKey);

// 6. Decrypt ONLY the comparison result
boolean exceeded = resultEnc.decrypt(clientKey);
```

**Key management:**
- `serverKey` — used for FHE evaluation, stored server-side
- `clientKey` — used to decrypt results, can be held by client or trusted HSM
- `encryptedThreshold` — pre-computed by compliance team, versioned per regulatory update

---

## AI Agent Behaviour

The `AmlAgent` is a LangChain4j `@AiService` powered by GPT-4o-mini.

**Decision rules (from system prompt):**

| Risk Level | Agent Action |
|-----------|-------------|
| `LOW` | Return `SAFE` immediately (no tool call) |
| `MEDIUM` | Call FHE tool if outside business hours (08:00–20:00 UTC) |
| `HIGH` | **MUST** call `checkThresholdFhe` → interpret result |
| `CRITICAL` | **MUST** call `checkThresholdFhe` + `escalateForManualReview` |

**Swapping the LLM provider:**

In `AmlAgentConfig.java`, replace `OpenAiChatModel` with any LangChain4j-compatible model:

```java
// Local Ollama (privacy-first, no external API calls):
return OllamaChatModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("mistral")
        .build();

// Anthropic Claude:
return AnthropicChatModel.builder()
        .apiKey(System.getenv("ANTHROPIC_API_KEY"))
        .modelName("claude-3-5-haiku-20241022")
        .build();
```

---

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=FheServiceTest
mvn test -Dtest=ComplianceOrchestratorTest
mvn test -Dtest=AmlControllerTest

# Run with verbose output
mvn test -Dsurefire.useFile=false
```

**Test coverage:**
- `FheServiceTest` — FHE mock logic, factory methods, validation
- `ComplianceOrchestratorTest` — Orchestration paths (mocked agent + FHE)
- `AmlControllerTest` — HTTP layer (WebMvcTest, validation, status codes)

---

## Roadmap to Production

- [ ] Replace mock `FheService` with Zama TFHE JNI native binding
- [ ] Add HSM (Hardware Security Module) for server key storage
- [ ] Implement proper key rotation and versioned encrypted thresholds
- [ ] Add distributed tracing (OpenTelemetry) for FHE circuit latency monitoring
- [ ] Introduce async processing queue (Kafka) for high-volume transaction loads
- [ ] Add audit log persistence (append-only database) for compliance trail
- [ ] Implement rate limiting and mutual TLS for bank authentication
- [ ] Add Swagger/OpenAPI documentation (`springdoc-openapi`)

---

*Built with ❤️ for privacy-preserving financial compliance.*
