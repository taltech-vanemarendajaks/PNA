import { describe, expect, it, vi } from "vitest";
import { redirectUnauthenticatedUser } from "./routeAuth";

const getSessionMock = vi.hoisted(() => vi.fn());

vi.mock("./auth", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./auth")>();

  return {
    ...actual,
    getSession: getSessionMock,
  };
});

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

  it("redirects to login when the session request fails", async () => {
    const error = new Error("Backend unavailable");
    getSessionMock.mockRejectedValue(error);

    await expect(redirectUnauthenticatedUser()).rejects.toMatchObject({
      options: {
        to: "/",
        statusCode: 307,
      },
    });
  });
});
