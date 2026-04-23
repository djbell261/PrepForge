import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { codingService } from "../services/codingService";
import { behavioralService } from "../services/behavioralService";
import { dashboardService } from "../services/dashboardService";
import StatsOverview from "../components/dashboard/StatsOverview";
import RecentSessionsList from "../components/dashboard/RecentSessionsList";
import EmptyState from "../components/ui/EmptyState";
import LoadingState from "../components/ui/LoadingState";
import { useAuth } from "../hooks/useAuth";
import Button from "../components/ui/Button";
import { extractApiErrorMessage } from "../services/api";

function DashboardPage() {
  const navigate = useNavigate();
  const { user, token, isAuthenticated } = useAuth();
  const [analytics, setAnalytics] = useState(null);
  const [behavioralAnalytics, setBehavioralAnalytics] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [coachingSummary, setCoachingSummary] = useState(null);
  const [coachingSummaryError, setCoachingSummaryError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [startForm, setStartForm] = useState({
    questionId: "",
    timedMode: false,
    durationMinutes: 30,
  });
  const [startError, setStartError] = useState("");
  const [isStartingSession, setIsStartingSession] = useState(false);

  const loadDashboard = async () => {
    setIsLoading(true);
    setError("");
    setCoachingSummaryError("");

    try {
      const [
        analyticsResponse,
        behavioralAnalyticsResponse,
        sessionsResponse,
        questionsResponse,
        coachingSummaryResult,
      ] = await Promise.allSettled([
        codingService.getAnalytics(),
        behavioralService.getAnalytics(),
        codingService.getSessions(),
        codingService.getQuestions(),
        dashboardService.getCoachingSummary(),
      ]);

      if (
        analyticsResponse.status !== "fulfilled" ||
        behavioralAnalyticsResponse.status !== "fulfilled" ||
        sessionsResponse.status !== "fulfilled" ||
        questionsResponse.status !== "fulfilled"
      ) {
        const rejectedResult = [
          analyticsResponse,
          behavioralAnalyticsResponse,
          sessionsResponse,
          questionsResponse,
        ].find((result) => result.status === "rejected");
        throw rejectedResult.reason;
      }

      setAnalytics(analyticsResponse.value);
      setBehavioralAnalytics(behavioralAnalyticsResponse.value);
      setSessions(sessionsResponse.value);
      setQuestions(questionsResponse.value);
      setStartForm((current) => ({
        questionId: current.questionId || questionsResponse.value[0]?.id || "",
        timedMode: current.timedMode ?? false,
        durationMinutes: current.durationMinutes || 30,
      }));

      if (coachingSummaryResult.status === "fulfilled") {
        setCoachingSummary(coachingSummaryResult.value);
      } else {
        setCoachingSummary(null);
        setCoachingSummaryError(
          extractApiErrorMessage(
            coachingSummaryResult.reason,
            "We couldn't load your combined coaching summary right now.",
          ),
        );
      }
    } catch (requestError) {
      setError(extractApiErrorMessage(requestError, "We couldn't load your dashboard right now. Please try again."));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!token || !isAuthenticated) {
      setIsLoading(false);
      return;
    }

    loadDashboard();
  }, [token, isAuthenticated]);

  const handleStartFormChange = (event) => {
    const { name, value } = event.target;

    setStartForm((current) => ({
      ...current,
      [name]: value,
    }));
  };

  const handleModeChange = (timedMode) => {
    setStartForm((current) => ({
      ...current,
      timedMode,
    }));
  };

  const handleStartSession = async (event) => {
    event.preventDefault();
    setStartError("");
    setIsStartingSession(true);

    try {
      const session = await codingService.startSession({
        questionId: startForm.questionId,
        timedMode: startForm.timedMode,
        durationMinutes: startForm.timedMode ? Number(startForm.durationMinutes) : null,
      });
      navigate(`/sessions/${session.sessionId}`);
    } catch (requestError) {
      setStartError(
        extractApiErrorMessage(
          requestError,
          "We couldn't start a coding session. Choose a question and try again.",
        ),
      );
    } finally {
      setIsStartingSession(false);
    }
  };

  if (isLoading) {
    return <LoadingState label="Heating up your dashboard..." />;
  }

  if (error) {
    return (
      <div className="space-y-4">
        <EmptyState title="Dashboard unavailable" description={error} />
        <div className="flex justify-center">
          <Button onClick={loadDashboard} variant="ghost">
            Retry dashboard load
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <section className="panel overflow-hidden p-6 sm:p-8">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.35em] text-ember-300">Forge Console</p>
            <h1 className="mt-3 text-4xl font-bold text-white">Welcome back, {user?.name || "builder"}.</h1>
            <p className="mt-3 max-w-2xl text-base leading-7 text-slate-400">
              Keep your coding reps sharp with a focused dashboard for analytics, recent sessions, and AI-reviewed progress.
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="panel-soft px-5 py-4">
              <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Account</p>
              <p className="mt-2 text-sm font-medium text-white">{user?.email || "No email available"}</p>
            </div>
            <div className="panel-soft px-5 py-4">
              <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Role</p>
              <p className="mt-2 text-sm font-medium text-white">{user?.role || "USER"}</p>
            </div>
          </div>
        </div>
      </section>

      <section className="panel p-6 sm:p-7">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Coaching Summary</p>
            <h2 className="mt-3 text-2xl font-semibold text-white">What PrepForge wants you to work on next</h2>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-400">
              This combines your latest coding and behavioral outcomes into one practical coaching direction.
            </p>
          </div>
          {coachingSummary?.recommendedSessionType ? (
            <div className="rounded-2xl border border-ember-500/20 bg-ember-500/10 px-4 py-3 text-sm font-semibold text-ember-200">
              Focus next: {coachingSummary.recommendedSessionType}
            </div>
          ) : null}
        </div>

        {coachingSummaryError ? (
          <p className="mt-5 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{coachingSummaryError}</p>
        ) : coachingSummary ? (
          <div className="mt-6 grid gap-5 xl:grid-cols-[1.1fr,0.9fr]">
            <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
              <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Headline</p>
              <p className="mt-3 text-lg font-semibold leading-8 text-white">{coachingSummary.headline}</p>

              <div className="mt-5 grid gap-4 sm:grid-cols-2">
                <InsightCard label="Strongest Area" value={coachingSummary.strongestArea} />
                <InsightCard label="Weakest Area" value={coachingSummary.weakestArea} />
              </div>
            </div>

            <div className="grid gap-4">
              <InsightCard label="Recommended Next Step" value={coachingSummary.recommendedNextStep} />
              <InsightCard label="Pressure Insight" value={coachingSummary.pressureInsight} />
              <InsightCard label="Confidence Note" value={coachingSummary.confidenceNote} />
            </div>
          </div>
        ) : (
          <div className="mt-6">
            <EmptyState
              title="Coaching summary warming up"
              description="Finish a few reviewed coding or behavioral reps and this dashboard block will start pointing you toward the next best practice move."
            />
          </div>
        )}
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.1fr,0.9fr]">
        <div className="panel p-6 sm:p-7">
          <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Start Session</p>
          <h2 className="mt-3 text-2xl font-semibold text-white">Open a new coding rep</h2>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-400">
            Pick a challenge from the forge queue and PrepForge will open a live coding session immediately.
          </p>

          <form className="mt-6 space-y-4" onSubmit={handleStartSession}>
            <label className="block space-y-2">
              <span className="text-sm font-medium text-slate-200">Coding Question</span>
              <select
                className="w-full rounded-2xl border border-white/10 bg-forge-800 px-4 py-3 text-slate-100 ember-ring disabled:cursor-not-allowed disabled:opacity-60"
                name="questionId"
                value={startForm.questionId}
                onChange={handleStartFormChange}
                disabled={!questions.length || isStartingSession}
                required
              >
                {!questions.length ? (
                  <option value="">No coding questions available</option>
                ) : null}

                {questions.map((question) => (
                  <option key={question.id} value={question.id}>
                    {question.title} · {question.difficulty}
                  </option>
                ))}
              </select>
            </label>

            <div className="space-y-2">
              <span className="text-sm font-medium text-slate-200">Assessment Mode</span>
              <div className="grid gap-3 md:grid-cols-2">
                <ModeCard
                  active={!startForm.timedMode}
                  description="Keep the full interview workflow without countdown pressure."
                  onClick={() => handleModeChange(false)}
                  title="Practice Mode"
                />
                <ModeCard
                  active={startForm.timedMode}
                  description="Train like a real assessment with a visible countdown and submission lock on expiry."
                  onClick={() => handleModeChange(true)}
                  title="Timed Assessment"
                />
              </div>
            </div>

            {startForm.timedMode ? (
              <label className="block space-y-2">
                <span className="text-sm font-medium text-slate-200">Assessment Length</span>
                <select
                  className="w-full rounded-2xl border border-white/10 bg-forge-800 px-4 py-3 text-slate-100 ember-ring"
                  name="durationMinutes"
                  value={startForm.durationMinutes}
                  onChange={handleStartFormChange}
                  disabled={isStartingSession}
                >
                  <option value={15}>15 minutes</option>
                  <option value={30}>30 minutes</option>
                  <option value={45}>45 minutes</option>
                </select>
              </label>
            ) : null}

            {!questions.length ? (
              <p className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-400">
                No coding questions are available yet. Add or seed questions in the backend to enable session creation.
              </p>
            ) : null}

            {startError ? (
              <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{startError}</p>
            ) : null}
            <div className="flex flex-wrap gap-3">
              <Button disabled={isStartingSession || !questions.length || !startForm.questionId} type="submit">
                {isStartingSession ? "Forging session..." : "Start Session"}
              </Button>
              <Button onClick={loadDashboard} type="button" variant="ghost">
                Refresh Dashboard
              </Button>
            </div>
          </form>
        </div>

        <div className="panel p-6 sm:p-7">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Session Flow</p>
          <div className="mt-5 space-y-4">
            <FlowStep
              number="01"
              title="Create a session"
              description="Launch a fresh practice round by choosing a question from the available challenge list."
            />
            <FlowStep
              number="02"
              title="Plan your strategy"
              description="Clarify assumptions and outline your approach before writing any code."
            />
            <FlowStep
              number="03"
              title="Write your solution"
              description="Use the editor on the session page to draft and refine your answer."
            />
            <FlowStep
              number="04"
              title="Submit for AI review"
              description="PrepForge stores the submission, scores it, and attaches actionable feedback."
            />
          </div>
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[0.9fr,1.1fr]">
        <div className="panel p-6 sm:p-7">
          <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Behavioral Progress</p>
          <h2 className="mt-3 text-2xl font-semibold text-white">Track your story-driven interview reps</h2>
          <p className="mt-3 text-sm leading-7 text-slate-400">
            Measure how your behavioral answers are improving across categories, and use the weakest area as your next practice target.
          </p>

          <div className="mt-6 grid gap-4 sm:grid-cols-3">
            <DashboardMetric
              label="Average Score"
              value={
                behavioralAnalytics?.averageScore !== null && behavioralAnalytics?.averageScore !== undefined
                  ? behavioralAnalytics.averageScore.toFixed(1)
                  : "N/A"
              }
            />
            <DashboardMetric
              label="Attempts"
              value={String(behavioralAnalytics?.attemptsCount ?? 0)}
            />
            <DashboardMetric
              label="Weakest Category"
              value={behavioralAnalytics?.weakestCategory || "Not enough data"}
            />
          </div>

          <div className="mt-5 rounded-2xl border border-white/10 bg-white/5 p-4">
            <p className="text-xs uppercase tracking-[0.25em] text-slate-500">Next Suggestion</p>
            <p className="mt-3 text-sm leading-7 text-slate-300">
              {behavioralAnalytics?.weakestCategory
                ? `Practice more ${behavioralAnalytics.weakestCategory} questions to strengthen your weakest interview pattern.`
                : "Complete a few behavioral submissions to unlock a personalized practice recommendation."}
            </p>
          </div>
        </div>

        <div className="panel p-6 sm:p-7">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Behavioral Categories</p>
          <h2 className="mt-3 text-2xl font-semibold text-white">Where your practice time is going</h2>

          {behavioralAnalytics?.categoryBreakdown && Object.keys(behavioralAnalytics.categoryBreakdown).length ? (
            <div className="mt-6 grid gap-4 sm:grid-cols-2">
              {Object.entries(behavioralAnalytics.categoryBreakdown).map(([category, count]) => (
                <div key={category} className="rounded-2xl border border-white/10 bg-black/20 px-4 py-4">
                  <p className="text-xs uppercase tracking-[0.25em] text-slate-500">{category}</p>
                  <p className="mt-3 text-2xl font-semibold text-white">{count}</p>
                  <p className="mt-2 text-sm text-slate-400">Recorded attempt{count === 1 ? "" : "s"}</p>
                </div>
              ))}
            </div>
          ) : (
            <div className="mt-6 rounded-2xl border border-white/10 bg-white/5 px-4 py-6 text-sm text-slate-400">
              No behavioral attempts yet. Start a behavioral session to begin tracking progress by category.
            </div>
          )}
        </div>
      </section>

      <StatsOverview analytics={analytics} />
      <RecentSessionsList sessions={sessions} />
    </div>
  );
}

