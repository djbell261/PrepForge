function Input({ label, error, className = "", ...props }) {
  return (
    <label className="block space-y-2">
      <span className="text-sm font-medium text-slate-200">{label}</span>
      <input
        className={`w-full rounded-2xl border border-white/10 bg-forge-800 px-4 py-3 text-slate-100 placeholder:text-slate-500 ember-ring ${className}`}
        {...props}
      />
      {error ? <p className="text-sm text-red-400">{error}</p> : null}
    </label>
  );
}

export default Input;
