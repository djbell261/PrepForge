import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import Button from "../components/ui/Button";
import Input from "../components/ui/Input";

function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (event) => {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    try {
      await login(form);
      navigate("/dashboard");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Login failed. Check your credentials and try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthShell
      title="Return to the forge"
      subtitle="Sign in to track your coding momentum, review AI feedback, and open recent sessions."
      footer={
        <p className="text-sm text-slate-400">
          New here?{" "}
          <Link className="font-semibold text-ember-400 hover:text-ember-300" to="/register">
            Create an account
          </Link>
        </p>
      }
    >
      <form className="space-y-5" onSubmit={handleSubmit}>
        <Input label="Email" name="email" type="email" value={form.email} onChange={handleChange} required />
        <Input
          label="Password"
          name="password"
          type="password"
          value={form.password}
          onChange={handleChange}
          required
        />
        {error ? <p className="rounded-2xl bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p> : null}
        <Button className="w-full" disabled={isSubmitting} type="submit">
          {isSubmitting ? "Forging access..." : "Login"}
        </Button>
      </form>
    </AuthShell>
  );
}

export function AuthShell({ title, subtitle, children, footer }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-[1.1fr,0.9fr]">
      <section className="relative hidden overflow-hidden border-r border-white/10 bg-forge-950 lg:block">
        <div className="absolute inset-0 bg-forge-radial" />
        <div className="relative flex h-full flex-col justify-between p-12">
          <div>
            <p className="text-xs uppercase tracking-[0.4em] text-ember-300">PrepForge</p>
            <h1 className="mt-6 max-w-lg text-5xl font-bold leading-tight text-white">
              Sharpen interview performance with every rep.
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-8 text-slate-300">
              Track coding sessions, review AI feedback, and build a reliable prep rhythm inside one focused workspace.
            </p>
          </div>

          <div className="grid gap-4">
            <div className="panel p-5">
              <p className="text-sm text-slate-400">Dark, focused workspace</p>
              <p className="mt-2 text-2xl font-semibold text-white">Built for deliberate practice</p>
            </div>
            <div className="panel p-5">
              <p className="text-sm text-slate-400">Feedback loop</p>
              <p className="mt-2 text-2xl font-semibold text-white">Code, score, refine, repeat</p>
            </div>
          </div>
        </div>
      </section>

      <section className="flex items-center justify-center px-6 py-10 sm:px-8">
        <div className="w-full max-w-md panel p-8 shadow-ember">
          <p className="text-xs uppercase tracking-[0.3em] text-ember-300">Forge Access</p>
          <h2 className="mt-3 text-3xl font-bold text-white">{title}</h2>
          <p className="mt-3 leading-7 text-slate-400">{subtitle}</p>
          <div className="mt-8">{children}</div>
          <div className="mt-6">{footer}</div>
        </div>
      </section>
    </div>
  );
}

export default LoginPage;
