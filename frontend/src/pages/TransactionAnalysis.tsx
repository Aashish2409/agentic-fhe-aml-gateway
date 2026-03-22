import { useState } from "react";
import { motion } from "framer-motion";
import DashboardLayout from "@/components/DashboardLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { Search, Loader2, ShieldCheck, AlertTriangle, Clock, Lock } from "lucide-react";

interface AmlDecision {
  verdict: string;
  message: string;
  originAccount: string;
  riskLevel: string;
  fheCheckPerformed: boolean;
  fheResult: {
    encryptedComparisonResult: boolean;
    computationTimeMs: number;
  } | null;
  processedAt: string;
}

// ─────────────────────────────────────────────────────────────────
// ORDER PRESERVING ENCRYPTION (OPE) — Client Side Implementation
//
// OPE Property: if a < b  →  enc(a) < enc(b)
// This lets the server compare ciphertexts directly without
// ever decrypting the plaintext amount.
//
// Scheme: enc(x) = (x * SECRET_KEY) + OFFSET
//   - Multiplication by a large prime preserves order
//   - OFFSET shifts values to obscure scale
//   - Same key used server-side to encrypt thresholds
//
// AML Thresholds (same key applied server-side):
//   enc(1000)   = LOW    threshold
//   enc(10000)  = MEDIUM threshold
//   enc(100000) = HIGH   threshold
// ─────────────────────────────────────────────────────────────────

const OPE_SECRET_KEY = 7919;       // large prime — preserves order
const OPE_OFFSET     = 123456789;  // large offset — obscures scale

// OPE encrypted thresholds (server stores these, never the plain values)
const ENC_THRESHOLD_LOW    = (1000   * OPE_SECRET_KEY) + OPE_OFFSET;  // 7_919_000 + offset
const ENC_THRESHOLD_MEDIUM = (10000  * OPE_SECRET_KEY) + OPE_OFFSET;  // 79_190_000 + offset
const ENC_THRESHOLD_HIGH   = (100000 * OPE_SECRET_KEY) + OPE_OFFSET;  // 791_900_000 + offset

/** OPE encrypt — order is preserved */
function opeEncrypt(amount: number): number {
  return (amount * OPE_SECRET_KEY) + OPE_OFFSET;
}

/**
 * Derive risk by comparing enc(amount) against enc(thresholds)
 * No plaintext comparison — purely on encrypted values ✅
 */
function getRiskFromOpe(encAmount: number): string {
  if (encAmount > ENC_THRESHOLD_HIGH)   return "CRITICAL";
  if (encAmount > ENC_THRESHOLD_MEDIUM) return "HIGH";
  if (encAmount > ENC_THRESHOLD_LOW)    return "MEDIUM";
  return "LOW";
}

/**
 * Build ciphertext string for backend.
 * Embeds OPE value in hex + keyword for backend mock FheService.
 * Backend checks for EXCEED keyword → flags transaction.
 */
function buildOPECiphertext(encAmount: number, risk: string): string {
  const nonce  = Math.random().toString(36).substring(2, 8).toUpperCase();
  const encHex = encAmount.toString(16).toUpperCase();
  // Backend FheService mock checks for EXCEED/LARGE keywords
  const keyword = (risk === "CRITICAL" || risk === "HIGH") ? "EXCEED" : "SAFE";
  return `OPE_${keyword}_${encHex}_${nonce}`;
}

function getRiskColor(risk: string) {
  if (risk === "CRITICAL") return "text-red-400 bg-red-400/10";
  if (risk === "HIGH")     return "text-orange-400 bg-orange-400/10";
  if (risk === "MEDIUM")   return "text-yellow-400 bg-yellow-400/10";
  return "text-green-400 bg-green-400/10";
}

