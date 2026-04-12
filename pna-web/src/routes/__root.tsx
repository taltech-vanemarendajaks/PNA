import { createRootRoute, Link, Outlet, useNavigate } from "@tanstack/react-router";
import { useSyncExternalStore } from "react";
import { clearSession, hasStoredSession, subscribeToSessionChanges } from "../auth/session";
import { MobileAuthDock } from "../components/MobileAuthDock";
import { ThemeController } from "../components/ThemeController";

export const Route = createRootRoute({
  component: RootLayout,
});

function RootLayout() {
  const navigate = useNavigate();
  const isAuthenticated = useSyncExternalStore(
    subscribeToSessionChanges,
    hasStoredSession,
    () => false,
  );

  async function logout() {
    clearSession();
    await navigate({ to: "/" });
  }

  return (
    <div className="min-h-screen text-base-content">
      <div className="mx-auto flex min-h-screen w-full max-w-6xl flex-col px-4 py-4 sm:px-6 lg:px-8">
        <header className="navbar rounded-box border border-base-300/80 bg-base-100/90 px-4 shadow-sm backdrop-blur">
          <div className="flex-1">
            <Link to="/" className="text-lg font-semibold tracking-[0.18em] text-primary">
              PNA WEB
            </Link>
          </div>
          <nav className="flex gap-2">
            <Link to="/" className="btn btn-ghost btn-sm">
              Home
            </Link>
            <ThemeController />
          </nav>
        </header>

        <main
          className={
            isAuthenticated ? "flex-1 pt-6 pb-24 sm:pt-10 md:pb-10" : "flex-1 py-6 sm:py-10"
          }
        >
          <Outlet />
        </main>
      </div>

      {isAuthenticated ? <MobileAuthDock onLogout={() => void logout()} /> : null}
    </div>
  );
}
