import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Shield, Mail, Lock, Building2, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";

const Signup = () => {
  const [form, setForm] = useState({ bankName: "", email: "", password: "", confirm: "" });
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();
  const navigate = useNavigate();

  const update = (key: string, val: string) => setForm((f) => ({ ...f, [key]: val }));

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (form.password !== form.confirm) {
      toast({ title: "Error", description: "Passwords don't match", variant: "destructive" });
      return;
    }
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      toast({ title: "Account created", description: "Welcome to CIPHER" });
      navigate("/dashboard");
    }, 1000);
  };

  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden">
      <div className="absolute inset-0 grid-bg opacity-20" />
      <div className="absolute top-1/3 left-1/2 -translate-x-1/2 w-[400px] h-[400px] bg-primary/5 rounded-full blur-[100px]" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative z-10 w-full max-w-md px-4"
      >
        <div className="text-center mb-8">
          <Link to="/" className="inline-flex items-center gap-2 mb-6">
            <Shield className="w-6 h-6 text-primary" />
            <span className="text-xl font-bold tracking-tighter">CIPHER</span>
          </Link>
          <h1 className="text-2xl font-bold tracking-tight">Create your account</h1>
          <p className="text-sm text-muted-foreground mt-1">Start monitoring transactions securely</p>
        </div>

        <form onSubmit={handleSubmit} className="glass rounded-xl p-8 space-y-4">
          <div className="space-y-2">
            <Label className="text-sm text-muted-foreground">Bank / Institution Name</Label>
            <div className="relative">
              <Building2 className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input value={form.bankName} onChange={(e) => update("bankName", e.target.value)} className="pl-10 bg-input border-border" placeholder="Acme Bank" required />
            </div>
          </div>
          <div className="space-y-2">
            <Label className="text-sm text-muted-foreground">Email</Label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input type="email" value={form.email} onChange={(e) => update("email", e.target.value)} className="pl-10 bg-input border-border" placeholder="analyst@bank.com" required />
            </div>
          </div>
          <div className="space-y-2">
            <Label className="text-sm text-muted-foreground">Password</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input type="password" value={form.password} onChange={(e) => update("password", e.target.value)} className="pl-10 bg-input border-border" placeholder="••••••••" required />
            </div>
          </div>
          <div className="space-y-2">
            <Label className="text-sm text-muted-foreground">Confirm Password</Label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input type="password" value={form.confirm} onChange={(e) => update("confirm", e.target.value)} className="pl-10 bg-input border-border" placeholder="••••••••" required />
            </div>
          </div>

          <Button type="submit" disabled={loading} className="w-full bg-primary hover:bg-secondary text-primary-foreground glow-primary gap-2">
            {loading ? "Creating..." : "Create Account"}
            {!loading && <ArrowRight className="w-4 h-4" />}
          </Button>

          <p className="text-center text-sm text-muted-foreground">
            Already have an account? <Link to="/login" className="text-primary hover:text-secondary transition-colors">Login</Link>
          </p>
        </form>
      </motion.div>
    </div>
  );
};

export default Signup;
