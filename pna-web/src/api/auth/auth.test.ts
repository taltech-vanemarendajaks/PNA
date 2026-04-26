import { afterEach, describe, expect, it, vi } from "vitest";
import { hasApiResponseStatus } from "../command";
import {
  buildGoogleRedirectLoginUriWithContext,
  getSession,
  logout,
  requireAuthenticatedSession,
  startGoogleLoginWithRedirect,
} from "./auth";

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("google redirect auth helpers", () => {
  it("returns null for an unauthenticated session", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify({ error: "Session cookie missing" }), {
            status: 401,
            headers: { "Content-Type": "application/json" },
          }),
      ),
    );

    await expect(getSession()).resolves.toBeNull();
  });

  it("returns session data when authenticated", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({
              subject: "subject-1",
              email: "user@example.com",
              name: "Jane Doe",
              givenName: "Jane",
            }),
            {
              status: 200,
              headers: { "Content-Type": "application/json" },
            },
          ),
      ),
    );

    await expect(getSession()).resolves.toEqual({
      subject: "subject-1",
      email: "user@example.com",
      name: "Jane Doe",
      givenName: "Jane",
    });
  });

  it("surfaces real session failures", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify({ error: "Backend unavailable" }), {
            status: 503,
            headers: { "Content-Type": "application/json" },
          }),
      ),
    );

    await expect(getSession()).rejects.toThrow("Backend unavailable");
  });

  it("allows authenticated preflight to continue", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({
              subject: "subject-1",
              email: "user@example.com",
              name: "Jane Doe",
              givenName: "Jane",
            }),
            {
              status: 200,
              headers: { "Content-Type": "application/json" },
            },
          ),
      ),
    );

    await expect(requireAuthenticatedSession()).resolves.toBeUndefined();
  });

  it("throws an auth-shaped error when authenticated preflight fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify({ error: "Session cookie missing" }), {
            status: 401,
            headers: { "Content-Type": "application/json" },
          }),
      ),
    );

    const error = await requireAuthenticatedSession().catch((caughtError) => caughtError);

    expect(hasApiResponseStatus(error, 401)).toBe(true);
  });

  it("preserves real failures during authenticated preflight", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify({ error: "Backend unavailable" }), {
            status: 503,
            headers: { "Content-Type": "application/json" },
          }),
      ),
    );

    await expect(requireAuthenticatedSession()).rejects.toThrow("Backend unavailable");
  });

  it("bootstraps a session through refresh when the auth cookie needs renewal", async () => {
    const fetchSpy = vi
      .fn<(_: string, __?: RequestInit) => Promise<Response>>()
      .mockImplementationOnce(async () => {
        return new Response(JSON.stringify({ error: "Not authenticated" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        });
      })
      .mockImplementationOnce(async () => {
        return new Response(null, { status: 204 });
      })
      .mockImplementationOnce(async () => {
        return new Response(JSON.stringify({ subject: "subject-1" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      });

    vi.stubGlobal("fetch", fetchSpy);

    await expect(getSession()).resolves.toEqual({ subject: "subject-1" });
  });

  it("refreshes the auth cookie when session checking gets an auth failure", async () => {
    const fetchSpy = vi
      .fn<(_: string, __?: RequestInit) => Promise<Response>>()
      .mockImplementationOnce(async () => {
        return new Response(JSON.stringify({ error: "expired" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        });
      })
      .mockImplementationOnce(async () => {
        return new Response(null, { status: 204 });
      })
      .mockImplementationOnce(async () => {
        return new Response(
          JSON.stringify({
            subject: "subject-1",
            email: "user@example.com",
            name: "Jane Doe",
            givenName: "Jane",
          }),
          {
            status: 200,
            headers: { "Content-Type": "application/json" },
          },
        );
      });

    vi.stubGlobal("fetch", fetchSpy);

    await expect(getSession()).resolves.toEqual({
      subject: "subject-1",
      email: "user@example.com",
      name: "Jane Doe",
      givenName: "Jane",
    });
  });

  it("skips the backend redirect when already authenticated", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify({ subject: "subject-1" }), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          }),
      ),
    );

    const navigate = vi.fn();

    await startGoogleLoginWithRedirect(
      {
        origin: "http://localhost:5173",
        pathname: "/",
        search: "",
        protocol: "http:",
      },
      navigate,
    );

    expect(navigate).toHaveBeenCalledWith("/search");
  });

  it("preserves the validated return path in the backend redirect url", async () => {
    const redirectUri = buildGoogleRedirectLoginUriWithContext(
      {
        origin: "http://localhost:5173",
        pathname: "/",
        search: "",
        protocol: "http:",
      },
      "http://localhost:8080",
      "/search?number=%2B37255551234",
    );

    expect(redirectUri).toBe(
      "http://localhost:8080/api/v1/auth/google/redirect?frontendOrigin=http%3A%2F%2Flocalhost%3A5173&returnPath=%2Fsearch%3Fnumber%3D%252B37255551234",
    );
  });

  it("logs out through the cookie-backed endpoint", async () => {
    const fetchSpy = vi.fn(
      async () =>
        new Response(null, {
          status: 204,
        }),
    );
    vi.stubGlobal("fetch", fetchSpy);

    await logout();

    expect(fetchSpy).toHaveBeenCalledWith("http://localhost:8080/api/v1/auth/logout", {
      method: "POST",
      headers: undefined,
      body: undefined,
      credentials: "include",
    });
  });
});
