import { motion } from "framer-motion";
import DashboardLayout from "@/components/DashboardLayout";
import { AlertTriangle, ShieldCheck, Activity, Fingerprint, Network, Lock } from "lucide-react";

const riskData = [
  {
    id: "TXN-8A3F2",
    status: "Flagged",
    score: 87,
    reason: "Unusual transaction velocity detected across encrypted channels",
    entityReputation: 32,
    topologyDivergence: 0.94,
    privacyEntropy: 0.23,
    fraudIndicators: ["Velocity anomaly", "Cross-border pattern", "Time-zone mismatch"],
  },
  {
    id: "TXN-5D2A7",
    status: "Flagged",
    score: 79,
    reason: "Encrypted amount exceeds behavioral baseline for entity cluster",
    entityReputation: 45,
    topologyDivergence: 0.81,
    privacyEntropy: 0.41,
    fraudIndicators: ["Amount outlier", "New destination"],
  },
  {
    id: "TXN-7B1E9",
    status: "Verified",
    score: 12,
    reason: "Transaction within expected parameters",
    entityReputation: 91,
    topologyDivergence: 0.12,
    privacyEntropy: 0.88,
    fraudIndicators: [],
  },
  {
    id: "TXN-6C4D8",
    status: "Verified",
    score: 8,
    reason: "Regular recurring pattern identified",
    entityReputation: 95,
    topologyDivergence: 0.05,
    privacyEntropy: 0.95,
    fraudIndicators: [],
  },
];

const RiskResults = () => {
  return (
    <DashboardLayout>
      <div className="space-y-8">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Risk Assessment Results</h1>
          <p className="text-sm text-muted-foreground mt-1">Detailed risk analysis for flagged transactions</p>
        </div>

        {/* Risk gauge */}
        <div className="bg-card rounded-xl p-8 card-surface flex flex-col items-center">
          <div className="relative w-48 h-24 overflow-hidden mb-4">
            <div
              className="absolute inset-0 rounded-t-full"
              style={{
                background: "conic-gradient(from 180deg, hsl(var(--muted)) 0deg, hsl(0 0% 100%) 90deg, hsl(var(--primary)) 180deg)",
                clipPath: "polygon(0 100%, 0 0, 100% 0, 100% 100%)",
              }}
            />
            <div className="absolute inset-3 bg-card rounded-t-full" />
            <div className="absolute bottom-0 left-1/2 -translate-x-1/2 text-center">
              <div className="text-3xl font-bold">47</div>
              <div className="text-[10px] text-muted-foreground uppercase tracking-wider">Avg Score</div>
            </div>
          </div>
        </div>

        {/* Risk cards */}
        <div className="grid md:grid-cols-2 gap-4">
          {riskData.map((r, i) => (
            <motion.div
              key={r.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
              className={`bg-card rounded-xl p-6 card-surface ${r.status === "Flagged" ? "border-l-2 border-l-primary" : "border-l-2 border-l-success"}`}
            >
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  {r.status === "Flagged" ? (
                    <AlertTriangle className="w-4 h-4 text-primary" />
                  ) : (
                    <ShieldCheck className="w-4 h-4 text-success" />
                  )}
                  <span className="font-mono text-sm">{r.id}</span>
                </div>
                <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${
                  r.status === "Flagged" ? "text-primary bg-primary/10" : "text-success bg-success/10"
                }`}>
                  {r.status}
                </span>
              </div>

              <p className="text-sm text-muted-foreground mb-4">{r.reason}</p>

              {/* Score */}
              <div className="mb-4">
                <div className="flex justify-between text-xs text-muted-foreground mb-1">
                  <span>Risk Score</span>
                  <span className="font-mono">{r.score}/100</span>
                </div>
                <div className="h-1.5 rounded-full bg-accent overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all ${r.score > 75 ? "bg-primary" : r.score > 40 ? "bg-muted-foreground" : "bg-success"}`}
                    style={{ width: `${r.score}%` }}
                  />
                </div>
              </div>

              {/* Metrics */}
              <div className="grid grid-cols-3 gap-3 text-center">
                <div className="bg-accent/30 rounded-lg p-3">
                  <Fingerprint className="w-3 h-3 text-muted-foreground mx-auto mb-1" />
                  <div className="text-xs font-mono">{r.entityReputation}</div>
                  <div className="text-[9px] text-muted-foreground">Entity Rep</div>
                </div>
                <div className="bg-accent/30 rounded-lg p-3">
                  <Network className="w-3 h-3 text-muted-foreground mx-auto mb-1" />
                  <div className="text-xs font-mono">{r.topologyDivergence}</div>
                  <div className="text-[9px] text-muted-foreground">Topology Div</div>
                </div>
                <div className="bg-accent/30 rounded-lg p-3">
                  <Lock className="w-3 h-3 text-muted-foreground mx-auto mb-1" />
                  <div className="text-xs font-mono">{r.privacyEntropy}</div>
                  <div className="text-[9px] text-muted-foreground">Privacy Entropy</div>
                </div>
              </div>

              {/* Fraud indicators */}
              {r.fraudIndicators.length > 0 && (
                <div className="mt-4 flex flex-wrap gap-1.5">
                  {r.fraudIndicators.map((fi) => (
                    <span key={fi} className="text-[10px] px-2 py-0.5 rounded bg-primary/10 text-primary font-mono">
                      {fi}
                    </span>
                  ))}
                </div>
              )}
            </motion.div>
          ))}
        </div>
      </div>
    </DashboardLayout>
  );
};

export default RiskResults;
