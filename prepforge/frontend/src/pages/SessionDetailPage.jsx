import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import SubmissionHistory from "../components/coding/SubmissionHistory";
import CodeDiffViewer from "../components/coding/CodeDiffViewer";
import RunResultsPanel from "../components/coding/RunResultsPanel";
import StrategyPanel from "../components/coding/StrategyPanel";
import EmptyState from "../components/ui/EmptyState";
import LoadingState from "../components/ui/LoadingState";
import StatusBadge from "../components/ui/StatusBadge";
import { useAuth } from "../hooks/useAuth";
import { codingService } from "../services/codingService";
import { formatDateTime } from "../utils/formatters";
import Button from "../components/ui/Button";
import { extractApiErrorMessage } from "../services/api";
import { parseAiFeedback } from "../utils/feedback";
import Editor from "@monaco-editor/react";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";

function SessionDetailPage() {
  const { sessionId } = useParams();
  const { token, isAuthenticated } = useAuth();
  const [detail, setDetail] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [submissionForm, setSubmissionForm] = useState({
    language: "java",
    solutionCode: "",
  });
  const [strategy, setStrategy] = useState(null);
  const [strategyError, setStrategyError] = useState("");
  const [isSavingStrategy, setIsSavingStrategy] = useState(false);
  const [strategyEvaluation, setStrategyEvaluation] = useState(null);
  const [strategyEvaluationError, setStrategyEvaluationError] = useState("");
  const [isEvaluatingStrategy, setIsEvaluatingStrategy] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [runResult, setRunResult] = useState(null);
  const [runError, setRunError] = useState("");
  const [isRunning, setIsRunning] = useState(false);
  const [latestFeedback, setLatestFeedback] = useState(null);
  const [approachComparison, setApproachComparison] = useState(null);
  const [approachComparisonError, setApproachComparisonError] = useState("");
  const [isComparingApproach, setIsComparingApproach] = useState(false);
  const [improvedResult, setImprovedResult] = useState(null);
  const [improveError, setImproveError] = useState("");
  const [isImproving, setIsImproving] = useState(false);
  const [now, setNow] = useState(Date.now());
  const editorRef = useRef(null);
  const monacoRef = useRef(null);
  const latestSubmission = detail?.submissions?.[0] ?? null;

  const clearCompileMarkers = () => {
    const editor = editorRef.current;
    const monaco = monacoRef.current;
    const model = editor?.getModel?.();

    if (!monaco || !model) {
      return;
    }

    monaco.editor.setModelMarkers(model, "prepforge-compile", []);
  };

  const applyCompileMarker = (compileError) => {
    const editor = editorRef.current;
    const monaco = monacoRef.current;
    const model = editor?.getModel?.();

    if (!editor || !monaco || !model || !compileError?.line) {
      clearCompileMarkers();
      return;
    }

    const lineNumber = Math.min(Math.max(compileError.line, 1), model.getLineCount());
    const maxColumn = model.getLineMaxColumn(lineNumber);
    const startColumn = Math.min(Math.max(compileError.column || 1, 1), maxColumn);
    const endColumn = Math.min(startColumn + 1, maxColumn);

    monaco.editor.setModelMarkers(model, "prepforge-compile", [
      {
        startLineNumber: lineNumber,
        endLineNumber: lineNumber,
        startColumn,
        endColumn,
        message: compileError.message || "Compilation failed.",
        severity: monaco.MarkerSeverity.Error,
      },
    ]);

    editor.revealLineInCenter(lineNumber);
    editor.setPosition({ lineNumber, column: startColumn });
  };

  const loadDetail = async () => {
    setIsLoading(true);
    setError("");

    try {
      const response = await codingService.getSessionDetail(sessionId);
      setDetail(response);
      setStrategy(response?.strategy || null);
      const newestSubmission = response?.submissions?.[0];
      setLatestFeedback(newestSubmission ? parseAiFeedback(newestSubmission.aiFeedback) : null);
      setStrategyEvaluation(null);
      setStrategyEvaluationError("");
      setApproachComparison(null);
      setApproachComparisonError("");
      setRunResult(null);
      setRunError("");
      setImprovedResult(null);
      setImproveError("");
    } catch (requestError) {
      setError(
        extractApiErrorMessage(
          requestError,
          "We couldn't load this coding session. It may not exist or you may not have access.",
        ),
      );
    } finally {
      setIsLoading(false);
    }
  };

  const loadApproachComparison = async () => {
    setApproachComparisonError("");
    setIsComparingApproach(true);

    try {
      const response = await codingService.compareApproach(sessionId);
      setApproachComparison(response);
    } catch (requestError) {
      setApproachComparison(null);
      setApproachComparisonError(
        extractApiErrorMessage(
          requestError,
          "We couldn't compare your approach and implementation right now.",
        ),
      );
    } finally {
      setIsComparingApproach(false);
    }
  };

  const handleSaveStrategy = async (payload) => {
    setStrategyError("");
    setStrategyEvaluation(null);
    setStrategyEvaluationError("");
    setApproachComparison(null);
    setApproachComparisonError("");
    setIsSavingStrategy(true);

    try {
      const response = await codingService.saveStrategy(sessionId, payload);
      setStrategy(response);
    } catch (requestError) {
      setStrategyError(
        extractApiErrorMessage(
          requestError,
          "We couldn't save your strategy right now. Please try again.",
        ),
      );
    } finally {
      setIsSavingStrategy(false);
    }
  };

  const handleEvaluateStrategy = async (payload) => {
    setStrategyEvaluationError("");
    setStrategyEvaluation(null);
    setIsEvaluatingStrategy(true);

    try {
      const response = await codingService.evaluateStrategy(sessionId, payload);
      setStrategyEvaluation(response);
    } catch (requestError) {
      setStrategyEvaluationError(
        extractApiErrorMessage(
          requestError,
          "We couldn't evaluate your approach right now. Please try again.",
        ),
      );
    } finally {
      setIsEvaluatingStrategy(false);
    }
  };

  useEffect(() => {
    if (!token || !isAuthenticated) {
      setIsLoading(false);
      return;
    }

    loadDetail();
  }, [sessionId, token, isAuthenticated]);

  useEffect(() => {
    if (!detail?.session?.timedMode || !detail?.session?.expiresAt || detail?.session?.expired) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => window.clearInterval(intervalId);
  }, [detail?.session?.timedMode, detail?.session?.expiresAt, detail?.session?.expired]);

  useEffect(() => {
    if (!token || !isAuthenticated) {
      return;
    }

    if (latestSubmission?.submissionId && strategy?.plannedApproach?.trim()) {
      loadApproachComparison();
      return;
    }

    setApproachComparison(null);
    setApproachComparisonError("");
  }, [latestSubmission?.submissionId, strategy?.plannedApproach, sessionId, token, isAuthenticated]);

  useEffect(() => {
    if (runResult?.compileError) {
      applyCompileMarker(runResult.compileError);
      return;
    }

    clearCompileMarkers();
  }, [runResult]);

  const handleLanguageChange = (event) => {
    setSubmissionForm((current) => ({
      ...current,
      language: event.target.value,
    }));
  };

  const handleCodeChange = (value) => {
    clearCompileMarkers();

    setSubmissionForm((current) => ({
      ...current,
      solutionCode: value || "",
    }));
  };

  const handleEditorMount = (editor, monaco) => {
    editorRef.current = editor;
    monacoRef.current = monaco;

    if (runResult?.compileError) {
      applyCompileMarker(runResult.compileError);
    }
  };

  const handleSubmitSolution = async (event) => {
    event.preventDefault();
    setSubmitError("");
    setIsSubmitting(true);

    try {
      const response = await codingService.submitSolution(sessionId, {
        language: submissionForm.language,
        solutionCode: submissionForm.solutionCode,
      });

      setLatestFeedback(parseAiFeedback(response.aiFeedback));
      setImprovedResult(null);
      setImproveError("");
      await loadDetail();
    } catch (requestError) {
      setSubmitError(
        extractApiErrorMessage(
          requestError,
          "We couldn't submit your code. Make sure the solution is filled in and try again.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRunCode = async () => {
    setRunError("");
    setIsRunning(true);

    try {
      const response = await codingService.runCode(sessionId, {
        language: submissionForm.language,
        solutionCode: submissionForm.solutionCode,
      });

      setRunResult(response);
    } catch (requestError) {
      setRunError(
        extractApiErrorMessage(
          requestError,
          "We couldn't run your code right now. Please try again.",
        ),
      );
      setRunResult(null);
    } finally {
      setIsRunning(false);
    }
  };

  const handleImproveCode = async () => {
    if (!latestSubmission?.submissionId) {
      return;
    }

    setImproveError("");
    setIsImproving(true);

    try {
      const response = await codingService.improveSubmission(latestSubmission.submissionId);
      setImprovedResult(response);
    } catch (requestError) {
      setImproveError(
        extractApiErrorMessage(
          requestError,
          "We couldn't improve this submission right now. Please try again.",
        ),
      );
    } finally {
      setIsImproving(false);
    }
  };

  if (isLoading) {
    return <LoadingState label="Loading session detail..." />;
  }

  if (error) {
    return (
      <div className="space-y-4">
        <EmptyState title="Session unavailable" description={error} />
        <div className="flex justify-center">
          <Button onClick={loadDetail} variant="ghost">
            Retry session load
          </Button>
        </div>
      </div>
    );
  }

  const expiresAtMs = detail?.session?.expiresAt ? new Date(detail.session.expiresAt).getTime() : null;
  const timeRemainingMs = expiresAtMs ? Math.max(expiresAtMs - now, 0) : null;
  const sessionExpired =
    Boolean(detail?.session?.expired) || (expiresAtMs !== null ? timeRemainingMs === 0 : false);
  const isTimedSession = Boolean(detail?.session?.timedMode);
  const timerTone =
    sessionExpired
      ? "border-red-500/30 bg-red-500/10"
      : timeRemainingMs !== null && timeRemainingMs <= 5 * 60 * 1000
        ? "border-red-500/30 bg-red-500/10"
        : timeRemainingMs !== null && timeRemainingMs <= 10 * 60 * 1000
          ? "border-amber-500/30 bg-amber-500/10"
          : "border-ember-500/20 bg-ember-500/10";

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Session Detail</p>
          <h1 className="mt-3 text-3xl font-bold text-white">
            {detail?.question?.title || "Coding Session"}
          </h1>
        </div>
        <Link className="text-sm font-semibold text-ember-400 hover:text-ember-300" to="/dashboard">
          Back to dashboard
        </Link>
      </div>

      <section className="grid gap-6 lg:grid-cols-[0.9fr,1.1fr]">
        <div className="panel p-6">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Session Info</p>
          <div className="mt-5 space-y-4 text-sm text-slate-300">
            <DetailRow label="Session ID" value={detail?.session?.sessionId} mono />
            <DetailRow label="Started At" value={formatDateTime(detail?.session?.startedAt)} />
            <div className="flex items-center justify-between">
              <span className="text-slate-500">Status</span>
              <StatusBadge value={detail?.session?.status} />
            </div>
          </div>
        </div>

        <div className="panel p-6">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Question Info</p>
          <div className="mt-5 space-y-4">
            <div>
              <p className="text-sm text-slate-500">Difficulty</p>
              <p className="mt-1 text-lg font-semibold text-white">{detail?.question?.difficulty || "Unknown"}</p>
            </div>
            <div>
              <p className="text-sm text-slate-500">Prompt</p>
              <div className="mt-2 rounded-2xl border border-white/10 bg-forge-800/70 p-4 leading-7 text-slate-200">
                {detail?.question?.prompt || "No prompt available."}
              </div>
            </div>
          </div>
        </div>
      </section>

      {isTimedSession ? (
        <section className={`panel overflow-hidden border ${timerTone}`}>
          <div className="flex flex-col gap-5 px-6 py-6 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-xs uppercase tracking-[0.35em] text-ember-300">Timed Assessment</p>
              <h2 className="mt-3 text-3xl font-bold text-white">
                {sessionExpired ? "Time expired" : "Assessment in progress"}
              </h2>
              <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-400">
                This session started with a {detail?.session?.durationMinutes}-minute countdown.
                {sessionExpired
                  ? " The workspace remains visible for review, but new submissions are now locked."
                  : " Treat this like a real interview round and keep your pacing deliberate."}
              </p>
            </div>

            <div className="rounded-3xl border border-white/10 bg-black/20 px-6 py-5 text-center">
              <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Time Remaining</p>
              <p className={`mt-3 text-5xl font-bold ${sessionExpired ? "text-red-300" : "text-white"}`}>
                {formatCountdown(timeRemainingMs)}
              </p>
              <p className="mt-3 text-sm text-slate-400">
                {sessionExpired ? "Submission window closed" : "Finish strong under pressure"}
              </p>
            </div>
          </div>
        </section>
      ) : null}

      <StrategyPanel
        evaluationError={strategyEvaluationError}
        evaluationResult={strategyEvaluation}
        initialStrategy={strategy}
        isEvaluating={isEvaluatingStrategy}
        isSaving={isSavingStrategy}
        onEvaluate={handleEvaluateStrategy}
        onSave={handleSaveStrategy}
        saveError={strategyError}
      />

      <section className="grid gap-6 xl:grid-cols-[1.05fr,0.95fr]">
        <div className="panel p-6">
          <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Code Workspace</p>
          <h2 className="mt-3 text-2xl font-semibold text-white">Write and submit your solution</h2>
          <p className="mt-3 text-sm leading-7 text-slate-400">
            Draft your answer below, submit it for AI review, and use the feedback to improve the next attempt.
          </p>

          <form className="mt-6 space-y-4" onSubmit={handleSubmitSolution}>
            <label className="block space-y-2">
              <span className="text-sm font-medium text-slate-200">Language</span>
              <select
                className="w-full rounded-2xl border border-white/10 bg-forge-800 px-4 py-3 text-slate-100 ember-ring"
                value={submissionForm.language}
                onChange={handleLanguageChange}
              >
                <option value="java">Java</option>
                <option value="javascript">JavaScript</option>
                <option value="python">Python</option>
                <option value="cpp">C++</option>
              </select>
            </label>

            <div className="overflow-hidden rounded-2xl border border-white/10">
              <Editor
                height="360px"
                defaultLanguage="java"
                language={submissionForm.language}
                value={submissionForm.solutionCode}
                onChange={handleCodeChange}
                onMount={handleEditorMount}
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  scrollBeyondLastLine: false,
                  wordWrap: "on",
                }}
                theme="vs-dark"
              />
            </div>

            {submitError ? (
              <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{submitError}</p>
            ) : null}

            <div className="flex flex-wrap gap-3">
              <Button disabled={isRunning || !submissionForm.solutionCode.trim()} onClick={handleRunCode} type="button" variant="ghost">
                {isRunning ? "Running..." : "Run Code"}
              </Button>
              <Button disabled={isSubmitting || sessionExpired || !submissionForm.solutionCode.trim()} type="submit">
                {sessionExpired ? "Submission Closed" : isSubmitting ? "Submitting for review..." : "Submit Code"}
              </Button>
              <Button onClick={loadDetail} type="button" variant="ghost">
                Refresh Session
              </Button>
            </div>

            {sessionExpired ? (
              <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">
                This timed assessment has expired. You can still review your code and feedback, but new submissions are blocked.
              </p>
            ) : null}

            {runError ? (
              <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{runError}</p>
            ) : null}
          </form>
        </div>

        <div className="panel p-6">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Latest Review</p>
          <h2 className="mt-3 text-2xl font-semibold text-white">AI feedback snapshot</h2>
          {latestFeedback ? (
            <div className="mt-6 space-y-5 text-sm text-slate-300">
              <div className="rounded-2xl border border-ember-500/20 bg-ember-500/10 p-4">
                <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Score</p>
                <p className="mt-2 text-4xl font-bold text-white">
                  {latestFeedback.score ?? "N/A"}
                  <span className="ml-2 text-base font-medium text-slate-400">/ 10</span>
                </p>
              </div>

              <FeedbackSection title="Summary">
                <p className="leading-7 text-slate-300">
                  {latestFeedback.summary || "No summary available yet."}
                </p>
              </FeedbackSection>

              <FeedbackSection title="Strengths" items={latestFeedback.strengths} />
              <FeedbackSection title="Weaknesses" items={latestFeedback.weaknesses} />
              <FeedbackSection title="Recommendations" items={latestFeedback.recommendations} />

              <div className="rounded-2xl border border-white/10 bg-forge-950 overflow-hidden">
                <div className="border-b border-white/10 px-4 py-3">
                  <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Latest Submitted Code</p>
                </div>
                {latestSubmission?.code ? (
                  <SyntaxHighlighter
                    customStyle={{
                      margin: 0,
                      padding: "1rem",
                      background: "transparent",
                      maxHeight: "320px",
                    }}
                    language={getCodeLanguage(latestSubmission.language)}
                    showLineNumbers
                    style={vscDarkPlus}
                    wrapLongLines
                  >
                    {latestSubmission.code}
                  </SyntaxHighlighter>
                ) : (
                  <div className="px-4 py-6 text-sm text-slate-400">
                    No submitted code is available for the latest attempt yet.
                  </div>
                )}
              </div>

              <div className="space-y-3">
                <div className="flex flex-wrap gap-3">
                  <Button
                    disabled={isImproving || !latestSubmission?.submissionId || !latestSubmission?.code}
                    onClick={handleImproveCode}
                    type="button"
                  >
                    {isImproving ? "Improving code..." : "Improve My Code"}
                  </Button>
                </div>

                {improveError ? (
                  <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{improveError}</p>
                ) : null}
              </div>
            </div>
          ) : (
            <div className="mt-6">
              <EmptyState
                title="No AI review yet"
                description="Submit your code to generate a score, summary, strengths, weaknesses, and recommendations."
              />
            </div>
          )}
        </div>
      </section>

      {latestFeedback ? (
        <section className="panel p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Approach vs Implementation</p>
              <h2 className="mt-2 text-2xl font-semibold text-white">Did your code match what you said?</h2>
            </div>
            {isComparingApproach ? (
              <p className="text-sm text-slate-400">Comparing latest submission...</p>
            ) : null}
          </div>

          {approachComparisonError ? (
            <p className="mt-5 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">
              {approachComparisonError}
            </p>
          ) : null}

          {approachComparison ? (
            <div className="mt-6 grid gap-5 xl:grid-cols-[0.32fr,0.68fr]">
              <div className="rounded-2xl border border-ember-500/20 bg-ember-500/10 p-4">
                <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Alignment Score</p>
                <p className="mt-2 text-4xl font-bold text-white">
                  {approachComparison.alignmentScore ?? "N/A"}
                  <span className="ml-2 text-base font-medium text-slate-400">/ 10</span>
                </p>
              </div>

              <div className="space-y-4">
                <FeedbackSection title="Summary">
                  <p className="leading-7 text-slate-300">
                    {approachComparison.summary || "No summary available yet."}
                  </p>
                </FeedbackSection>

                <FeedbackSection title="Matches">
                  <p className="leading-7 text-slate-300">
                    {approachComparison.matches || "No strong matches identified."}
                  </p>
                </FeedbackSection>

                <FeedbackSection title="Mismatches">
                  <p className="leading-7 text-slate-300">
                    {approachComparison.mismatches || "No major mismatches identified."}
                  </p>
                </FeedbackSection>

                <FeedbackSection title="Improvement Areas">
                  <p className="leading-7 text-slate-300">
                    {approachComparison.improvementAreas || "No improvement areas identified."}
                  </p>
                </FeedbackSection>

                <FeedbackSection title="Final Verdict">
                  <p className="leading-7 text-slate-300">
                    {approachComparison.finalVerdict || "No final verdict available yet."}
                  </p>
                </FeedbackSection>
              </div>
            </div>
          ) : null}
        </section>
      ) : null}

      {runResult ? <RunResultsPanel runResult={runResult} /> : null}

      {improvedResult ? (
        <section className="panel overflow-hidden border border-ember-500/15 shadow-ember">
          <div className="border-b border-white/10 bg-gradient-to-r from-ember-500/12 via-white/0 to-white/0 px-6 py-6 sm:px-8">
            <div className="flex flex-col gap-5 xl:flex-row xl:items-end xl:justify-between">
              <div>
                <p className="text-xs uppercase tracking-[0.35em] text-ember-300">AI Improved Solution</p>
                <h2 className="mt-3 text-3xl font-bold text-white">A sharper version of your latest submission</h2>
                <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-400">
                  Compare your original answer with the refined version, then use the improvement notes to understand what changed and why.
                </p>
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                <ComplexityCard
                  accent="text-ember-300"
                  label="Time Complexity"
                  value={improvedResult.timeComplexity}
                />
                <ComplexityCard
                  accent="text-slate-300"
                  label="Space Complexity"
                  value={improvedResult.spaceComplexity}
                />
              </div>
            </div>
          </div>

          {latestSubmission?.code && improvedResult.improvedCode ? (
            <div className="px-6 py-6 sm:px-8">
              <CodeDiffViewer
                improvedCode={improvedResult.improvedCode}
                language={latestSubmission?.language}
                originalCode={latestSubmission?.code}
              />
            </div>
          ) : null}

          <div className="border-t border-white/10 px-6 py-6 sm:px-8">
            <div className="grid gap-6 xl:grid-cols-[1.15fr,0.85fr]">
              <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
                <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Improvement Summary</p>
                <h3 className="mt-3 text-xl font-semibold text-white">What changed</h3>

                {Array.isArray(improvedResult.explanation) && improvedResult.explanation.length ? (
                  <div className="mt-5 grid gap-3 sm:grid-cols-2">
                    {improvedResult.explanation.map((item, index) => (
                      <div
                        key={item}
                        className="rounded-2xl border border-white/10 bg-black/20 px-4 py-4"
                      >
                        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-ember-300">
                          Step {index + 1}
                        </p>
                        <p className="mt-3 text-sm leading-7 text-slate-300">{item}</p>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="mt-4 text-sm leading-7 text-slate-400">
                    No improvement summary is available yet.
                  </p>
                )}
              </div>

              <div className="rounded-3xl border border-white/10 bg-forge-800/60 p-5">
                <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Review Lens</p>
                <h3 className="mt-3 text-xl font-semibold text-white">What this optimization focused on</h3>
                <div className="mt-5 space-y-3">
                  <FocusChip label="Cleaner structure" />
                  <FocusChip label="More readable naming" />
                  <FocusChip label="Practical interview best practices" />
                  <FocusChip label="Complexity-awareness" />
                </div>
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <section className="space-y-4">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Submission History</p>
          <h2 className="mt-2 text-2xl font-semibold text-white">AI-reviewed attempts</h2>
        </div>
        <SubmissionHistory submissions={detail?.submissions} />
      </section>
    </div>
  );
}

function DetailRow({ label, value, mono = false }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <span className="text-slate-500">{label}</span>
      <span className={`text-right text-slate-200 ${mono ? "font-mono text-xs sm:text-sm" : ""}`}>{value}</span>
    </div>
  );
}

function FeedbackSection({ title, items, children }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
      <p className="font-semibold text-white">{title}</p>
      {children}
      {Array.isArray(items) ? (
        items.length ? (
          <ul className="mt-3 space-y-2">
            {items.map((item) => (
              <li key={item} className="rounded-xl bg-black/20 px-3 py-2">
                {item}
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-2 text-slate-500">No {title.toLowerCase()} available.</p>
        )
      ) : null}
    </div>
  );
}

function ComplexityCard({ label, value, accent }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-4">
      <p className="text-xs uppercase tracking-[0.25em] text-slate-500">{label}</p>
      <p className={`mt-3 text-2xl font-semibold ${accent}`}>{value || "Not available"}</p>
    </div>
  );
}

function FocusChip({ label }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
      {label}
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

function formatCountdown(timeRemainingMs) {
  if (timeRemainingMs === null || timeRemainingMs === undefined) {
    return "--:--";
  }

  const totalSeconds = Math.floor(timeRemainingMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;

  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

export default SessionDetailPage;
