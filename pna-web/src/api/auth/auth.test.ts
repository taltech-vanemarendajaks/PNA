import { afterEach, describe, expect, it, vi } from "vitest";
import { hasApiResponseStatus } from "../command";
import {
  consumeAccessTokenFromRedirect,
  getSession,
  logout,
  requireAuthenticatedSession,
  startGoogleLoginWithRedirect,
} from "./auth";
import { clearStoredAccessToken, getStoredAccessToken, storeAccessToken } from "./tokenStorage";

afterEach(() => {
  vi.unstubAllGlobals();
  clearStoredAccessToken();
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

  it("clears a stored token when the backend rejects it", async () => {
    storeAccessToken("stale-token");

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
    expect(getStoredAccessToken()).toBeNull();
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

  it("consumes an access token from the redirect fragment and clears it from the url", () => {
    const replaceState = vi.fn();
    const values = new Map<string, string>();
    const localStorage = {
      getItem: vi.fn((key: string) => values.get(key) ?? null),
      setItem: vi.fn((key: string, value: string) => {
        values.set(key, value);
      }),
      removeItem: vi.fn((key: string) => {
        values.delete(key);
      }),
      clear: vi.fn(() => {
        values.clear();
      }),
      key: vi.fn(() => null),
      length: 0,
    };
    vi.stubGlobal("window", {
      history: { replaceState },
      localStorage,
    });
    vi.stubGlobal("document", { title: "PNA" });

    expect(
      consumeAccessTokenFromRedirect({
        pathname: "/search",
        search: "",
        hash: "#accessToken=fresh-token",
      } as Pick<Location, "hash" | "search" | "pathname">),
    ).toBe("fresh-token");
    expect(getStoredAccessToken()).toBe("fresh-token");
    expect(replaceState).toHaveBeenCalledWith({}, "PNA", "/search");
  });

  it("clears the stored token during logout", async () => {
    storeAccessToken("jwt-token");

    await logout();

    expect(getStoredAccessToken()).toBeNull();
  });
});
