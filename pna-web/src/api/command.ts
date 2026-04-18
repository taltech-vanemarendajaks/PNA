import type { ApiError } from "../lib/googleAuth";
import { LOGOUT_PATH, REFRESH_PATH } from "./auth/authPaths";
import {
  clearStoredAccessToken,
  getStoredAccessToken,
  storeAccessToken,
} from "./auth/tokenStorage";

export class ApiResponseError extends Error {
  status: number;

  constructor(status: number, payload: ApiError) {
    super(payload.error ?? "Request failed");
    this.name = "ApiResponseError";
    this.status = status;
  }
}

export function hasApiResponseStatus(error: unknown, expectedStatus: number | number[]): boolean {
  const statuses = Array.isArray(expectedStatus) ? expectedStatus : [expectedStatus];

  return error instanceof ApiResponseError && statuses.includes(error.status);
}

export function isAuthenticationError(error: unknown): boolean {
  return hasApiResponseStatus(error, [401, 403]);
}

type ApiRequestPreflight = () => Promise<void>;

type ExecuteApiQueryOptions = {
  path: string;
  preflight?: ApiRequestPreflight;
};

type ExecuteApiActionOptions<TRequest> = {
  path: string;
  body?: TRequest;
  preflight?: ApiRequestPreflight;
};

type RequestOptions<TRequest> = {
  path: string;
  method: "GET" | "POST";
  body?: TRequest;
  preflight?: ApiRequestPreflight;
  allowAuthRefresh?: boolean;
};

type AccessTokenResponse = {
  accessToken: string;
};

let inFlightRefresh: Promise<string | null> | null = null;

export function getApiBaseUrl() {
  return (
    (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, "") ||
    "http://localhost:8080"
  );
}

async function parseApiResponse<TResponse>(response: Response): Promise<TResponse> {
  if (!response.ok) {
    const payload = (await response.json().catch(() => ({}))) as ApiError;
    throw new ApiResponseError(response.status, payload);
  }

  return (await response.json()) as TResponse;
}

async function executeRequest<TRequest>({
  path,
  method,
  body,
  preflight,
  allowAuthRefresh = true,
}: RequestOptions<TRequest>): Promise<Response> {
  await preflight?.();

  const token = getStoredAccessToken();
  const headers: Record<string, string> = {};

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  if (token && shouldAttachBearerToken(path)) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    method,
    headers: Object.keys(headers).length > 0 ? headers : undefined,
    body: body === undefined ? undefined : JSON.stringify(body),
    credentials: "include",
  });

  if (!allowAuthRefresh || ![401, 403].includes(response.status) || !shouldAttemptRefresh(path)) {
    return response;
  }

  const refreshedToken = await refreshAccessToken();
  if (!refreshedToken) {
    return response;
  }

  return executeRequest({ path, method, body, preflight: undefined, allowAuthRefresh: false });
}

export async function executeApiQuery<TResponse>({
  path,
  preflight,
}: ExecuteApiQueryOptions): Promise<TResponse> {
  const response = await executeRequest({ path, method: "GET", preflight });

  return parseApiResponse<TResponse>(response);
}

export async function executeApiAction({ path, preflight }: ExecuteApiQueryOptions): Promise<void> {
  const response = await executeRequest({ path, method: "POST", preflight });

  if (!response.ok) {
    const payload = (await response.json().catch(() => ({}))) as ApiError;
    throw new ApiResponseError(response.status, payload);
  }
}

export async function executeApiActionWithResponse<TResponse, TRequest = never>({
  path,
  body,
  preflight,
}: ExecuteApiActionOptions<TRequest>): Promise<TResponse> {
  const response = await executeRequest({ path, method: "POST", body, preflight });

  return parseApiResponse<TResponse>(response);
}

function shouldAttachBearerToken(path: string): boolean {
  return path !== REFRESH_PATH && path !== LOGOUT_PATH;
}

function shouldAttemptRefresh(path: string): boolean {
  return path !== REFRESH_PATH && path !== LOGOUT_PATH;
}

async function refreshAccessToken(): Promise<string | null> {
  if (inFlightRefresh) {
    return inFlightRefresh;
  }

  inFlightRefresh = (async () => {
    const response = await fetch(`${getApiBaseUrl()}${REFRESH_PATH}`, {
      method: "POST",
      credentials: "include",
    });

    if ([401, 403].includes(response.status)) {
      clearStoredAccessToken();
      return null;
    }

    const payload = await parseApiResponse<AccessTokenResponse>(response);
    storeAccessToken(payload.accessToken);
    return payload.accessToken;
  })();

  try {
    return await inFlightRefresh;
  } finally {
    inFlightRefresh = null;
  }
}
