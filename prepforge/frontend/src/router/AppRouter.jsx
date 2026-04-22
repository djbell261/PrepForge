import { BrowserRouter, Navigate, Outlet, Route, Routes } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import AppLayout from "../components/layout/AppLayout";
import BehavioralPracticePage from "../pages/BehavioralPracticePage";
import BehavioralSessionPage from "../pages/BehavioralSessionPage";
import DashboardPage from "../pages/DashboardPage";
import LoginPage from "../pages/LoginPage";
import RegisterPage from "../pages/RegisterPage";
import SessionDetailPage from "../pages/SessionDetailPage";
import LoadingState from "../components/ui/LoadingState";

function AppRouter() {
  const { isAuthenticated, isBootstrapping } = useAuth();

  if (isBootstrapping) {
    return <LoadingState label="Preparing PrepForge..." />;
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            <GuestRoute isAuthenticated={isAuthenticated}>
              <LoginPage />
            </GuestRoute>
          }
        />
        <Route
          path="/register"
          element={
            <GuestRoute isAuthenticated={isAuthenticated}>
              <RegisterPage />
            </GuestRoute>
          }
        />

        <Route element={<ProtectedRoute isAuthenticated={isAuthenticated} />}>
          <Route element={<AppLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/behavioral" element={<BehavioralPracticePage />} />
          <Route path="/behavioral/sessions/:sessionId" element={<BehavioralSessionPage />} />
          <Route path="/sessions/:sessionId" element={<SessionDetailPage />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to={isAuthenticated ? "/dashboard" : "/login"} replace />} />
      </Routes>
    </BrowserRouter>
  );
}

function GuestRoute({ children, isAuthenticated }) {
  return isAuthenticated ? <Navigate to="/dashboard" replace /> : children;
}

function ProtectedRoute({ isAuthenticated }) {
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
}

export default AppRouter;
