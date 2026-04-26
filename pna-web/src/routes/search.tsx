import { createFileRoute, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { getSession, logout } from "../api/auth/auth";
import { isAuthenticationError } from "../api/command";
import { SearchComponent } from "../components/search-component/SearchComponent";

export async function requireAuthenticatedSession() {
  const session = await getSession();

  if (!session) {
    throw redirect({ to: "/" });
  }

  return session;
}

export const Route = createFileRoute("/search")({
  beforeLoad: async () => requireAuthenticatedSession(),
  component: SearchRoute,
});

export function getLogoutErrorMessage(
  error: unknown,
  onUnauthenticated: () => void,
): string | null {
  if (isAuthenticationError(error)) {
    onUnauthenticated();
    return null;
  }

  return error instanceof Error ? error.message : "Logout failed";
}

function SearchRoute() {
  const [logoutError, setLogoutError] = useState<string | null>(null);

  function leaveAuthenticatedFlow() {
    window.location.assign("/");
  }

  async function handleLogout() {
    setLogoutError(null);

    try {
      await logout();
      leaveAuthenticatedFlow();
    } catch (error: unknown) {
      const logoutErrorMessage = getLogoutErrorMessage(error, leaveAuthenticatedFlow);

      if (!logoutErrorMessage) {
        return;
      }

      setLogoutError(logoutErrorMessage);
    }
  }

  return (
    <div className="mx-auto w-full max-w-3xl">
      <div className="mb-4 hidden justify-end md:flex">
        <button
          type="button"
          className="btn btn-outline btn-sm"
          onClick={() => void handleLogout()}
        >
          Logout
        </button>
      </div>

      {logoutError ? (
        <div role="alert" className="alert alert-error mb-4">
          <span>{logoutError}</span>
        </div>
      ) : null}

      <SearchComponent onUnauthenticated={leaveAuthenticatedFlow} />
    </div>
  );
}
