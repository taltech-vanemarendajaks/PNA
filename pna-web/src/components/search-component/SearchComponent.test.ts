import { describe, expect, it, vi } from "vitest";
import { ApiResponseError } from "../../api/command";
import { getSearchErrorMessage } from "./SearchComponent";

describe("getSearchErrorMessage", () => {
  it("redirects away from the authenticated flow when the backend session expires", () => {
    const onUnauthenticated = vi.fn();
    const error = new ApiResponseError(401, { error: "Session expired" });

    expect(getSearchErrorMessage(error, onUnauthenticated)).toBeNull();
    expect(onUnauthenticated).toHaveBeenCalledTimes(1);
  });

  it("keeps non-auth search failures as inline errors", () => {
    const onUnauthenticated = vi.fn();
    const error = new ApiResponseError(503, { error: "Search backend unavailable" });

    expect(getSearchErrorMessage(error, onUnauthenticated)).toBe("Search backend unavailable");
    expect(onUnauthenticated).not.toHaveBeenCalled();
  });
});
