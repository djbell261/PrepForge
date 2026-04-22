function LoadingState({ label = "Loading..." }) {
  return (
    <div className="panel-soft flex min-h-40 items-center justify-center gap-3 px-6 py-10 text-slate-300">
      <span className="h-3 w-3 animate-pulse rounded-full bg-ember-500" />
      <span>{label}</span>
    </div>
  );
}

export default LoadingState;
