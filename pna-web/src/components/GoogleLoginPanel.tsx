import { GoogleLogin, GoogleOAuthProvider } from "@react-oauth/google";
import { useNavigate } from "@tanstack/react-router";
import { useRef, useState } from "react";
import { verifyLogin } from "../api/auth";
import {
  type AuthUser,
  clearSession,
  getStoredUser,
  hasStoredSession,
  saveSession,
} from "../auth/session";

export function GoogleLoginPanel() {
  const navigate = useNavigate();
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
  const resolvedClientId = googleClientId ?? null;
  const [error, setError] = useState<string | null>(null);
  const googleButtonHostRef = useRef<HTMLDivElement | null>(null);
  const [user, setUser] = useState<AuthUser | null>(() =>
    hasStoredSession() ? getStoredUser() : null,
  );

  const envReady = Boolean(resolvedClientId);

  async function verifyWithBackend(idToken: string) {
    setError(null);

    const result = await verifyLogin({ idToken });

    if (result.status === "success") {
      saveSession(result.data, idToken);
      setUser(result.data);
      await navigate({ to: "/search" });
      return;
    }

    if (result.status === "error") {
      clearSession();
      setUser(null);
      setError(result.message);
    }
  }

  function logout() {
    clearSession();
    setUser(null);
    setError(null);
  }

  function openGoogleLogin() {
    setError(null);

    const googleButton =
      googleButtonHostRef.current?.querySelector<HTMLDivElement>('div[role="button"]');

    if (!googleButton) {
      setError("Google button is not ready yet. Try again.");
      return;
    }

    googleButton.click();
  }

  return (
    <div className="mx-auto flex min-h-[60vh] w-full items-center justify-center">
      <section className="w-full max-w-2xl rounded-4xl border border-base-300 bg-base-100 p-8 shadow-xl shadow-primary/5">
        <span className="badge badge-primary mb-4">Authentication</span>
        <h1 className="text-4xl font-semibold sm:text-5xl">Login with Google</h1>

        <div className="mt-8 space-y-4">
          {!envReady ? (
            <div className="alert alert-warning">
              <span>
                Missing Google client ID. Set <code>VITE_GOOGLE_CLIENT_ID</code> in frontend env.
              </span>
            </div>
          ) : null}

          {envReady ? (
            <div className="relative">
              <button
                type="button"
                className="btn w-full bg-white text-black border-[#e5e5e5]"
                onClick={openGoogleLogin}
              >
                <svg
                  aria-label="Google logo"
                  width="16"
                  height="16"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 512 512"
                >
                  <g>
                    <path d="m0 0H512V512H0" fill="#fff"></path>
                    <path
                      fill="#34a853"
                      d="M153 292c30 82 118 95 171 60h62v48A192 192 0 0190 341"
                    ></path>
                    <path
                      fill="#4285f4"
                      d="m386 400a140 175 0 0053-179H260v74h102q-7 37-38 57"
                    ></path>
                    <path fill="#fbbc02" d="m90 341a208 200 0 010-171l63 49q-12 37 0 73"></path>
                    <path
                      fill="#ea4335"
                      d="m153 219c22-69 116-109 179-50l55-54c-78-75-230-72-297 55"
                    ></path>
                  </g>
                </svg>
                Login with Google
              </button>
              <div
                className="pointer-events-none absolute inset-0 opacity-0"
                ref={googleButtonHostRef}
              >
                <GoogleOAuthProvider clientId={resolvedClientId ?? ""}>
                  <GoogleLogin
                    useOneTap
                    onSuccess={(credentialResponse) => {
                      const token = credentialResponse.credential;
                      if (!token) {
                        setError("Google did not return an ID token");
                        return;
                      }

                      void verifyWithBackend(token);
                    }}
                    onError={() => {
                      setError(
                        `Google login failed. If you see origin errors, add ${window.location.origin} to Authorized JavaScript origins in your Google OAuth client.`,
                      );
                    }}
                  />
                </GoogleOAuthProvider>
              </div>
            </div>
          ) : null}

          {user ? (
            <article className="rounded-2xl border border-success/30 bg-success/10 p-5">
              <h2 className="text-xl font-semibold text-success-content">Logged in</h2>
              <p className="mt-2 text-sm text-base-content/80">
                Signed in as <strong>{user.email ?? user.name ?? user.subject}</strong>
              </p>
              <p className="mt-2 break-all text-xs text-base-content/70">Subject: {user.subject}</p>
              <button type="button" className="btn btn-outline btn-sm mt-4" onClick={logout}>
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
