import { afterEach, describe, expect, it, vi } from "vitest";
import {
  ApiResponseError,
  executeApiActionWithResponse,
  executeApiQuery,
  hasApiResponseStatus,
  isAuthenticationError,
} from "./command";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("API response errors", () => {
  it("preserves HTTP status for failed requests", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        async () =>
          new Response(JSON.stringify({ error: "Session expired" }), {
            status: 401,
            headers: { "Content-Type": "application/json" },
          }),
      ),
    );

    const error = await executeApiQuery({ path: "/api/v1/auth/session" }).catch(
      (caughtError) => caughtError,
    );

    expect(error).toBeInstanceOf(Error);

    if (!(error instanceof Error)) {
      throw new Error("Expected executeApiQuery to reject with an Error");
    }

    expect(error.message).toBe("Session expired");
    expect(hasApiResponseStatus(error, 401)).toBe(true);
    expect(hasApiResponseStatus(error, [401, 403])).toBe(true);
    expect(isAuthenticationError(error)).toBe(true);
  });

  it("does not treat non-auth failures as authentication errors", async () => {
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

    const error = await executeApiQuery({ path: "/api/v1/auth/session" }).catch(
      (caughtError) => caughtError,
    );

    expect(hasApiResponseStatus(error, 503)).toBe(true);
    expect(isAuthenticationError(error)).toBe(false);
  });

  it("runs preflight before sending the request", async () => {
    const events: string[] = [];

    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        events.push("fetch");

        return new Response(JSON.stringify({ message: "ok" }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }),
    );

    await executeApiActionWithResponse<{ message: string }>({
      path: "/api/v1/number/search",
      preflight: async () => {
        events.push("preflight");
      },
    });

    expect(events).toEqual(["preflight", "fetch"]);
  });

  it("does not send the request when preflight rejects", async () => {
    const fetchSpy = vi.fn();
    vi.stubGlobal("fetch", fetchSpy);

    const error = await executeApiActionWithResponse<{ message: string }>({
      path: "/api/v1/number/search",
      preflight: async () => {
        throw new ApiResponseError(401, { error: "Not authenticated" });
      },
    }).catch((caughtError) => caughtError);

    expect(hasApiResponseStatus(error, 401)).toBe(true);
    expect(fetchSpy).not.toHaveBeenCalled();
  });
});