function FlowStep({ number, title, description }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
      <p className="text-xs font-semibold tracking-[0.3em] text-ember-300">{number}</p>
      <h3 className="mt-2 text-lg font-semibold text-white">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-400">{description}</p>
    </div>
  );
}

function ModeCard({ active, description, onClick, title }) {
  return (
    <button
      className={`rounded-2xl border p-4 text-left transition ${
        active
          ? "border-ember-500/30 bg-ember-500/10"
          : "border-white/10 bg-white/5 hover:bg-white/10"
      }`}
      onClick={onClick}
      type="button"
    >
      <p className={`text-sm font-semibold ${active ? "text-ember-300" : "text-white"}`}>{title}</p>
      <p className="mt-2 text-sm leading-6 text-slate-400">{description}</p>
    </button>
  );
}

function DashboardMetric({ label, value }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-4">
      <p className="text-xs uppercase tracking-[0.25em] text-slate-500">{label}</p>
      <p className="mt-3 text-2xl font-semibold text-white">{value}</p>
    </div>
  );
}

function InsightCard({ label, value }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-black/20 p-4">
      <p className="text-xs uppercase tracking-[0.25em] text-slate-500">{label}</p>
      <p className="mt-3 text-sm leading-7 text-slate-300">{value || "Not enough signal yet."}</p>
    </div>
  );
}

export default DashboardPage;
