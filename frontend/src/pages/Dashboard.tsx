import { motion } from "framer-motion";
import { Activity, AlertTriangle, ShieldCheck, TrendingUp } from "lucide-react";
import DashboardLayout from "@/components/DashboardLayout";

const stats = [
  { label: "Total Monitored", value: "12,847", icon: Activity, change: "+12.5%" },
  { label: "Flagged", value: "234", icon: AlertTriangle, change: "+3.2%", alert: true },
  { label: "Verified Safe", value: "12,613", icon: ShieldCheck, change: "+11.8%" },
  { label: "Risk Score Avg", value: "23.4", icon: TrendingUp, change: "-2.1%" },
];

const recentTransactions = [
  { id: "TXN-8A3F2", customer: "CUST-001", status: "Flagged", amount: "ENC_x7f2a9...", date: "2026-03-15 09:23" },
  { id: "TXN-7B1E9", customer: "CUST-042", status: "Verified", amount: "ENC_k3m8b1...", date: "2026-03-15 09:18" },
  { id: "TXN-6C4D8", customer: "CUST-119", status: "Verified", amount: "ENC_p9n2c4...", date: "2026-03-15 09:12" },
  { id: "TXN-5D2A7", customer: "CUST-088", status: "Flagged", amount: "ENC_q1r5d7...", date: "2026-03-15 09:05" },
  { id: "TXN-4E9B6", customer: "CUST-201", status: "Verified", amount: "ENC_s4t8e2...", date: "2026-03-15 08:58" },
  { id: "TXN-3F8C5", customer: "CUST-067", status: "Verified", amount: "ENC_u7v1f5...", date: "2026-03-15 08:51" },
];

const Dashboard = () => {
  return (
    <DashboardLayout>
      <div className="space-y-8">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-sm text-muted-foreground mt-1">AML monitoring overview</p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {stats.map((s, i) => (
            <motion.div
              key={s.label}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
              className={`bg-card rounded-xl p-6 card-surface ${s.alert ? "border-l-2 border-l-primary" : ""}`}
            >
              <div className="flex items-center justify-between mb-4">
                <s.icon className={`w-5 h-5 ${s.alert ? "text-primary" : "text-muted-foreground"}`} />
                <span className={`text-xs font-mono ${s.change.startsWith("+") && s.alert ? "text-primary" : "text-muted-foreground"}`}>
                  {s.change}
                </span>
              </div>
              <div className="text-2xl font-bold tracking-tight">{s.value}</div>
              <div className="text-xs text-muted-foreground mt-1">{s.label}</div>
            </motion.div>
          ))}
        </div>

        {/* Risk distribution bar */}
        <div className="bg-card rounded-xl p-6 card-surface">
          <h3 className="text-sm font-semibold mb-4">Risk Distribution</h3>
          <div className="flex rounded-full overflow-hidden h-3 bg-accent">
            <div className="bg-success h-full" style={{ width: "65%" }} />
            <div className="bg-muted-foreground h-full" style={{ width: "20%" }} />
            <div className="bg-primary h-full" style={{ width: "15%" }} />
          </div>
          <div className="flex justify-between mt-3 text-xs text-muted-foreground">
            <span className="flex items-center gap-1.5"><span className="w-2 h-2 rounded-full bg-success" />Low (65%)</span>
            <span className="flex items-center gap-1.5"><span className="w-2 h-2 rounded-full bg-muted-foreground" />Medium (20%)</span>
            <span className="flex items-center gap-1.5"><span className="w-2 h-2 rounded-full bg-primary" />High (15%)</span>
          </div>
        </div>

        {/* Recent transactions table */}
        <div className="bg-card rounded-xl card-surface overflow-hidden">
          <div className="p-6 border-b border-border">
            <h3 className="text-sm font-semibold">Recent Activity</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left p-4 text-xs font-mono uppercase tracking-wider text-muted-foreground">Transaction ID</th>
                  <th className="text-left p-4 text-xs font-mono uppercase tracking-wider text-muted-foreground">Customer</th>
                  <th className="text-left p-4 text-xs font-mono uppercase tracking-wider text-muted-foreground">Status</th>
                  <th className="text-left p-4 text-xs font-mono uppercase tracking-wider text-muted-foreground">Encrypted Amount</th>
                  <th className="text-left p-4 text-xs font-mono uppercase tracking-wider text-muted-foreground">Date</th>
                </tr>
              </thead>
              <tbody>
                {recentTransactions.map((tx) => (
                  <tr key={tx.id} className="border-b border-border/50 hover:bg-accent/30 transition-colors">
                    <td className="p-4 font-mono text-xs">{tx.id}</td>
                    <td className="p-4 font-mono text-xs">{tx.customer}</td>
                    <td className="p-4">
                      <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${
                        tx.status === "Flagged"
                          ? "text-primary bg-primary/10"
                          : "text-success bg-success/10"
                      }`}>
                        {tx.status}
                      </span>
                    </td>
                    <td className="p-4 font-mono text-xs text-muted-foreground">{tx.amount}</td>
                    <td className="p-4 text-xs text-muted-foreground">{tx.date}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default Dashboard;
