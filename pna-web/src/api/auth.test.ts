import { afterEach, describe, expect, it, vi } from "vitest";
import {
  buildGoogleRedirectLoginUri,
  buildGoogleRedirectLoginUriWithContext,
  buildGoogleRedirectReturnPath,
  getSession,
  startGoogleLoginWithRedirect,
} from "./auth";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("google redirect auth helpers", () => {
  it("builds the backend redirect login URI from the API base URL", () => {
    expect(buildGoogleRedirectLoginUri("http://localhost:8080")).toBe(
      "http://localhost:8080/api/v1/auth/google/redirect",
    );
  });

  it("preserves the current path and search as the return path", () => {
    expect(buildGoogleRedirectReturnPath({ pathname: "/numbers", search: "?q=123" })).toBe(
      "/numbers?q=123",
    );
  });

  it("creates a redirect login URI with frontend origin and return path", () => {
    expect(
      buildGoogleRedirectLoginUriWithContext({
        origin: "http://localhost:5173",
        pathname: "/numbers",
        search: "?q=123",
        protocol: "http:",
      }),
    ).toBe(
      "http://localhost:8080/api/v1/auth/google/redirect?frontendOrigin=http%3A%2F%2Flocalhost%3A5173&returnPath=%2Fnumbers%3Fq%3D123",
    );
  });

  it("starts same-window login with redirect context in the URL", () => {
    let navigatedTo = "";

    startGoogleLoginWithRedirect(
      {
        origin: "http://localhost:5173",
        pathname: "/numbers",
        search: "?q=123",
        protocol: "http:",
      },
      (url) => {
        navigatedTo = url;
      },
    );

    expect(navigatedTo).toBe(
      "http://localhost:8080/api/v1/auth/google/redirect?frontendOrigin=http%3A%2F%2Flocalhost%3A5173&returnPath=%2Fnumbers%3Fq%3D123",
    );
  });

  it("returns null for an unauthenticated session", async () => {
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

  it("returns session data when authenticated", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({
              subject: "subject-1",
              email: "user@example.com",
              emailVerified: true,
              name: "Jane Doe",
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
      emailVerified: true,
      name: "Jane Doe",
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
});
