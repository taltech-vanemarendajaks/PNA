import type { ApiError } from "../lib/googleAuth";

type ExecuteApiQueryOptions = {
  path: string;
};

type RequestOptions<TRequest> = {
  path: string;
  method: "GET" | "POST";
  body?: TRequest;
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
    throw new Error(payload.error ?? "Request failed");
  }

  return (await response.json()) as TResponse;
}

async function executeRequest<TRequest>({
  path,
  method,
  body,
}: RequestOptions<TRequest>): Promise<Response> {
  return fetch(`${getApiBaseUrl()}${path}`, {
    method,
    credentials: "include",
    headers: body === undefined ? undefined : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

export async function executeApiQuery<TResponse>({
  path,
}: ExecuteApiQueryOptions): Promise<TResponse> {
  const response = await executeRequest({ path, method: "GET" });

  return parseApiResponse<TResponse>(response);
}

export async function executeApiAction({ path }: ExecuteApiQueryOptions): Promise<void> {
  const response = await executeRequest({ path, method: "POST" });

  if (!response.ok) {
    const payload = (await response.json().catch(() => ({}))) as ApiError;
    throw new Error(payload.error ?? "Request failed");
  }
}
