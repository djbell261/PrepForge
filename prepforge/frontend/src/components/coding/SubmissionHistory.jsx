import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";
import StatusBadge from "../ui/StatusBadge";
import EmptyState from "../ui/EmptyState";
import { formatDateTime } from "../../utils/formatters";
import { parseAiFeedback } from "../../utils/feedback";

function SubmissionHistory({ submissions }) {
  if (!submissions?.length) {
    return (
      <EmptyState
        title="No submissions yet"
        description="Once a solution is submitted, the code review trail and AI feedback will appear here."
      />
    );
  }

  return (
    <div className="space-y-4">
      {submissions.map((submission) => {
        const feedback = parseAiFeedback(submission.aiFeedback);
        const hasCode = Boolean(submission.code);
        const language = getCodeLanguage(submission.language);

        return (
          <div key={submission.submissionId} className="panel p-5">
            <div className="flex flex-col gap-3 border-b border-white/10 pb-4 md:flex-row md:items-center md:justify-between">
              <div>
                <h4 className="text-lg font-semibold text-white">
                  Submission #{submission.submissionId.slice(0, 8)}
                </h4>
                <p className="mt-1 text-sm text-slate-400">
                  {submission.language} · {formatDateTime(submission.submittedAt)}
                </p>
              </div>
              <StatusBadge value={submission.status} />
            </div>

            <div className="mt-5 grid gap-5 xl:grid-cols-[1.15fr,0.85fr]">
              {hasCode ? (
                <div className="overflow-hidden rounded-2xl border border-white/10 bg-forge-950">
                  <div className="border-b border-white/10 px-4 py-3 text-xs uppercase tracking-[0.3em] text-slate-500">
                    Submitted Code
                  </div>
                  <SyntaxHighlighter
                    customStyle={{
                      margin: 0,
                      padding: "1rem",
                      background: "transparent",
                      minHeight: "320px",
                    }}
                    language={language}
                    showLineNumbers
                    style={vscDarkPlus}
                    wrapLongLines
                  >
                    {submission.code}
                  </SyntaxHighlighter>
                </div>
              ) : (
                <div className="panel-soft flex min-h-[320px] items-center justify-center p-6 text-center">
                  <div>
                    <p className="text-sm font-semibold text-white">Source code unavailable</p>
                    <p className="mt-2 max-w-sm text-sm text-slate-400">
                      This submission does not include stored source code yet.
                    </p>
                  </div>
                </div>
              )}

              <div className="panel-soft p-4">
                <p className="text-xs uppercase tracking-[0.3em] text-ember-300">AI Feedback</p>
                <div className="mt-4 space-y-4 text-sm text-slate-300">
                  <div>
                    <p className="font-semibold text-white">Score</p>
                    <p className="mt-1 text-2xl font-bold text-ember-400">
                      {feedback?.score ?? "N/A"}
                      <span className="ml-1 text-sm text-slate-400">/ 10</span>
                    </p>
                  </div>

                  <div>
                    <p className="font-semibold text-white">Summary</p>
                    <p className="mt-1 leading-6 text-slate-300">
                      {feedback?.summary || "No AI feedback available yet."}
                    </p>
                  </div>

                  <FeedbackList title="Strengths" items={feedback?.strengths} />
                  <FeedbackList title="Weaknesses" items={feedback?.weaknesses} />
                  <FeedbackList title="Recommendations" items={feedback?.recommendations} />
                </div>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

function FeedbackList({ title, items = [] }) {
  return (
    <div>
      <p className="font-semibold text-white">{title}</p>
      {items.length ? (
        <ul className="mt-2 space-y-2 text-slate-300">
          {items.map((item) => (
            <li key={item} className="rounded-xl bg-white/5 px-3 py-2">
              {item}
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-1 text-slate-500">No {title.toLowerCase()} available.</p>
      )}
    </div>
  );
}

function getCodeLanguage(language) {
  const normalized = language?.toLowerCase();

  if (normalized === "java") return "java";
  if (normalized === "javascript") return "javascript";
  if (normalized === "python") return "python";
  if (normalized === "cpp" || normalized === "c++") return "cpp";

  return "java";
}

export default SubmissionHistory;
