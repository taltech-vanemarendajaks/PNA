import { describe, expect, it, vi } from "vitest";
import { redirectUnauthenticatedUser } from "./routeAuth";

const getSessionMock = vi.hoisted(() => vi.fn());

vi.mock("./auth", () => ({
  getSession: getSessionMock,
}));

describe("redirectUnauthenticatedUser", () => {
  it("allows authenticated users to continue", async () => {
    getSessionMock.mockResolvedValue({ subject: "subject-1" });

    await expect(redirectUnauthenticatedUser()).resolves.toBeUndefined();
  });

  it("redirects unauthenticated users to the login page", async () => {
    getSessionMock.mockResolvedValue(null);

    await expect(redirectUnauthenticatedUser()).rejects.toMatchObject({
      options: {
        to: "/",
        statusCode: 307,
      },
    });
  });

  it("rethrows non-authentication failures", async () => {
    const error = new Error("Backend unavailable");
    getSessionMock.mockRejectedValue(error);

    await expect(redirectUnauthenticatedUser()).rejects.toBe(error);
  });
});