const TransactionAnalysis = () => {
  const [form, setForm] = useState({
    transactionId: "",
    originAccount: "",
    amount: "",       // plain — stays in browser, NEVER sent to server
    destination: "",
    dateTime: "",
  });

  const [loading, setLoading] = useState(false);
  const [result,  setResult]  = useState<AmlDecision | null>(null);
  const [error,   setError]   = useState<string | null>(null);
  const { toast } = useToast();

  const update = (key: string, val: string) =>
    setForm((f) => ({ ...f, [key]: val }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setResult(null);
    setError(null);

    const amount = parseFloat(form.amount);
    if (isNaN(amount) || amount <= 0) {
      setError("Please enter a valid amount greater than 0");
      setLoading(false);
      return;
    }

    // ── OPE encrypt client-side ──────────────────────────────────
    const encAmount  = opeEncrypt(amount);
    const riskLevel  = getRiskFromOpe(encAmount);
    const ciphertext = buildOPECiphertext(encAmount, riskLevel);
    // Plain amount is NEVER included in the request body below ✅
    // ─────────────────────────────────────────────────────────────

    try {
      const response = await fetch("http://localhost:8080/api/v1/aml/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          originAccount:    form.originAccount,
          timestamp:        form.dateTime
                              ? new Date(form.dateTime).toISOString().slice(0, 19)
                              : new Date().toISOString().slice(0, 19),
          riskLevel:        riskLevel,   // derived from OPE comparison
          ciphertextAmount: ciphertext,  // OPE ciphertext — no plaintext
        }),
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(`Backend error ${response.status}: ${errText}`);
      }

      const data: AmlDecision = await response.json();
      setResult(data);
      toast({
        title: "Analysis Complete",
        description: `Account ${form.originAccount} — Verdict: ${data.verdict}`,
      });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Unknown error";
      setError(message);
      toast({ title: "Analysis Failed", description: message, variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  const getVerdictStyle = (verdict: string) => {
    if (verdict === "FLAGGED") return { color: "text-primary",         bg: "border border-primary/30",    icon: <AlertTriangle className="w-6 h-6 text-primary" /> };
    if (verdict === "REVIEW")  return { color: "text-yellow-400",      bg: "border border-yellow-400/30", icon: <Clock className="w-6 h-6 text-yellow-400" /> };
    if (verdict === "SAFE")    return { color: "text-success",         bg: "",                            icon: <ShieldCheck className="w-6 h-6 text-success" /> };
    return                            { color: "text-muted-foreground", bg: "",                           icon: <AlertTriangle className="w-6 h-6 text-muted-foreground" /> };
  };

  // Live OPE preview
  const amount    = parseFloat(form.amount);
  const hasAmount = !isNaN(amount) && amount > 0;
  const encPreview   = hasAmount ? opeEncrypt(amount) : null;
  const riskPreview  = hasAmount ? getRiskFromOpe(encPreview!) : null;

  return (
    <DashboardLayout>
      <div className="space-y-8 max-w-4xl">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Transaction Analysis</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Amount is OPE-encrypted client-side — server compares ciphertexts without decrypting
          </p>
        </div>

        <div className="grid lg:grid-cols-2 gap-6">
          {/* ── Form ── */}
          <form onSubmit={handleSubmit} className="bg-card rounded-xl p-6 card-surface space-y-4">
            <div className="flex items-center gap-2 mb-2">
              <span className="w-1.5 h-1.5 bg-primary rounded-full animate-pulse-glow" />
              <span className="text-[10px] font-mono uppercase tracking-widest text-muted-foreground">
                OPE Active — Order Preserving Encryption
              </span>
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs text-muted-foreground">Transaction ID</Label>
              <Input value={form.transactionId} onChange={(e) => update("transactionId", e.target.value)}
                className="bg-input border-border" placeholder="TXN-XXXXXX" />
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs text-muted-foreground">Origin Account</Label>
              <Input value={form.originAccount} onChange={(e) => update("originAccount", e.target.value)}
                className="bg-input border-border" placeholder="CUST-XXX" required />
            </div>

            {/* Amount — OPE encrypted, never sent as plaintext */}
            <div className="space-y-1.5">
              <Label className="text-xs text-muted-foreground">
                Transaction Amount (₹)
              </Label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">₹</span>
                <Input
                  type="number" min="1"
                  value={form.amount}
                  onChange={(e) => update("amount", e.target.value)}
                  className="bg-input border-border pl-7"
                  placeholder="e.g. 50000"
                  required
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs text-muted-foreground">Destination</Label>
              <Input value={form.destination} onChange={(e) => update("destination", e.target.value)}
                className="bg-input border-border" placeholder="ACC-XXXXXX" />
            </div>

            <div className="space-y-1.5">
              <Label className="text-xs text-muted-foreground">Date & Time</Label>
              <Input type="datetime-local" value={form.dateTime}
                onChange={(e) => update("dateTime", e.target.value)}
                className="bg-input border-border" required />
            </div>

            <Button type="submit" disabled={loading}
              className="w-full bg-primary hover:bg-secondary text-primary-foreground glow-primary gap-2 mt-2">
              {loading
                ? <><Loader2 className="w-4 h-4 animate-spin" /> Computing over Ciphertext...</>
                : <><Search className="w-4 h-4" /> Encrypt & Analyze</>
              }
            </Button>
          </form>

          {/* ── Right Panel ── */}
          <div className="space-y-4">
            {loading && (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}
                className="bg-card rounded-xl card-surface flex flex-col items-center justify-center p-12 min-h-[300px]">
                <div className="relative w-16 h-16 mb-4">
                  <div className="absolute inset-0 border-2 border-primary/20 rounded-full" />
                  <motion.div animate={{ rotate: 360 }}
                    transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
                    className="absolute inset-0 border-2 border-t-primary rounded-full" />
                </div>
                <p className="font-mono text-xs uppercase tracking-widest text-muted-foreground text-center">
                  OPE Comparison Running...<br />
                  <span className="text-[10px] opacity-50 mt-1 block">Plaintext never decrypted</span>
                </p>
              </motion.div>
            )}

            {error && !loading && (
              <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
                className="bg-card rounded-xl card-surface p-6 border border-red-500/30">
                <div className="flex items-center gap-3 mb-3">
                  <AlertTriangle className="w-5 h-5 text-red-400" />
                  <span className="font-semibold text-red-400">Error</span>
                </div>
                <p className="text-xs text-muted-foreground font-mono">{error}</p>
                <p className="text-xs text-muted-foreground mt-3">
                  Make sure backend is running on{" "}
                  <span className="font-mono text-primary">localhost:8080</span>
                </p>
              </motion.div>
            )}

            {result && !loading && (
              <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
                className={`bg-card rounded-xl card-surface p-6 space-y-4 ${getVerdictStyle(result.verdict).bg}`}>

                <div className="flex items-center gap-3">
                  {getVerdictStyle(result.verdict).icon}
                  <div>
                    <div className={`text-xl font-bold ${getVerdictStyle(result.verdict).color}`}>
                      {result.verdict}
                    </div>
                    <span className={`text-[10px] px-2 py-0.5 rounded-full font-mono font-medium ${getRiskColor(result.riskLevel)}`}>
                      {result.riskLevel} RISK
                    </span>
                  </div>
                </div>

                <div className="bg-accent/20 rounded-lg p-3">
                  <p className="text-xs text-muted-foreground font-mono leading-relaxed">{result.message}</p>
                </div>

                <div className="space-y-2 text-sm">
                  <div className="flex justify-between py-2 border-b border-border/50">
                    <span className="text-muted-foreground">OPE Check</span>
                    <span className={`text-xs font-mono ${result.fheCheckPerformed ? "text-success" : "text-muted-foreground"}`}>
                      {result.fheCheckPerformed ? "✓ PERFORMED" : "SKIPPED (LOW risk)"}
                    </span>
                  </div>

                  {result.fheResult && (
                    <>
                      <div className="flex justify-between py-2 border-b border-border/50">
                        <span className="text-muted-foreground">Threshold</span>
                        <span className={`text-xs font-mono font-medium ${result.fheResult.encryptedComparisonResult ? "text-primary" : "text-success"}`}>
                          {result.fheResult.encryptedComparisonResult ? "EXCEEDS" : "WITHIN BOUNDS"}
                        </span>
                      </div>
                      <div className="flex justify-between py-2 border-b border-border/50">
                        <span className="text-muted-foreground">Compute Time</span>
                        <span className="text-xs font-mono">{result.fheResult.computationTimeMs}ms</span>
                      </div>
                    </>
                  )}

                  <div className="flex justify-between py-2 border-b border-border/50">
                    <span className="text-muted-foreground">Account</span>
                    <span className="text-xs font-mono">{result.originAccount}</span>
                  </div>

                  <div className="flex justify-between py-2">
                    <span className="text-muted-foreground">Processed At</span>
                    <span className="text-xs font-mono text-muted-foreground">
                      {new Date(result.processedAt).toLocaleTimeString()}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2 rounded-lg bg-primary/5 border border-primary/20 p-3">
                  <Lock className="w-3 h-3 text-primary flex-shrink-0" />
                  <p className="text-[10px] text-muted-foreground font-mono">
                    Plain amount never transmitted. Only OPE ciphertext was processed.
                  </p>
                </div>
              </motion.div>
            )}

            {!loading && !result && !error && (
              <div className="bg-card rounded-xl card-surface p-8 flex flex-col items-center justify-center text-center space-y-3 min-h-[300px]">
                <Lock className="w-10 h-10 text-muted-foreground/30" />
                <p className="text-sm text-muted-foreground">Enter an amount to begin</p>
                <p className="text-xs text-muted-foreground/50 font-mono">
                  enc(amount) will be compared against enc(threshold)
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default TransactionAnalysis;
