function StatusBadge({ value }) {
  const normalized = value?.toUpperCase();
  const styles =
    normalized === "COMPLETED"
      ? "bg-emerald-500/15 text-emerald-300"
      : normalized === "REVIEWED"
        ? "bg-amber-500/15 text-amber-300"
        : "bg-white/10 text-slate-300";

  return (
    <span className={`rounded-full px-3 py-1 text-xs font-semibold tracking-wide ${styles}`}>
      {value || "Unknown"}
    </span>
  );
}

export default StatusBadge;
