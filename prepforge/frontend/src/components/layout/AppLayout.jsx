import { Outlet } from "react-router-dom";
import { useAuth } from "../../hooks/useAuth";
import Navbar from "./Navbar";

function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-forge-radial">
      <Navbar user={user} onLogout={logout} />
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <Outlet />
      </main>
    </div>
  );
}

export default AppLayout;
