import { useState } from "react";
import { motion } from "framer-motion";
import DashboardLayout from "@/components/DashboardLayout";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search, ChevronLeft, ChevronRight } from "lucide-react";

const allLogs = Array.from({ length: 40 }, (_, i) => ({
  id: `TXN-${String(1000 + i).slice(1)}${["A", "B", "C", "D", "E"][i % 5]}${Math.floor(Math.random() * 9)}`,
  customer: `CUST-${String(i + 1).padStart(3, "0")}`,
  amount: `ENC_${Math.random().toString(36).slice(2, 8)}...`,
  status: i % 5 === 0 || i % 7 === 0 ? "Flagged" : "Verified",
  verdict: i % 5 === 0 || i % 7 === 0 ? "SUSPICIOUS" : "CLEARED",
  timestamp: `2026-03-${String(15 - Math.floor(i / 5)).padStart(2, "0")} ${String(9 + (i % 12)).padStart(2, "0")}:${String((i * 7) % 60).padStart(2, "0")}`,
}));

const PAGE_SIZE = 10;

const SystemLogs = () => {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>("all");

  const filtered = allLogs.filter((l) => {
    const matchesSearch = !search || l.id.toLowerCase().includes(search.toLowerCase()) || l.customer.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === "all" || l.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const paged = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">System Logs</h1>
          <p className="text-sm text-muted-foreground mt-1">Immutable record of all AML analyses</p>
        </div>

        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              placeholder="Search by ID or customer..."
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              className="pl-10 bg-input border-border"
            />
          </div>
          <div className="flex gap-2">
            {["all", "Flagged", "Verified"].map((s) => (
              <Button
                key={s}
                size="sm"
                variant={statusFilter === s ? "default" : "outline"}
                onClick={() => { setStatusFilter(s); setPage(0); }}
                className={statusFilter === s ? "bg-primary hover:bg-secondary text-primary-foreground" : "border-border"}
              >
                {s === "all" ? "All" : s}
              </Button>
            ))}
          </div>
        </div>

        {/* Table */}
        <div className="bg-card rounded-xl card-surface overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="sticky top-0 bg-card/80 backdrop-blur">
                <tr className="border-b border-border">
                  {["Transaction ID", "Customer", "Amount", "Status", "Verdict", "Timestamp"].map((h) => (
                    <th key={h} className="text-left p-4 text-xs font-mono uppercase tracking-wider text-muted-foreground">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {paged.map((log, i) => (
                  <motion.tr
                    key={log.id + i}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: i * 0.02 }}
                    className="border-b border-border/50 hover:bg-accent/30 transition-colors"
                  >
                    <td className="p-4 font-mono text-xs">{log.id}</td>
                    <td className="p-4 font-mono text-xs">{log.customer}</td>
                    <td className="p-4 font-mono text-xs text-muted-foreground">{log.amount}</td>
                    <td className="p-4">
                      <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${
                        log.status === "Flagged" ? "text-primary bg-primary/10" : "text-success bg-success/10"
                      }`}>
                        {log.status}
                      </span>
                    </td>
                    <td className={`p-4 text-xs font-mono font-medium ${log.verdict === "SUSPICIOUS" ? "text-primary" : "text-success"}`}>
                      {log.verdict}
                    </td>
                    <td className="p-4 text-xs text-muted-foreground">{log.timestamp}</td>
                  </motion.tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between p-4 border-t border-border">
            <span className="text-xs text-muted-foreground">
              Showing {page * PAGE_SIZE + 1}-{Math.min((page + 1) * PAGE_SIZE, filtered.length)} of {filtered.length}
            </span>
            <div className="flex gap-2">
              <Button size="sm" variant="outline" disabled={page === 0} onClick={() => setPage(page - 1)} className="border-border">
                <ChevronLeft className="w-4 h-4" />
              </Button>
              <Button size="sm" variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)} className="border-border">
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default SystemLogs;
