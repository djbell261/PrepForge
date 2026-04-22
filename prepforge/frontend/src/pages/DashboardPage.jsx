import { useEffect, useState } from "react";
import { codingService } from "../services/codingService";
import StatsOverview from "../components/dashboard/StatsOverview";
import RecentSessionsList from "../components/dashboard/RecentSessionsList";
import EmptyState from "../components/ui/EmptyState";
import LoadingState from "../components/ui/LoadingState";
import { useAuth } from "../hooks/useAuth";

function DashboardPage() {
  const { user } = useAuth();
  const [analytics, setAnalytics] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadDashboard = async () => {
      setIsLoading(true);
      setError("");

      try {
        const [analyticsResponse, sessionsResponse] = await Promise.all([
          codingService.getAnalytics(),
          codingService.getSessions(),
        ]);

        setAnalytics(analyticsResponse);
        setSessions(sessionsResponse);
      } catch (requestError) {
        setError("We couldn't load your dashboard right now. Please try again.");
      } finally {
        setIsLoading(false);
      }
    };

    loadDashboard();
  }, []);

  if (isLoading) {
    return <LoadingState label="Heating up your dashboard..." />;
  }

  if (error) {
    return <EmptyState title="Dashboard unavailable" description={error} />;
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

      <StatsOverview analytics={analytics} />
      <RecentSessionsList sessions={sessions} />
    </div>
  );
}

export default DashboardPage;
