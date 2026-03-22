import { motion } from "framer-motion";
import { ArrowRight, Building2, Lock, Bot, ShieldAlert, FileCheck } from "lucide-react";

const steps = [
  { icon: Building2, title: "Bank Transaction", desc: "Raw transaction enters the pipeline" },
  { icon: Lock, title: "Encrypted Amount", desc: "Amount encrypted via FHE scheme" },
  { icon: Bot, title: "AI Agent Decision", desc: "LangChain agent analyzes patterns" },
  { icon: ShieldAlert, title: "FHE Threshold Check", desc: "Encrypted threshold comparison" },
  { icon: FileCheck, title: "Risk Verdict", desc: "Final compliance determination" },
];

const HowItWorksSection = () => {
  return (
    <section className="py-32 relative">
      <div className="container px-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-20"
        >
          <p className="text-xs uppercase tracking-[0.2em] text-primary font-mono mb-4">Pipeline</p>
          <h2 className="text-3xl md:text-5xl font-bold tracking-tighter">How It Works</h2>
        </motion.div>

        <div className="flex flex-col md:flex-row items-center justify-center gap-4 max-w-5xl mx-auto">
          {steps.map((step, i) => (
            <motion.div
              key={step.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1, duration: 0.5 }}
              className="flex items-center gap-4"
            >
              <div className="flex flex-col items-center text-center min-w-[140px]">
                <div className="w-14 h-14 rounded-xl bg-card card-surface flex items-center justify-center mb-3 hover:glow-primary transition-shadow duration-300">
                  <step.icon className="w-6 h-6 text-primary" />
                </div>
                <h3 className="text-sm font-semibold mb-1">{step.title}</h3>
                <p className="text-xs text-muted-foreground">{step.desc}</p>
              </div>
              {i < steps.length - 1 && (
                <ArrowRight className="w-4 h-4 text-muted-foreground hidden md:block shrink-0" />
              )}
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default HowItWorksSection;
