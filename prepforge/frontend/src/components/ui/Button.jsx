function Button({ children, className = "", variant = "primary", ...props }) {
  const styles = {
    primary:
      "bg-ember-500 text-white hover:bg-ember-400 focus:ring-ember-500/40",
    ghost:
      "border border-white/10 bg-white/5 text-slate-200 hover:bg-white/10 focus:ring-white/20",
  };

  return (
    <button
      className={`inline-flex items-center justify-center rounded-2xl px-4 py-3 text-sm font-semibold transition focus:outline-none focus:ring-2 ${styles[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}

export default Button;
