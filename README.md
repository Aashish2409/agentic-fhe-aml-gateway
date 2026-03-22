# Cipher — Privacy-Preserving AML Gateway

> Banks can't outsource compliance checks — sharing customer amounts with third parties is a privacy violation. **Cipher solves this.**

Cipher is an Anti-Money Laundering gateway that detects suspicious transactions **without ever seeing the transaction amount**. The amount is encrypted on the client using Order Preserving Encryption before it leaves the browser — the server only ever sees ciphertext.

---

## The Problem with Traditional AML

Every bank today runs AML checks like this:

```
Transaction arrives → Is ₹9,50,000 > threshold? → FLAG
```

Simple. But the moment you want to outsource this to a third-party compliance service, you have to hand over your customer's financial data. That's a fundamental privacy violation — and why inter-bank AML cooperation is nearly impossible today.

---

## How Cipher Works

```
User types ₹9,50,000
    ↓
Browser encrypts: OPE(9,50,000) = 7,647,161,789
    ↓
Only this number is sent over the network
    ↓
Gateway compares: OPE(amount) > OPE(threshold)
    ↓
AI agent returns: SAFE / FLAGGED / REVIEW
    ↓
Plaintext amount was never transmitted, stored, or seen
```

The key property of OPE: `enc(a) > enc(b)` if and only if `a > b`. This means the server can determine whether an amount exceeds a threshold **purely on ciphertext**, with zero knowledge of the actual value.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     Browser                                   │
│                                                              │
│  amount = ₹9,50,000                                         │
│  enc    = (amount × 7919) + 123456789  ← OPE encrypt        │
│  risk   = enc(amount) > enc(threshold) ← no plaintext        │
│                                                              │
│  POST { originAccount, timestamp, riskLevel, ciphertext }   │
│                         ↓ plaintext NEVER crosses this line  │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│                  Spring Boot Gateway                          │
│                                                              │
│  AmlController                                               │
│      ↓                                                       │
│  ComplianceOrchestrator → AmlAgent (Groq Llama 70B)         │
│      ↓                                                       │
│  Agent reasons over metadata + calls tools autonomously:    │
│    • checkThresholdFhe(ciphertext) → OPE comparison         │
│    • escalateForManualReview(account, reason)               │
│      ↓                                                       │
│  Verdict: SAFE | FLAGGED | REVIEW + reason                  │
└──────────────────────────────────────────────────────────────┘
```

Every transaction goes through the AI agent — including LOW risk ones. This ensures every decision is **reasoned, explainable, and auditable**, not just a threshold comparison.

---

## What Makes This Different

| | Traditional AML | Cipher |
|---|---|---|
| Sees plaintext amount | ✅ Yes | ❌ Never |
| Safe to outsource | ❌ Privacy risk | ✅ Mathematically safe |
| Decision logic | Static rule engine | LLM reasoning with tools |
| Explains its decisions | ❌ Just a flag | ✅ Reason attached to every verdict |
| Cross-bank potential | ❌ Data silos | ✅ Architecture designed for it |

---

## Tech Stack

| | |
|---|---|
| **Backend** | Java 21, Spring Boot 3.4.1 |
| **AI Agent** | LangChain4j 0.36.2 + Groq Llama-3.3-70B |
| **Encryption** | Order Preserving Encryption (OPE) — client side TypeScript |
| **Frontend** | React 18, TypeScript, Tailwind CSS, shadcn/ui, Framer Motion |
| **Build** | Maven, Vite |

---

## Project Structure

```
cipher-aml/
├── backend/
│   └── src/main/java/com/aml/gateway/
│       ├── agent/          AmlAgent.java       — LangChain4j AI service
│       ├── config/         AmlAgentConfig.java — Groq model wiring
│       ├── controller/     AmlController.java  — REST + CORS
│       ├── service/
│       │   ├── ComplianceOrchestrator.java     — routes all txns to AI
│       │   └── FheService.java                 — OPE comparison engine
│       ├── tools/          AmlComplianceTools  — AI-callable @Tool methods
│       └── domain/         request + response models
└── frontend/
    └── src/
        ├── pages/TransactionAnalysis.tsx       — OPE encryption + UI
        ├── pages/Dashboard.tsx                 — overview
        ├── pages/RiskResults.tsx               — risk breakdown
        └── pages/SystemLogs.tsx                — audit trail
```

---

## Running Locally

**Prerequisites:** Java 21+, Node.js 18+, free Groq API key from [console.groq.com](https://console.groq.com)

```bash
# Backend
cd backend
export GROQ_API_KEY=gsk_your_key   # or set in IntelliJ env vars
mvn spring-boot:run                 # → localhost:8080

# Frontend
cd frontend
npm install
npm run dev                         # → localhost:3000
```

---

## OPE — The Encryption Scheme

```
Key    K = 7919        (large prime as multiplier)
Offset O = 123456789   (large offset to obscure scale)

enc(x) = (x × K) + O

enc(9,50,000)  = 7,523,561,789
enc(10,00,000) = 7,942,656,789

7,523,561,789 < 7,942,656,789  →  9,50,000 < 10,00,000  ✓
```

The server stores only the encrypted threshold. It compares enc(amount) against it and returns a boolean — never the amount itself.

> In production, this mock OPE would be replaced with **Zama TFHE** via JNI. The architecture is designed for this swap — only FheService.java changes.

---

## Regulatory Basis

Thresholds are grounded in Indian PMLA and RBI guidelines:

- Cash transactions above ₹10,00,000 → mandatory CTR to FIU-IND
- Cross-border transfers above ₹5,00,000 → mandatory report
- Any suspicious pattern regardless of amount → STR filing required

The CRITICAL risk tier maps directly to the RBI mandatory reporting threshold.

---

## Future Scope

**Inter-bank coordination** — the architecture's real potential. Multiple banks submit encrypted transactions to Cipher. The gateway detects cross-bank structuring patterns without any bank revealing customer data to each other or to Cipher.

**Circular laundering detection** — graph cycle detection (DFS) on the transaction graph to catch A→B→C→D→A patterns.

**Velocity detection** — flag structuring: 5+ transactions in 7 days from the same identity, each just below threshold.

**True FHE** — swap mock with Zama TFHE JNI for cryptographically rigorous guarantees.

---

## Author

**Aashish** — B.Tech CSE · [github.com/Aashish2409](https://github.com/Aashish2409)