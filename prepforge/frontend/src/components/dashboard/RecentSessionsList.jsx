import { Link } from "react-router-dom";
import StatusBadge from "../ui/StatusBadge";
import EmptyState from "../ui/EmptyState";
import { formatDateTime } from "../../utils/formatters";

function RecentSessionsList({ sessions }) {
  if (!sessions?.length) {
    return (
      <EmptyState
        title="No sessions forged yet"
        description="Once you start coding sessions, they will show up here with quick access to details and submissions."
      />
    );
  }

  return (
    <div className="panel overflow-hidden">
      <div className="border-b border-white/10 px-6 py-5">
        <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Recent Sessions</p>
        <h3 className="mt-2 text-xl font-semibold text-white">Latest coding reps</h3>
      </div>

      <div className="divide-y divide-white/5">
        {sessions.map((session) => (
          <Link
            key={session.sessionId}
            to={`/sessions/${session.sessionId}`}
            className="flex flex-col gap-4 px-6 py-5 transition hover:bg-white/5 md:flex-row md:items-center md:justify-between"
          >
            <div>
              <p className="text-lg font-semibold text-white">Session #{session.sessionId.slice(0, 8)}</p>
              <p className="mt-1 text-sm text-slate-400">
                Question ID: <span className="font-mono text-slate-300">{session.questionId}</span>
              </p>
              <p className="mt-2 text-sm text-slate-500">{formatDateTime(session.startedAt)}</p>
            </div>
            <StatusBadge value={session.status} />
          </Link>
        ))}
      </div>
    </div>
  );
}

export default RecentSessionsList;
