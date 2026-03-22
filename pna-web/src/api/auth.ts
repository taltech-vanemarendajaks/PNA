import type { AuthUser } from "../auth/session";
import type { SessionCommandResult } from "../auth/sessionCommand";
import { executeApiCommand } from "./command";

type AuthRequest = {
  idToken: string;
};

type VerifyLoginOptions = {
  idToken: string;
};

export async function verifyLogin({
  idToken,
}: VerifyLoginOptions): Promise<SessionCommandResult<AuthUser>> {
  try {
    return {
      status: "success",
      data: await executeApiCommand<AuthUser, AuthRequest>({
        path: "/api/v1/auth/google",
        body: { idToken },
      }),
    };
  } catch (error: unknown) {
    return {
      status: "error",
      message: error instanceof Error ? error.message : "Login failed",
    };
  }
}
