import { useEffect, useState } from "react";
import { getSession, logout as logoutSession, startGoogleLoginWithRedirect } from "../api/auth";
import type { GoogleAuthResponse } from "../lib/googleAuth";
import { GoogleLoginButton } from "./GoogleLoginButton";

function consumeAuthErrorFromQuery(): string | null {
  const query = new URLSearchParams(window.location.search);
  const authError = query.get("authError");

  if (!authError) {
    return null;
  }

  query.delete("authError");
  const nextSearch = query.toString();
  const nextUrl = `${window.location.pathname}${nextSearch ? `?${nextSearch}` : ""}`;
  window.history.replaceState({}, document.title, nextUrl);
  return authError;
}

export function GoogleLoginPanel() {
  const [isLoadingSession, setIsLoadingSession] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [user, setUser] = useState<GoogleAuthResponse | null>(null);

  useEffect(() => {
    async function loadSession() {
      const authError = consumeAuthErrorFromQuery();

      if (authError) {
        setError(authError);
      }

      try {
        const session = await getSession();
        setUser(session);
      } catch (sessionError: unknown) {
        setError(sessionError instanceof Error ? sessionError.message : "Failed to load session");
      } finally {
        setIsLoadingSession(false);
      }
    }

    void loadSession();
  }, []);

  async function handleLogout() {
    setError(null);
    try {
      await logoutSession();
      setUser(null);
    } catch (logoutError: unknown) {
      setError(logoutError instanceof Error ? logoutError.message : "Logout failed");
    }
  }

  function prepareGoogleRedirectLogin() {
    setError(null);
    setIsLoadingSession(true);
    startGoogleLoginWithRedirect(
      window.location,
      (url) => {
        window.location.assign(url);
      },
    );
  }

  return (
    <div className="mx-auto flex min-h-[60vh] w-full items-center justify-center">
      <section className="w-full max-w-2xl rounded-4xl border border-base-300 bg-base-100 p-8 shadow-xl shadow-primary/5">
        <span className="badge badge-primary mb-4">Authentication</span>
        <h1 className="text-4xl font-semibold sm:text-5xl">Login with Google</h1>

        <div className="mt-8 space-y-4">
          {!user && !isLoadingSession ? (
            <div className="flex justify-center">
              <GoogleLoginButton onClick={prepareGoogleRedirectLogin} />
            </div>
          ) : null}

          {user ? (
            <article className="rounded-2xl border border-success/30 bg-success/10 p-5">
              <h2 className="text-xl font-semibold text-success-content">Logged in</h2>
              <p className="mt-2 text-sm text-base-content/80">
                Signed in as <strong>{user.email ?? user.name ?? user.subject}</strong>
              </p>
              <p className="mt-2 break-all text-xs text-base-content/70">Subject: {user.subject}</p>
              <button
                type="button"
                className="btn btn-outline btn-sm mt-4"
                onClick={() => void handleLogout()}
              >
                Logout
              </button>
            </article>
          ) : null}

          {error ? (
            <div className="alert alert-error">
              <span>{error}</span>
            </div>
          ) : null}
        </div>
      </section>
    </div>
  );
}
