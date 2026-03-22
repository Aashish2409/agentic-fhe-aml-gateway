# Cipher AML Gateway

Privacy-preserving Anti-Money Laundering gateway using Order Preserving Encryption (OPE) + Agentic AI.

## How it works
1. User enters transaction amount on frontend
2. Amount is OPE-encrypted client-side — plaintext NEVER sent to server
3. Backend AI agent compares encrypted value against encrypted threshold
4. Returns SAFE / FLAGGED / REVIEW verdict

## Run Backend
```bash
cd backend
# Set your Groq API key first:
export GROQ_API_KEY=your-key-here
mvn spring-boot:run
# Runs on http://localhost:8080
```

## Run Frontend
```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:3000
```
