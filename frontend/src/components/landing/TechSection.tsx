import { motion } from "framer-motion";

const techs = [
  {
    title: "Fully Homomorphic Encryption",
    desc: "Compute on encrypted data without decryption. Mathematical guarantees for data privacy.",
    tag: "CRYPTOGRAPHY",
  },
  {
    title: "LangChain AI Agents",
    desc: "Autonomous reasoning agents that analyze transaction patterns and make compliance decisions.",
    tag: "AI / ML",
  },
  {
    title: "Spring Boot Backend",
    desc: "Enterprise-grade backend infrastructure with high-throughput transaction processing.",
    tag: "INFRASTRUCTURE",
  },
  {
    title: "Privacy-First Compliance",
    desc: "Meet GDPR, PCI-DSS, and AML regulations while keeping all data encrypted end-to-end.",
    tag: "COMPLIANCE",
  },
];

const TechSection = () => {
  return (
    <section className="py-32 relative">
      <div className="container px-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-16"
        >
          <p className="text-xs uppercase tracking-[0.2em] text-primary font-mono mb-4">Technology</p>
          <h2 className="text-3xl md:text-5xl font-bold tracking-tighter">Built on Proven Foundations</h2>
        </motion.div>

        <div className="grid md:grid-cols-2 gap-px bg-border rounded-xl overflow-hidden max-w-4xl mx-auto">
          {techs.map((t, i) => (
            <motion.div
              key={t.title}
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className="bg-card p-8 hover:bg-accent/30 transition-colors duration-300"
            >
              <span className="text-[10px] font-mono uppercase tracking-[0.2em] text-primary">{t.tag}</span>
              <h3 className="text-lg font-semibold mt-3 mb-2 tracking-tight">{t.title}</h3>
              <p className="text-sm text-muted-foreground leading-relaxed">{t.desc}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default TechSection;
