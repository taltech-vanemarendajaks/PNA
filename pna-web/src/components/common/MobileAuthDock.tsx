import {
  ArrowRightStartOnRectangleIcon,
  Cog6ToothIcon,
  MagnifyingGlassIcon,
} from "@heroicons/react/24/outline";
import { Link } from "@tanstack/react-router";
import { handleLogout } from "../../api/auth/auth";

export function MobileAuthDock() {
  return (
    <nav aria-label="Authenticated navigation" className="dock dock-sm md:hidden">
      <Link to="/search" aria-label="Search" activeProps={{ className: "dock-active" }}>
        <MagnifyingGlassIcon className="size-7" />
      </Link>

      <Link to="/settings" aria-label="settings" activeProps={{ className: "dock-active" }}>
        <Cog6ToothIcon className="size-7" />
      </Link>

      <button type="button" aria-label="Logout" onClick={() => void handleLogout()}>
        <ArrowRightStartOnRectangleIcon className="size-7" />
      </button>
    </nav>
  );
}
