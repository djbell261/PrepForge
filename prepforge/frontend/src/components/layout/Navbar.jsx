import { Link } from "react-router-dom";
import Button from "../ui/Button";

function Navbar({ user, onLogout }) {
  return (
    <header className="sticky top-0 z-20 border-b border-white/10 bg-forge-950/80 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <Link to="/dashboard" className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-ember-400 to-ember-600 text-lg font-bold text-white shadow-ember">
            PF
          </div>
          <div>
            <p className="text-xs uppercase tracking-[0.3em] text-ember-300">PrepForge</p>
            <p className="text-sm text-slate-300">Sharpen every interview rep</p>
          </div>
        </Link>

        <div className="flex items-center gap-3">
          <div className="hidden rounded-2xl border border-white/10 bg-white/5 px-4 py-2 sm:block">
            <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Signed In</p>
            <p className="text-sm font-medium text-slate-200">{user?.name || "PrepForge User"}</p>
          </div>
          <Button variant="ghost" onClick={onLogout}>
            Logout
          </Button>
        </div>
      </div>
    </header>
  );
}

export default Navbar;
