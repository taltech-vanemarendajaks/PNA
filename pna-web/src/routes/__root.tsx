import { createRootRoute, Link, Outlet, useLocation } from "@tanstack/react-router";
import { logout } from "../api/auth/auth";
import { isAuthenticationError } from "../api/command";
import { MobileAuthDock } from "../components/MobileAuthDock";
import { ThemeController } from "../components/ThemeController";
import { NotFoundRoute } from "../components/common/NotFound";

export const Route = createRootRoute({
  component: RootLayout,
  notFoundComponent: NotFoundRoute,
});

function RootLayout() {
  const location = useLocation();
  const isAuthenticatedRoute = location.pathname === "/search" || location.pathname === "/settings";

  function leaveAuthenticatedFlow() {
    window.location.assign("/");
  }

  async function handleLogout() {
    try {
      await logout();
      leaveAuthenticatedFlow();
    } catch (error: unknown) {
      if (isAuthenticationError(error)) {
        leaveAuthenticatedFlow();
        return;
      }
    }
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
            isAuthenticatedRoute ? "flex-1 pt-6 pb-24 sm:pt-10 md:pb-10" : "flex-1 py-6 sm:py-10"
          }
        >
          <Outlet />
        </main>
      </div>

      {isAuthenticatedRoute ? <MobileAuthDock onLogout={() => void handleLogout()} /> : null}
    </div>
  );
}
