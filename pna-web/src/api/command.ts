import {
  runSessionCommand,
  type SessionCommandHandlerOptions,
  type SessionCommandResult,
} from "../auth/sessionCommand";

type ApiErrorPayload = {
  error?: string;
};

export class ApiCommandError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiCommandError";
    this.status = status;
  }
}

type CommandOptions<TBody> = {
  path: string;
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: TBody;
  headers?: HeadersInit;
  bearerToken?: string | null;
};

const DEFAULT_API_BASE_URL = "http://localhost:8080";

export async function executeApiCommand<TResponse, TBody = undefined>({
  path,
  method = "POST",
  body,
  headers,
  bearerToken,
}: CommandOptions<TBody>): Promise<TResponse> {
  const apiBaseUrl =
    (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, "") ||
    DEFAULT_API_BASE_URL;

  const requestHeaders = new Headers(headers);

  if (body !== undefined && !requestHeaders.has("Content-Type")) {
    requestHeaders.set("Content-Type", "application/json");
  }

  if (bearerToken && !requestHeaders.has("Authorization")) {
    requestHeaders.set("Authorization", `Bearer ${bearerToken}`);
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    method,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (!response.ok) {
    const payload = (await response.json().catch(() => ({}))) as ApiErrorPayload;
    throw new ApiCommandError(
      payload.error ?? `Request failed with status ${response.status}`,
      response.status,
    );
  }

  return (await response.json()) as TResponse;
}

export function executeSessionApiCommand<TResponse, TBody = undefined>(
  options: CommandOptions<TBody> & SessionCommandHandlerOptions,
): Promise<SessionCommandResult<TResponse>> {
  const { fallbackMessage, onUnauthorized, unauthorizedStatuses, ...commandOptions } = options;

  return runSessionCommand({
    command: () => executeApiCommand<TResponse, TBody>(commandOptions),
    fallbackMessage,
    onUnauthorized,
    unauthorizedStatuses,
  });
}
