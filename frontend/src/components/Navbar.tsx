import { useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import { Menu, X, Shield } from "lucide-react";
import { Button } from "@/components/ui/button";

const Navbar = () => {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <motion.header
      initial={{ y: -20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.4, ease: [0.2, 0, 0, 1] }}
      className="fixed top-0 left-0 right-0 z-50 glass-strong"
    >
      <div className="container flex items-center justify-between h-16">
        <Link to="/" className="flex items-center gap-2">
          <img src="/public/logo.png" alt="Logo" className="h-8" />
          <span className="text-lg font-bold tracking-tighter">CIPHER</span>
        </Link>

        <nav className="hidden md:flex items-center gap-8">
          <a href="#features" className="text-sm text-muted-foreground hover:text-foreground transition-colors duration-200">
            Why Us
          </a>
          <a href="#contact" className="text-sm text-muted-foreground hover:text-foreground transition-colors duration-200">
            Contact
          </a>
          <Link to="/login">
            <Button variant="ghost" size="sm" className="text-muted-foreground hover:text-foreground">
              Login
            </Button>
          </Link>
          <Link to="/signup">
            <Button size="sm" className="bg-primary hover:bg-secondary text-primary-foreground glow-primary transition-all duration-200">
              Sign Up
            </Button>
          </Link>
        </nav>

        <button
          className="md:hidden text-foreground"
          onClick={() => setMobileOpen(!mobileOpen)}
        >
          {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
        </button>
      </div>

      {mobileOpen && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: "auto" }}
          className="md:hidden border-t border-border"
        >
          <div className="container py-4 flex flex-col gap-3">
            <a href="#features" className="text-sm text-muted-foreground py-2" onClick={() => setMobileOpen(false)}>Why Us</a>
            <a href="#contact" className="text-sm text-muted-foreground py-2" onClick={() => setMobileOpen(false)}>Contact</a>
            <Link to="/login" className="text-sm text-muted-foreground py-2" onClick={() => setMobileOpen(false)}>Login</Link>
            <Link to="/signup" onClick={() => setMobileOpen(false)}>
              <Button size="sm" className="w-full bg-primary hover:bg-secondary text-primary-foreground">Sign Up</Button>
            </Link>
          </div>
        </motion.div>
      )}
    </motion.header>
  );
};

export default Navbar;
