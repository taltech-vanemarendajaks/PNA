import type { GoogleAuthResponse } from "../lib/googleAuth";
import { executeApiAction, executeApiQuery, getApiBaseUrl } from "./command";

const FRONTEND_ORIGIN_QUERY_PARAMETER = "frontendOrigin";
const RETURN_PATH_QUERY_PARAMETER = "returnPath";

type RedirectLoginLocation = Pick<Location, "origin" | "pathname" | "search" | "protocol">;

export async function getSession(): Promise<GoogleAuthResponse | null> {
  try {
    return await executeApiQuery<GoogleAuthResponse>({ path: "/api/v1/auth/session" });
  } catch (error: unknown) {
    if (error instanceof Error && error.message === "Not authenticated") {
      return null;
    }

    throw error;
  }
}

export function buildGoogleRedirectLoginUri(apiBaseUrl = getApiBaseUrl()): string {
  return `${apiBaseUrl}/api/v1/auth/google/redirect`;
}

export function buildGoogleRedirectReturnPath(
  location: Pick<RedirectLoginLocation, "pathname" | "search">,
): string {
  const returnPath = `${location.pathname}${location.search}`;
  return returnPath || "/";
}

export function buildGoogleRedirectLoginUriWithContext(
  location: RedirectLoginLocation,
  apiBaseUrl = getApiBaseUrl(),
): string {
  const query = new URLSearchParams({
    [FRONTEND_ORIGIN_QUERY_PARAMETER]: location.origin,
    [RETURN_PATH_QUERY_PARAMETER]: buildGoogleRedirectReturnPath(location),
  });

  return `${buildGoogleRedirectLoginUri(apiBaseUrl)}?${query.toString()}`;
}

export function startGoogleLoginWithRedirect(
  location: RedirectLoginLocation,
  navigate: (url: string) => void,
): void {
  navigate(buildGoogleRedirectLoginUriWithContext(location));
}

export async function logout(): Promise<void> {
  await executeApiAction({ path: "/api/v1/auth/logout" });
}
