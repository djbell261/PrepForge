import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import SubmissionHistory from "../components/coding/SubmissionHistory";
import EmptyState from "../components/ui/EmptyState";
import LoadingState from "../components/ui/LoadingState";
import StatusBadge from "../components/ui/StatusBadge";
import { codingService } from "../services/codingService";
import { formatDateTime } from "../utils/formatters";

function SessionDetailPage() {
  const { sessionId } = useParams();
  const [detail, setDetail] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadDetail = async () => {
      setIsLoading(true);
      setError("");

      try {
        const response = await codingService.getSessionDetail(sessionId);
        setDetail(response);
      } catch (requestError) {
        setError("We couldn't load this coding session. It may not exist or you may not have access.");
      } finally {
        setIsLoading(false);
      }
    };

    loadDetail();
  }, [sessionId]);

  if (isLoading) {
    return <LoadingState label="Loading session detail..." />;
  }

  if (error) {
    return <EmptyState title="Session unavailable" description={error} />;
  }

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

export default SessionDetailPage;
