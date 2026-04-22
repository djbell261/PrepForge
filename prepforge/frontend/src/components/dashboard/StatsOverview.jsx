import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { formatScore } from "../../utils/formatters";

function StatsCard({ label, value, detail }) {
  return (
    <div className="panel p-5 shadow-ember">
      <p className="text-xs uppercase tracking-[0.3em] text-slate-500">{label}</p>
      <p className="mt-3 text-3xl font-bold text-white">{value}</p>
      <p className="mt-2 text-sm text-slate-400">{detail}</p>
    </div>
  );
}

function StatsOverview({ analytics }) {
  const averageScore = analytics?.averageScore ?? 0;
  const latestScore = analytics?.latestSubmissionScore ?? 0;
  const totalSessions = analytics?.totalSessionsCount ?? 0;

  const chartData = [
    { name: "Average", value: averageScore },
    { name: "Latest", value: latestScore },
    { name: "Sessions", value: totalSessions },
  ];

  return (
    <section className="grid gap-6 xl:grid-cols-[1.4fr,0.9fr]">
      <div className="grid gap-4 md:grid-cols-3">
        <StatsCard
          label="Average Score"
          value={formatScore(analytics?.averageScore)}
          detail="Your current coding quality trend."
        />
        <StatsCard
          label="Latest Score"
          value={formatScore(analytics?.latestSubmissionScore)}
          detail="Most recent AI-evaluated submission."
        />
        <StatsCard
          label="Sessions"
          value={totalSessions}
          detail="Total coding sessions started so far."
        />
      </div>

      <div className="panel p-5">
        <div className="mb-4">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Performance Snapshot</p>
          <h3 className="mt-2 text-lg font-semibold text-white">Forge Heat</h3>
        </div>

        <div className="h-64">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" />
              <XAxis dataKey="name" stroke="#94a3b8" />
              <YAxis stroke="#94a3b8" />
              <Tooltip
                cursor={{ fill: "rgba(255,255,255,0.04)" }}
                contentStyle={{
                  background: "#171717",
                  border: "1px solid rgba(255,255,255,0.1)",
                  borderRadius: "16px",
                }}
              />
              <Bar dataKey="value" fill="#f97316" radius={[10, 10, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </section>
  );
}

export default StatsOverview;
