import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import EmptyState from "../components/ui/EmptyState";
import LoadingState from "../components/ui/LoadingState";
import Button from "../components/ui/Button";
import StatusBadge from "../components/ui/StatusBadge";
import { behavioralService } from "../services/behavioralService";
import { extractApiErrorMessage } from "../services/api";
import { useAuth } from "../hooks/useAuth";
import { formatDateTime } from "../utils/formatters";

function BehavioralSessionPage() {
  const { sessionId } = useParams();
  const { token, isAuthenticated } = useAuth();
  const [detail, setDetail] = useState(null);
  const [responseText, setResponseText] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isImproving, setIsImproving] = useState(false);
  const [improveError, setImproveError] = useState("");
  const [pollingError, setPollingError] = useState("");
  const [sessionSummary, setSessionSummary] = useState(null);
  const [summaryError, setSummaryError] = useState("");
  const [now, setNow] = useState(Date.now());

  const loadDetail = async ({ showLoading = true } = {}) => {
    if (showLoading) {
      setIsLoading(true);
      setError("");
    }

    try {
      const response = await behavioralService.getSessionDetail(sessionId);
      setDetail(response);
      setResponseText((current) => current || response?.submissions?.[0]?.responseText || "");
      setPollingError("");
      return response;
    } catch (requestError) {
      const message = extractApiErrorMessage(
        requestError,
        "We couldn't load this behavioral session. It may not exist or you may not have access.",
      );

      if (showLoading) {
        setError(message);
      } else {
        setPollingError("We couldn't refresh your feedback just now. We'll keep trying.");
      }

      throw requestError;
    } finally {
      if (showLoading) {
        setIsLoading(false);
      }
    }
  };

  const loadSummary = async () => {
    setSummaryError("");

    try {
      const response = await behavioralService.getSessionSummary(sessionId);
      setSessionSummary(response);
    } catch (requestError) {
      setSummaryError(
        extractApiErrorMessage(
          requestError,
          "We couldn't load your session summary right now.",
        ),
      );
    }
  };

  useEffect(() => {
    if (!token || !isAuthenticated) {
      setIsLoading(false);
      return;
    }

    loadDetail().catch(() => {});
  }, [sessionId, token, isAuthenticated]);

  useEffect(() => {
    if (!detail?.session?.isTimed || detail?.session?.expired) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => window.clearInterval(intervalId);
  }, [detail?.session?.isTimed, detail?.session?.expired]);

  const latestFeedback = detail?.submissions?.[0]?.feedback ?? null;
  const latestSubmission = detail?.submissions?.[0] ?? null;
  const latestFeedbackStatus = latestSubmission?.feedbackStatus ?? null;
  const latestFeedbackFailed = latestFeedbackStatus === "FAILED";
  const latestFeedbackPending = latestFeedbackStatus === "PENDING";
  const previousFeedback = detail?.submissions?.[1]?.feedback ?? null;
  const latestScore = latestFeedback?.score ?? null;
  const previousScore = previousFeedback?.score ?? null;
  const isImprovingTrend =
    latestScore !== null && latestScore !== undefined && previousScore !== null && previousScore !== undefined
      ? latestScore > previousScore
      : false;

  const expiresAtMs =
    detail?.session?.isTimed && detail?.session?.timeLimitSeconds
      ? new Date(detail.session.startedAt).getTime() + detail.session.timeLimitSeconds * 1000
      : null;
  const timeRemainingMs = expiresAtMs ? Math.max(expiresAtMs - now, 0) : null;
  const sessionExpired =
    Boolean(detail?.session?.expired) || (expiresAtMs !== null ? timeRemainingMs === 0 : false);

  useEffect(() => {
    if (!token || !isAuthenticated || !latestSubmission?.submissionId || !latestFeedbackPending) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      loadDetail({ showLoading: false }).catch(() => {});
    }, 2500);

    return () => window.clearInterval(intervalId);
  }, [sessionId, token, isAuthenticated, latestSubmission?.submissionId, latestFeedbackPending]);

  useEffect(() => {
    if (!token || !isAuthenticated || !latestSubmission?.submissionId || latestFeedbackPending) {
      return;
    }

    loadSummary();
  }, [sessionId, token, isAuthenticated, latestSubmission?.submissionId, latestFeedbackPending, latestFeedbackFailed]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitError("");
    setPollingError("");
    setIsSubmitting(true);

    try {
      await behavioralService.submitResponse(sessionId, { responseText });
      await loadDetail({ showLoading: false });
      setSessionSummary(null);
    } catch (requestError) {
      setSubmitError(
        extractApiErrorMessage(
          requestError,
          "We couldn't submit your behavioral response. Please revise it and try again.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleImproveAnswer = async () => {
    setImproveError("");
    setIsImproving(true);

    try {
      const response = await behavioralService.improveResponse({
        responseText,
        feedback: {
          strengths: latestFeedback?.strengths || [],
          weaknesses: latestFeedback?.weaknesses || [],
        },
      });

      setResponseText(response.improvedResponse || responseText);
    } catch (requestError) {
      setImproveError(
        extractApiErrorMessage(
          requestError,
          "We couldn't improve this answer right now. Please try again.",
        ),
      );
    } finally {
      setIsImproving(false);
    }
  };

  const handleRefreshSession = async () => {
    const response = await loadDetail();
    const hasLatestSubmission = Boolean(response?.submissions?.[0]?.submissionId);
    const isLatestPending = response?.submissions?.[0]?.feedbackStatus === "PENDING";

    if (hasLatestSubmission && !isLatestPending) {
      await loadSummary();
    }
  };

  const guidanceSteps = useMemo(
    () => [
      {
        label: "Situation",
        description: "Set the scene quickly so the interviewer understands the context and stakes.",
      },
      {
        label: "Task",
        description: "Explain the responsibility you owned or the problem you needed to solve.",
      },
      {
        label: "Action",
        description: "Focus on the specific decisions and actions you took, not what the team did broadly.",
      },
      {
        label: "Result",
        description: "Close with measurable impact, what changed, and what you learned from it.",
      },
    ],
    [],
  );

  if (isLoading) {
    return <LoadingState label="Preparing behavioral session..." />;
  }

  if (error) {
    return (
      <div className="space-y-4">
        <EmptyState title="Behavioral session unavailable" description={error} />
        <div className="flex justify-center">
          <Button onClick={handleRefreshSession} variant="ghost">
            Retry session load
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Behavioral Session</p>
          <h1 className="mt-3 text-3xl font-bold text-white">Tell the story clearly and with impact.</h1>
        </div>
        <Link className="text-sm font-semibold text-ember-400 hover:text-ember-300" to="/behavioral">
          Back to behavioral practice
        </Link>
      </div>

      <section className="grid gap-6 lg:grid-cols-[1.05fr,0.95fr]">
        <div className="panel p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap gap-2">
              <Tag>{detail?.question?.category}</Tag>
              <Tag tone="ember">{detail?.question?.difficulty}</Tag>
            </div>
            <StatusBadge value={detail?.session?.status} />
          </div>

          <p className="mt-5 text-xs uppercase tracking-[0.3em] text-slate-500">Question Prompt</p>
          <div className="mt-3 rounded-3xl border border-white/10 bg-forge-800/70 p-5">
            <p className="text-xl font-semibold leading-8 text-white">{detail?.question?.questionText}</p>
            <p className="mt-4 text-sm leading-7 text-slate-400">
              Started {formatDateTime(detail?.session?.startedAt)}. Answer in a way that feels ready to say aloud in a live interview.
            </p>
          </div>

          <div className="mt-6 rounded-3xl border border-white/10 bg-white/5 p-5">
            <p className="text-xs uppercase tracking-[0.3em] text-ember-300">STAR Guidance</p>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              {guidanceSteps.map((step) => (
                <div key={step.label} className="rounded-2xl border border-white/10 bg-black/20 p-4">
                  <p className="text-sm font-semibold text-white">{step.label}</p>
                  <p className="mt-2 text-sm leading-7 text-slate-400">{step.description}</p>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="panel p-6">
          {detail?.session?.isTimed ? (
            <div className={`mb-5 rounded-3xl border px-5 py-4 ${
              sessionExpired
                ? "border-red-500/20 bg-red-500/10"
                : timeRemainingMs !== null && timeRemainingMs <= 2 * 60 * 1000
                  ? "border-red-500/20 bg-red-500/10"
                  : "border-amber-500/20 bg-amber-500/10"
            }`}>
              <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Pressure Mode</p>
              <div className="mt-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-sm leading-7 text-slate-300">
                  {sessionExpired
                    ? "Time is up. You can keep revising, but submitting a new attempt is locked."
                    : "Answer as if you are live with an interviewer and a clock is running."}
                </p>
                <div className="rounded-2xl border border-white/10 bg-black/20 px-4 py-3 text-center">
                  <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Time Remaining</p>
                  <p className={`mt-2 text-2xl font-semibold ${sessionExpired ? "text-red-300" : "text-white"}`}>
                    {formatCountdown(timeRemainingMs)}
                  </p>
                </div>
              </div>
            </div>
          ) : null}

          <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Your Response</p>
          <h2 className="mt-3 text-2xl font-semibold text-white">Write your interview answer</h2>
          <p className="mt-3 text-sm leading-7 text-slate-400">
            Aim for clear structure, concrete actions, and a result that shows impact.
          </p>

          <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
            <div className="grid gap-4 xl:grid-cols-[1fr,0.42fr]">
              <textarea
                className="min-h-[320px] w-full rounded-3xl border border-white/10 bg-forge-800 px-4 py-4 text-slate-100 placeholder:text-slate-500 ember-ring"
                placeholder="Describe the situation, your responsibility, the actions you took, and the measurable result."
                value={responseText}
                onChange={(event) => setResponseText(event.target.value)}
              />

              <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
                <p className="text-xs uppercase tracking-[0.3em] text-ember-300">STAR Sidecar</p>
                <div className="mt-4 space-y-3">
                  {guidanceSteps.map((step) => (
                    <div key={step.label} className="rounded-2xl border border-white/10 bg-black/20 p-4">
                      <p className="text-sm font-semibold text-white">{step.label}</p>
                      <p className="mt-2 text-sm leading-6 text-slate-400">{step.description}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {submitError ? (
              <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{submitError}</p>
            ) : null}

            {pollingError ? (
              <p className="rounded-2xl bg-amber-500/10 px-4 py-3 text-sm text-amber-200">{pollingError}</p>
            ) : null}

            {improveError ? (
              <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{improveError}</p>
            ) : null}

            {sessionExpired ? (
              <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">
                This timed behavioral session has expired. You can keep editing your answer, but new submissions are locked.
              </p>
            ) : null}

            <div className="flex flex-wrap gap-3">
              <Button disabled={isSubmitting || sessionExpired || !responseText.trim()} type="submit">
                {isSubmitting ? "Evaluating answer..." : "Submit Answer"}
              </Button>
              <Button disabled={isImproving || !responseText.trim()} onClick={handleImproveAnswer} type="button" variant="ghost">
                {isImproving ? "Improving answer..." : "Improve My Answer"}
              </Button>
              <Button onClick={handleRefreshSession} type="button" variant="ghost">
                Refresh Session
              </Button>
            </div>
          </form>
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[0.95fr,1.05fr]">
        <div className="panel p-6">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Latest Feedback</p>
          <h2 className="mt-3 text-2xl font-semibold text-white">How interview-ready did this sound?</h2>

          {latestFeedbackPending ? (
            <div className="mt-6 space-y-5">
              <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 p-4">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Feedback Status</p>
                    <p className="mt-2 text-lg font-semibold text-white">Generating AI feedback...</p>
                  </div>
                  <PendingBadge label="Pending" />
                </div>
                <p className="mt-3 text-sm leading-7 text-slate-300">
                  Your answer was saved. PrepForge is reviewing clarity, STAR structure, and impact now.
                </p>
                <div className="mt-4 space-y-3">
                  <LoadingSkeleton />
                  <LoadingSkeleton />
                  <LoadingSkeleton compact />
                </div>
              </div>
            </div>
          ) : latestFeedbackFailed ? (
            <div className="mt-6">
              <div className="rounded-2xl border border-red-500/20 bg-red-500/10 p-4">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-xs uppercase tracking-[0.3em] text-red-200">Feedback Status</p>
                    <p className="mt-2 text-lg font-semibold text-white">Feedback generation failed</p>
                  </div>
                  <FailureBadge label="Failed" />
                </div>
                <p className="mt-3 text-sm leading-7 text-slate-300">
                  {latestSubmission?.feedbackFailureMessage ||
                    "We saved your answer, but the AI feedback did not finish this time. You can keep editing and try again later."}
                </p>
              </div>
            </div>
          ) : latestFeedback ? (
            <div className="mt-6 space-y-5">
              <div className={`rounded-2xl border p-4 ${getScoreTone(latestScore)}`}>
                <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Score</p>
                <p className={`mt-2 text-4xl font-bold ${getScoreTextTone(latestScore)}`}>
                  {latestFeedback.score ?? "N/A"}
                  <span className="ml-2 text-base font-medium text-slate-400">/ 10</span>
                </p>
                {isImprovingTrend ? (
                  <p className="mt-3 text-sm font-medium text-emerald-300">You&apos;re improving.</p>
                ) : null}
              </div>

              <FeedbackBlock title="Summary" text={latestFeedback.summary} />
              {Array.isArray(latestFeedback.improvements) && latestFeedback.improvements.length ? (
                <FeedbackList title="What Improved?" items={latestFeedback.improvements} />
              ) : null}
              {Array.isArray(latestFeedback.regressions) && latestFeedback.regressions.length ? (
                <FeedbackList title="Regressions" items={latestFeedback.regressions} />
              ) : null}
              <FeedbackList title="Strengths" items={latestFeedback.strengths} />
              <FeedbackList title="Weaknesses" items={latestFeedback.weaknesses} />
              <FeedbackList title="Recommendations" items={latestFeedback.recommendations} />
            </div>
          ) : (
            <div className="mt-6">
              <EmptyState
                title="No feedback yet"
                description="Submit your written answer to receive structured behavioral interview coaching."
              />
            </div>
          )}
        </div>

        <div className="panel p-6">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Retry History</p>
          <h2 className="mt-3 text-2xl font-semibold text-white">Track how your answer improves</h2>

          {detail?.submissions?.length ? (
            <div className="mt-6 space-y-4">
              {detail.submissions.map((submission, index) => (
                <article key={submission.submissionId} className="rounded-3xl border border-white/10 bg-white/5 p-5">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <p className="text-xs uppercase tracking-[0.25em] text-ember-300">Attempt {detail.submissions.length - index}</p>
                      <p className="mt-2 text-sm text-slate-400">{formatDateTime(submission.submittedAt)}</p>
                    </div>
                    {submission.feedbackStatus === "PENDING" ? (
                      <PendingBadge label="Feedback Pending" />
                    ) : submission.feedbackStatus === "FAILED" ? (
                      <FailureBadge label="Feedback Failed" />
                    ) : (
                      <div className="rounded-2xl border border-white/10 bg-black/20 px-4 py-3 text-sm text-white">
                        Score: {submission.feedback?.score ?? "N/A"} / 10
                      </div>
                    )}
                  </div>

                  <div className="mt-4 grid gap-4 lg:grid-cols-[1fr,0.9fr]">
                    <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
                      <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Response</p>
                      <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-slate-300">{submission.responseText}</p>
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
                      <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Summary</p>
                      {submission.feedbackStatus === "PENDING" ? (
                        <div className="mt-3 space-y-3">
                          <p className="text-sm leading-7 text-amber-200">Generating AI feedback...</p>
                          <LoadingSkeleton />
                          <LoadingSkeleton compact />
                        </div>
                      ) : submission.feedbackStatus === "FAILED" ? (
                        <p className="mt-3 text-sm leading-7 text-red-200">
                          {submission.feedbackFailureMessage || "Feedback generation failed for this attempt."}
                        </p>
                      ) : (
                        <p className="mt-3 text-sm leading-7 text-slate-300">
                          {submission.feedback?.summary || "No summary available."}
                        </p>
                      )}
                      <div className="mt-4">
                        <Button type="button" variant="ghost" onClick={() => setResponseText(submission.responseText)}>
                          Reuse This Draft
                        </Button>
                      </div>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="mt-6">
              <EmptyState
                title="No attempts yet"
                description="Your submission history will appear here so you can see how your stories get sharper over time."
              />
            </div>
          )}
        </div>
      </section>

      <section className="panel p-6">
        <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Session Summary</p>
        <h2 className="mt-2 text-2xl font-semibold text-white">Your final coaching block</h2>
        {latestFeedbackPending ? (
          <div className="mt-6 rounded-2xl border border-amber-500/20 bg-amber-500/10 p-4">
            <p className="text-sm leading-7 text-slate-300">
              Your summary will appear after PrepForge finishes reviewing this latest response.
            </p>
          </div>
        ) : summaryError ? (
          <p className="mt-5 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{summaryError}</p>
        ) : sessionSummary ? (
          <div className="mt-6 grid gap-5 xl:grid-cols-[1.15fr,0.85fr]">
            <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
              <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Summary</p>
              <p className="mt-3 text-sm leading-7 text-slate-300">{sessionSummary.summary}</p>
            </div>
            <div className="grid gap-4">
              <SummaryListCard title="What Went Well" items={sessionSummary.strengths} />
              <SummaryListCard title="What Went Wrong" items={sessionSummary.weaknesses} />
              <SummaryListCard title="What To Practice Next" items={sessionSummary.nextSteps} />
            </div>
          </div>
        ) : (
          <div className="mt-6">
            <EmptyState
              title="Summary warming up"
              description="Finish one reviewed attempt and your session summary will appear here."
            />
          </div>
        )}
      </section>
    </div>
  );
}

function Tag({ children, tone = "slate" }) {
  const styles =
    tone === "ember"
      ? "border-ember-500/20 bg-ember-500/10 text-ember-300"
      : "border-white/10 bg-black/20 text-slate-300";

  return (
    <span className={`rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.25em] ${styles}`}>
      {children}
    </span>
  );
}

function FeedbackBlock({ title, text }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
      <p className="font-semibold text-white">{title}</p>
      <p className="mt-3 text-sm leading-7 text-slate-300">{text || "No summary available."}</p>
    </div>
  );
}

function PendingBadge({ label }) {
  return (
    <div className="rounded-2xl border border-amber-500/20 bg-amber-500/10 px-4 py-3 text-sm font-medium text-amber-200">
      {label}
    </div>
  );
}

function FailureBadge({ label }) {
  return (
    <div className="rounded-2xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm font-medium text-red-200">
      {label}
    </div>
  );
}

function LoadingSkeleton({ compact = false }) {
  return (
    <div className={`animate-pulse rounded-2xl border border-white/10 bg-white/5 ${compact ? "h-12" : "h-20"}`} />
  );
}

function SummaryListCard({ title, items }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
      <p className="font-semibold text-white">{title}</p>
      {Array.isArray(items) && items.length ? (
        <ul className="mt-3 space-y-2">
          {items.map((item) => (
            <li key={item} className="rounded-xl bg-black/20 px-3 py-2 text-sm leading-7 text-slate-300">
              {item}
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-3 text-sm text-slate-500">No summary points yet.</p>
      )}
    </div>
  );
}

function FeedbackList({ title, items, emptyLabel }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
      <p className="font-semibold text-white">{title}</p>
      {Array.isArray(items) && items.length ? (
        <ul className="mt-3 space-y-2">
          {items.map((item) => (
            <li key={item} className="rounded-xl bg-black/20 px-3 py-2 text-sm leading-7 text-slate-300">
              {item}
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-3 text-sm text-slate-500">{emptyLabel || `No ${title.toLowerCase()} available.`}</p>
      )}
    </div>
  );
}

export default BehavioralSessionPage;

function getScoreTone(score) {
  if (score === null || score === undefined) {
    return "border-white/10 bg-white/5";
  }

  if (score <= 5) {
    return "border-red-500/20 bg-red-500/10";
  }

  if (score <= 7) {
    return "border-amber-500/20 bg-amber-500/10";
  }

  return "border-emerald-500/20 bg-emerald-500/10";
}

function getScoreTextTone(score) {
  if (score === null || score === undefined) {
    return "text-white";
  }

  if (score <= 5) {
    return "text-red-300";
  }

  if (score <= 7) {
    return "text-amber-300";
  }

  return "text-emerald-300";
}

function formatCountdown(timeRemainingMs) {
  if (timeRemainingMs === null || timeRemainingMs === undefined) {
    return "--:--";
  }

  const totalSeconds = Math.floor(timeRemainingMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}
