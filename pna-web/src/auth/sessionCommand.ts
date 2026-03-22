import { ApiCommandError } from "../api/command";
import { router } from "../router";
import { clearSession } from "./session";

const DEFAULT_UNAUTHORIZED_STATUSES = [401, 403] as const;

export type SessionCommandHandlerOptions = {
  fallbackMessage: string;
  onUnauthorized?: () => Promise<unknown> | unknown;
  unauthorizedStatuses?: number[];
};

type SessionCommandOptions<TResult> = SessionCommandHandlerOptions & {
  command: () => Promise<TResult>;
};

type SessionCommandSuccess<TResult> = {
  status: "success";
  data: TResult;
};

type SessionCommandRedirect = {
  status: "handled-auth-error";
};

type SessionCommandError = {
  status: "error";
  message: string;
};

export type SessionCommandResult<TResult> =
  | SessionCommandSuccess<TResult>
  | SessionCommandRedirect
  | SessionCommandError;

async function handleDefaultUnauthorizedSession() {
  clearSession();
  await router.navigate({ to: "/" });
}

export async function runSessionCommand<TResult>({
  command,
  fallbackMessage,
  onUnauthorized,
  unauthorizedStatuses = [...DEFAULT_UNAUTHORIZED_STATUSES],
}: SessionCommandOptions<TResult>): Promise<SessionCommandResult<TResult>> {
  try {
    return {
      status: "success",
      data: await command(),
    };
  } catch (error: unknown) {
    if (error instanceof ApiCommandError && unauthorizedStatuses.includes(error.status)) {
      const unauthorizedHandler = onUnauthorized ?? handleDefaultUnauthorizedSession;

      try {
        await unauthorizedHandler();
      } catch {
        return {
          status: "error",
          message: fallbackMessage,
        };
      }

      return { status: "handled-auth-error" };
    }

    return {
      status: "error",
      message: error instanceof Error ? error.message : fallbackMessage,
    };
  }
}
