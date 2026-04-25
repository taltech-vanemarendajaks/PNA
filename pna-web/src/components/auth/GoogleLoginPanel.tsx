import { useEffect, useState } from "react";
import {
  DEFAULT_AUTHENTICATED_PATH,
  getSession,
  startGoogleLoginWithRedirect,
} from "../../api/auth/auth";
import { GoogleLoginButton } from "./GoogleLoginButton";

export const LOGIN_REQUEST_FAILURE_MESSAGE = "Something went wrong. Please try again later.";

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
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const authError = consumeAuthErrorFromQuery();

    if (authError) {
      setError(authError);
    }
  }, []);

  useEffect(() => {
    let isActive = true;

    void getSession()
      .then((session) => {
        if (!isActive || !session) {
          return;
        }

        window.location.assign(DEFAULT_AUTHENTICATED_PATH);
      })
      .catch(() => {});

    return () => {
      isActive = false;
    };
  }, []);

  async function prepareGoogleRedirectLogin() {
    setError(null);

    try {
      await startGoogleLoginWithRedirect(window.location, (url) => {
        window.location.assign(url);
      });
    } catch {
      setError(LOGIN_REQUEST_FAILURE_MESSAGE);
    }
  }

  return (
    <div className="mx-auto flex w-full items-center justify-center">
      <section className="w-full rounded-4xl border border-base-300 bg-base-100 p-8 shadow-xl shadow-primary/5">
        <span className="badge badge-primary mb-4">Authentication</span>
        <h1 className="text-4xl font-semibold sm:text-5xl">Login with Google</h1>

        <div className="mt-8 space-y-4">
          <div className="flex justify-center">
            <GoogleLoginButton onClick={() => void prepareGoogleRedirectLogin()} />
          </div>

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
