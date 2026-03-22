import { getStoredIdToken } from "../auth/session";
import type { SessionCommandResult } from "../auth/sessionCommand";
import { executeSessionApiCommand } from "./command";

type SearchNumberRequest = {
  number: string;
};

export type SearchNumberResponse = {
  message: string;
};

export function searchNumber(number: string): Promise<SessionCommandResult<SearchNumberResponse>> {
  const idToken = getStoredIdToken();

  if (!idToken) {
    return Promise.resolve({
      status: "error",
      message: "Please log in again to search by phone number.",
    });
  }

  return executeSessionApiCommand<SearchNumberResponse, SearchNumberRequest>({
    path: "/api/v1/number/search",
    body: { number },
    bearerToken: idToken,
    fallbackMessage: "Search failed.",
  });
}
