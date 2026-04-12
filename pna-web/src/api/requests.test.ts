import { afterEach, describe, expect, it, vi } from "vitest";

const { requireAuthenticatedSessionSpy } = vi.hoisted(() => ({
  requireAuthenticatedSessionSpy: vi.fn(),
}));

vi.mock("./auth", () => ({
  requireAuthenticatedSession: requireAuthenticatedSessionSpy,
}));

import { ApiResponseError, hasApiResponseStatus } from "./command";
import { searchNumber } from "./requests";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("searchNumber", () => {
  it("posts the search request with cookie-based auth", async () => {
    requireAuthenticatedSessionSpy.mockResolvedValue(undefined);

    const fetchSpy = vi.fn(async () => {
      return new Response(JSON.stringify({ message: "Match found" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });

    vi.stubGlobal("fetch", fetchSpy);

    await expect(searchNumber("+37255551234")).resolves.toEqual({ message: "Match found" });
    expect(fetchSpy).toHaveBeenCalledWith("http://localhost:8080/api/v1/number/search", {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ number: "+37255551234" }),
    });
  });

  it("surfaces backend search errors", async () => {
    requireAuthenticatedSessionSpy.mockResolvedValue(undefined);

    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        return new Response(JSON.stringify({ error: "Not authenticated" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        });
      }),
    );

    await expect(searchNumber("+37255551234")).rejects.toThrow("Not authenticated");
  });
});
