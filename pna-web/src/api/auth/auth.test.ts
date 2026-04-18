import { afterEach, describe, expect, it, vi } from "vitest";
import { hasApiResponseStatus } from "../command";
import {
  getSession,
  logout,
  requireAuthenticatedSession,
  startGoogleLoginWithRedirect,
} from "./auth";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
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

  it("returns null when the backend rejects the current auth cookie", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify({ error: "Not authenticated" }), {
            status: 401,
            headers: { "Content-Type": "application/json" },
          }),
      ),
    );

    await expect(getSession()).resolves.toBeNull();
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

  it("calls the backend logout endpoint", async () => {
    const fetchSpy = vi.fn(async () => new Response(null, { status: 204 }));
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
