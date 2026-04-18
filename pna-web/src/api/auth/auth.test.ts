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
  clearStoredAccessToken();
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

  it("bootstraps a session through refresh when no bearer token is stored", async () => {
    const fetchSpy = vi
      .fn<(_: string, __?: RequestInit) => Promise<Response>>()
      .mockImplementationOnce(async () => {
        return new Response(JSON.stringify({ error: "Not authenticated" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        });
      })
      .mockImplementationOnce(async () => {
        return new Response(JSON.stringify({ accessToken: "fresh-token" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      })
      .mockImplementationOnce(async () => {
        return new Response(JSON.stringify({ subject: "subject-1" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      });

    vi.stubGlobal("fetch", fetchSpy);

    await expect(getSession()).resolves.toEqual({ subject: "subject-1" });
    expect(getStoredAccessToken()).toBe("fresh-token");
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

  it("refreshes the access token when session checking gets an auth failure", async () => {
    storeAccessToken("expired-token");

    const fetchSpy = vi
      .fn<(_: string, __?: RequestInit) => Promise<Response>>()
      .mockImplementationOnce(async () => {
        return new Response(JSON.stringify({ error: "expired" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        });
      })
      .mockImplementationOnce(async () => {
        return new Response(JSON.stringify({ accessToken: "fresh-token" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
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
    expect(getStoredAccessToken()).toBe("fresh-token");
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

  it("clears the stored token after logout succeeds", async () => {
    storeAccessToken("jwt-token");

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
    expect(getStoredAccessToken()).toBeNull();
  });

  it("clears the stored token even when logout fails", async () => {
    storeAccessToken("jwt-token");

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

    await expect(logout()).rejects.toThrow("Backend unavailable");
    expect(getStoredAccessToken()).toBeNull();
  });
});
