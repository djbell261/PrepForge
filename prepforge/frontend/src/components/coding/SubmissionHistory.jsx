import Editor from "@monaco-editor/react";
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
              <div className="overflow-hidden rounded-2xl border border-white/10">
                <Editor
                  height="320px"
                  defaultLanguage={submission.language || "javascript"}
                  value={submission.solutionCode || "// Submission source will appear here later."}
                  options={{
                    readOnly: true,
                    minimap: { enabled: false },
                    lineNumbers: "on",
                    fontSize: 14,
                    scrollBeyondLastLine: false,
                    wordWrap: "on",
                  }}
                  theme="vs-dark"
                />
              </div>

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

export default SubmissionHistory;
