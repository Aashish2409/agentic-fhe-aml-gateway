import { Shield } from "lucide-react";

const Footer = () => (
  <footer className="border-t border-border py-12">
    <div className="container px-4 flex flex-col md:flex-row items-center justify-between gap-4">
      <div className="flex items-center gap-2">
        <Shield className="w-4 h-4 text-primary" />
        <span className="text-sm font-bold tracking-tighter">CIPHER</span>
      </div>
      <p className="text-xs text-muted-foreground">
        © 2026 CIPHER. Privacy-Preserving AML Intelligence. All rights reserved.
      </p>
    </div>
  </footer>
);

export default Footer;
