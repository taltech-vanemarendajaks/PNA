import type { ApiError } from "../lib/googleAuth";

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
};

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
}: RequestOptions<TRequest>): Promise<Response> {
  await preflight?.();

  return fetch(`${getApiBaseUrl()}${path}`, {
    method,
    credentials: "include",
    headers: body === undefined ? undefined : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
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
