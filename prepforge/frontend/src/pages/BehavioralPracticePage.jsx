import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import EmptyState from "../components/ui/EmptyState";
import LoadingState from "../components/ui/LoadingState";
import Button from "../components/ui/Button";
import { behavioralService } from "../services/behavioralService";
import { extractApiErrorMessage } from "../services/api";
import { useAuth } from "../hooks/useAuth";

function BehavioralPracticePage() {
  const navigate = useNavigate();
  const { token, isAuthenticated } = useAuth();
  const [questions, setQuestions] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [startError, setStartError] = useState("");
  const [startingQuestionId, setStartingQuestionId] = useState(null);
  const [startMode, setStartMode] = useState({
    isTimed: false,
    timeLimitSeconds: 600,
  });

  const loadQuestions = async () => {
    setIsLoading(true);
    setError("");

    try {
      const response = await behavioralService.getQuestions();
      setQuestions(response);
    } catch (requestError) {
      setError(
        extractApiErrorMessage(
          requestError,
          "We couldn't load behavioral practice questions right now. Please try again.",
        ),
      );
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!token || !isAuthenticated) {
      setIsLoading(false);
      return;
    }

    loadQuestions();
  }, [token, isAuthenticated]);

  const categories = useMemo(
    () => ["All", ...new Set(questions.map((question) => question.category).filter(Boolean))],
    [questions],
  );

  const filteredQuestions = questions.filter((question) =>
    selectedCategory === "All" ? true : question.category === selectedCategory,
  );

  const handleStartSession = async (questionId) => {
    setStartError("");
    setStartingQuestionId(questionId);

    try {
      const session = await behavioralService.startSession({
        questionId,
        isTimed: startMode.isTimed,
        timeLimitSeconds: startMode.isTimed ? Number(startMode.timeLimitSeconds) : null,
      });
      navigate(`/behavioral/sessions/${session.sessionId}`);
    } catch (requestError) {
      setStartError(
        extractApiErrorMessage(
          requestError,
          "We couldn't start this behavioral session. Please try again.",
        ),
      );
    } finally {
      setStartingQuestionId(null);
    }
  };

  if (isLoading) {
    return <LoadingState label="Gathering behavioral prompts..." />;
  }

  if (error) {
    return (
      <div className="space-y-4">
        <EmptyState title="Behavioral practice unavailable" description={error} />
        <div className="flex justify-center">
          <Button onClick={loadQuestions} variant="ghost">
            Retry question load
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <section className="panel overflow-hidden p-6 sm:p-8">
        <p className="text-xs uppercase tracking-[0.35em] text-ember-300">Behavioral Forge</p>
        <div className="mt-4 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 className="text-4xl font-bold text-white">Practice the stories interviewers remember.</h1>
            <p className="mt-3 max-w-3xl text-base leading-7 text-slate-400">
              Pick a behavioral prompt, structure your answer with STAR, and get AI feedback on clarity,
              specificity, impact, and communication readiness.
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <InfoCard label="Question Bank" value={String(questions.length)} />
            <InfoCard label="Categories" value={String(Math.max(categories.length - 1, 0))} />
          </div>
        </div>
      </section>

      <section className="panel p-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Question Filter</p>
            <h2 className="mt-2 text-2xl font-semibold text-white">Choose your interview lens</h2>
          </div>

          <label className="block space-y-2 lg:min-w-[260px]">
            <span className="text-sm font-medium text-slate-200">Category</span>
            <select
              className="w-full rounded-2xl border border-white/10 bg-forge-800 px-4 py-3 text-slate-100 ember-ring"
              value={selectedCategory}
              onChange={(event) => setSelectedCategory(event.target.value)}
            >
              {categories.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="mt-6 grid gap-4 lg:grid-cols-[1fr,0.4fr]">
          <div className="space-y-2">
            <span className="text-sm font-medium text-slate-200">Interview Pressure Mode</span>
            <div className="grid gap-3 md:grid-cols-2">
              <ModeCard
                active={!startMode.isTimed}
                description="Answer with full breathing room and iterate without a countdown."
                onClick={() => setStartMode((current) => ({ ...current, isTimed: false }))}
                title="Practice Mode"
              />
              <ModeCard
                active={startMode.isTimed}
                description="Simulate a live interview with a countdown and submission lock when time runs out."
                onClick={() => setStartMode((current) => ({ ...current, isTimed: true }))}
                title="Pressure Mode"
              />
            </div>
          </div>

          {startMode.isTimed ? (
            <label className="block space-y-2">
              <span className="text-sm font-medium text-slate-200">Time Limit</span>
              <select
                className="w-full rounded-2xl border border-white/10 bg-forge-800 px-4 py-3 text-slate-100 ember-ring"
                value={startMode.timeLimitSeconds}
                onChange={(event) =>
                  setStartMode((current) => ({ ...current, timeLimitSeconds: Number(event.target.value) }))
                }
              >
                <option value={300}>5 minutes</option>
                <option value={600}>10 minutes</option>
                <option value={900}>15 minutes</option>
              </select>
            </label>
          ) : null}
        </div>

        {startError ? (
          <p className="mt-5 rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{startError}</p>
        ) : null}

        {filteredQuestions.length ? (
          <div className="mt-6 grid gap-5 lg:grid-cols-2">
            {filteredQuestions.map((question) => (
              <article key={question.id} className="rounded-3xl border border-white/10 bg-white/5 p-5">
                <div className="flex flex-wrap gap-2">
                  <Tag>{question.category}</Tag>
                  <Tag tone="ember">{question.difficulty}</Tag>
                </div>

                <h3 className="mt-4 text-xl font-semibold text-white">{question.questionText}</h3>
                <p className="mt-3 text-sm leading-7 text-slate-400">
                  Start a focused written practice session, answer with STAR, and review your submission history
                  as you tighten the story.
                </p>

                <div className="mt-5 flex">
                  <Button
                    disabled={startingQuestionId === question.id}
                    onClick={() => handleStartSession(question.id)}
                  >
                    {startingQuestionId === question.id ? "Starting session..." : "Start Behavioral Session"}
                  </Button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="mt-6">
            <EmptyState
              title="No questions in this category"
              description="Try a different category filter or add more behavioral questions in the backend."
            />
          </div>
        )}
      </section>
    </div>
  );
}

function InfoCard({ label, value }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 px-5 py-4">
      <p className="text-xs uppercase tracking-[0.25em] text-slate-500">{label}</p>
      <p className="mt-3 text-2xl font-semibold text-white">{value}</p>
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

export default BehavioralPracticePage;
