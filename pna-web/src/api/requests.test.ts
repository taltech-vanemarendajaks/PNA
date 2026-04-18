import { afterEach, describe, expect, it, vi } from "vitest";
import { searchNumber } from "./requests";

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
});

describe("searchNumber", () => {
  it("posts the search request with cookie auth and returns a result", async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-03-18T09:42:00Z"));

    const fetchSpy = vi.fn(async () => {
      return new Response(
        JSON.stringify({
          result: "result",
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      );
    });

    vi.stubGlobal("fetch", fetchSpy);

    const result = await searchNumber("+37255551234");

    expect(result).toBeTruthy();
    expect(fetchSpy).toHaveBeenCalledWith("http://localhost:8080/api/v1/number/search", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ number: "+37255551234" }),
      credentials: "include",
    });
  });

  it("surfaces backend search errors", async () => {
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
