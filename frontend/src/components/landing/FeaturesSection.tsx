import { motion } from "framer-motion";
import { Lock, Bot, BarChart3, ShieldCheck } from "lucide-react";

const features = [
  {
    icon: Lock,
    title: "Encrypted Transaction Processing",
    description: "Process financial data while it remains fully encrypted using Fully Homomorphic Encryption. Zero plaintext exposure.",
  },
  {
    icon: Bot,
    title: "AI Compliance Agent",
    description: "LangChain-powered AI agents autonomously analyze transaction patterns and flag anomalies in real-time.",
  },
  {
    icon: BarChart3,
    title: "Risk Analysis Engine",
    description: "Multi-dimensional risk scoring across topology, behavioral, and temporal vectors with cryptographic guarantees.",
  },
  {
    icon: ShieldCheck,
    title: "Secure AML Monitoring",
    description: "Continuous monitoring pipeline that meets regulatory requirements without compromising data privacy.",
  },
];

const container = {
  hidden: {},
  show: { transition: { staggerChildren: 0.1 } },
};

const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.2, 0, 0, 1] as const } },
};

const FeaturesSection = () => {
  return (
    <section id="features" className="py-32 relative">
      <div className="container px-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5 }}
          className="text-center mb-16"
        >
          <p className="text-xs uppercase tracking-[0.2em] text-primary font-mono mb-4">Capabilities</p>
          <h2 className="text-3xl md:text-5xl font-bold tracking-tighter">What It Does</h2>
        </motion.div>

        <motion.div
          variants={container}
          initial="hidden"
          whileInView="show"
          viewport={{ once: true }}
          className="grid md:grid-cols-2 gap-6 max-w-5xl mx-auto"
        >
          {features.map((f) => (
            <motion.div
              key={f.title}
              variants={item}
              className="bg-card rounded-xl p-8 card-surface hover:bg-accent/50 transition-colors duration-300 group"
            >
              <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center mb-5 group-hover:glow-primary transition-shadow duration-300">
                <f.icon className="w-5 h-5 text-primary" />
              </div>
              <h3 className="text-lg font-semibold tracking-tight mb-2">{f.title}</h3>
              <p className="text-sm text-muted-foreground leading-relaxed">{f.description}</p>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </section>
  );
};

export default FeaturesSection;
