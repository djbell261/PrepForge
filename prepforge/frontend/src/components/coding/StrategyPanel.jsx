import { useEffect, useState } from "react";
import Button from "../ui/Button";

function StrategyPanel({
  evaluationError,
  evaluationResult,
  initialStrategy,
  isEvaluating,
  isSaving,
  onEvaluate,
  onSave,
  saveError,
}) {
  const [form, setForm] = useState({
    clarificationQuestions: "",
    plannedApproach: "",
    expectedTimeComplexity: "",
    expectedSpaceComplexity: "",
  });

  useEffect(() => {
    setForm({
      clarificationQuestions: initialStrategy?.clarificationQuestions || "",
      plannedApproach: initialStrategy?.plannedApproach || "",
      expectedTimeComplexity: initialStrategy?.expectedTimeComplexity || "",
      expectedSpaceComplexity: initialStrategy?.expectedSpaceComplexity || "",
    });
  }, [initialStrategy]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    await onSave(form);
  };

  const handleEvaluate = async () => {
    await onEvaluate(form);
  };

  return (
    <section className="panel overflow-hidden">
      <div className="border-b border-white/10 bg-gradient-to-r from-ember-500/10 via-white/0 to-white/0 px-6 py-5">
        <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Approach & Strategy</p>
        <h2 className="mt-2 text-2xl font-semibold text-white">Think like it&apos;s a real interview</h2>
        <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-400">
          Capture your clarifying questions, outline your plan, and commit to the complexity targets before you start coding.
        </p>
      </div>

      <form className="space-y-5 px-6 py-6" onSubmit={handleSubmit}>
        <FieldBlock
          label="Clarification Questions / Assumptions"
          name="clarificationQuestions"
          onChange={handleChange}
          placeholder="Can input contain null? Are we guaranteed one valid solution?"
          rows={4}
          value={form.clarificationQuestions}
        />

        <FieldBlock
          label="Planned Approach"
          name="plannedApproach"
          onChange={handleChange}
          placeholder="Use two pointers after filtering non-alphanumeric characters."
          rows={5}
          value={form.plannedApproach}
        />

        <div className="grid gap-4 md:grid-cols-2">
          <TextInput
            label="Expected Time Complexity"
            name="expectedTimeComplexity"
            onChange={handleChange}
            placeholder="O(n)"
            value={form.expectedTimeComplexity}
          />
          <TextInput
            label="Expected Space Complexity"
            name="expectedSpaceComplexity"
            onChange={handleChange}
            placeholder="O(1)"
            value={form.expectedSpaceComplexity}
          />
        </div>

        {saveError ? (
          <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{saveError}</p>
        ) : null}

        {evaluationError ? (
          <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{evaluationError}</p>
        ) : null}

        <div className="flex flex-wrap gap-3">
          <Button disabled={isSaving} type="submit">
            {isSaving ? "Saving strategy..." : "Save Strategy"}
          </Button>
          <Button
            disabled={isEvaluating || !form.plannedApproach.trim()}
            onClick={handleEvaluate}
            type="button"
            variant="ghost"
          >
            {isEvaluating ? "Evaluating approach..." : "Evaluate My Approach"}
          </Button>
        </div>
      </form>

      {evaluationResult ? (
        <div className="border-t border-white/10 px-6 py-6">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Approach Feedback</p>
          <div className="mt-5 grid gap-6 xl:grid-cols-[0.32fr,0.68fr]">
            <div className="rounded-3xl border border-ember-500/20 bg-ember-500/10 p-5">
              <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Score</p>
              <p className="mt-3 text-5xl font-bold text-white">
                {evaluationResult.score ?? "N/A"}
                <span className="ml-2 text-base font-medium text-slate-400">/ 10</span>
              </p>
              <p className="mt-4 text-sm leading-7 text-slate-300">
                Interviewer-style feedback on your thinking before implementation.
              </p>
            </div>

            <div className="space-y-4">
              <FeedbackCard title="Summary">
                <p className="leading-7 text-slate-300">
                  {evaluationResult.summary || "No summary available yet."}
                </p>
              </FeedbackCard>
              <FeedbackCard items={evaluationResult.strengths} title="Strengths" />
              <FeedbackCard items={evaluationResult.weaknesses} title="Weaknesses" />
              <FeedbackCard items={evaluationResult.recommendations} title="Recommendations" />
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
}

function FieldBlock({ label, ...props }) {
  return (
    <label className="block space-y-2">
      <span className="text-sm font-medium text-slate-200">{label}</span>
      <textarea
        className="min-h-[120px] w-full rounded-2xl border border-white/10 bg-forge-800 px-4 py-3 text-slate-100 placeholder:text-slate-500 ember-ring"
        {...props}
      />
    </label>
  );
}

function TextInput({ label, ...props }) {
  return (
    <label className="block space-y-2">
      <span className="text-sm font-medium text-slate-200">{label}</span>
      <input
        className="w-full rounded-2xl border border-white/10 bg-forge-800 px-4 py-3 text-slate-100 placeholder:text-slate-500 ember-ring"
        {...props}
      />
    </label>
  );
}

function FeedbackCard({ title, items, children }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
      <p className="font-semibold text-white">{title}</p>
      {children}
      {Array.isArray(items) ? (
        items.length ? (
          <ul className="mt-3 space-y-2">
            {items.map((item) => (
              <li key={item} className="rounded-xl bg-black/20 px-3 py-2 text-sm text-slate-300">
                {item}
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-2 text-sm text-slate-500">No {title.toLowerCase()} available.</p>
        )
      ) : null}
    </div>
  );
}

export default StrategyPanel;
