function EmptyState({ title, description }) {
  return (
    <div className="panel-soft flex min-h-40 flex-col items-center justify-center px-6 py-10 text-center">
      <h3 className="text-lg font-semibold text-slate-100">{title}</h3>
      <p className="mt-2 max-w-md text-sm text-slate-400">{description}</p>
    </div>
  );
}

export default EmptyState;
