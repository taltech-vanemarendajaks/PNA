import { requireAuthenticatedSession } from "./auth/auth";
import { executeApiActionWithResponse } from "./command";

type SearchNumberRequest = {
  number: string;
};

export type SearchNumberResponse = {
  message: string;
};

export async function searchNumber(number: string): Promise<SearchNumberResponse> {
  return executeApiActionWithResponse<SearchNumberResponse, SearchNumberRequest>({
    path: "/api/v1/number/search",
    body: { number },
    preflight: requireAuthenticatedSession,
  });
}
