import { afterEach, describe, expect, it, vi } from "vitest";
import { hasApiResponseStatus } from "../command";
import { getSession, requireAuthenticatedSession } from "./auth";

afterEach(() => {
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

  it("allows authenticated preflight to continue", async () => {
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
});
