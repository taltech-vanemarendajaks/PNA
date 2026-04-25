import { Link } from "@tanstack/react-router";
import { handleLogout } from "../../api/auth/auth";
import { MobileAuthDock } from "./MobileAuthDock";

export function AppNavigation({ isAuthenticatedRoute }: { isAuthenticatedRoute: boolean }) {
  return (
    <>
      <div className="pt-4">
        <header className="navbar hidden md:flex rounded-box border border-base-300/80 bg-base-100/90 px-4 shadow-sm backdrop-blur">
          <div className="flex-1">
            <Link to="/" className="text-lg font-semibold tracking-[0.18em] text-primary">
              PNA WEB
            </Link>
          </div>
          <nav className="flex gap-2">
            {isAuthenticatedRoute && (
              <div className="flex gap-2">
                <Link to="/search" className="btn btn-ghost btn-sm">
                  Search
                </Link>
                <Link to="/settings" className="btn btn-ghost btn-sm">
                  Settings
                </Link>
                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={() => void handleLogout()}
                >
                  Logout
                </button>
              </div>
            )}
          </nav>
        </header>
      </div>
      {isAuthenticatedRoute ? <MobileAuthDock /> : null}
    </>
  );
}
