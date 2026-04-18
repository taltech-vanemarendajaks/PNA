import type { GoogleAuthResponse } from "../../lib/googleAuth";
import { ApiResponseError, executeApiAction, executeApiQuery, getApiBaseUrl, isAuthenticationError } from "../command";
import { AUTH_LOGOUT_PATH, AUTH_SESSION_PATH, GOOGLE_REDIRECT_PATH } from "./authPaths";

const FRONTEND_ORIGIN_QUERY_PARAMETER = "frontendOrigin";
const RETURN_PATH_QUERY_PARAMETER = "returnPath";
type RedirectLoginLocation = Pick<Location, "origin" | "pathname" | "search" | "protocol">;

export async function getSession(): Promise<GoogleAuthResponse | null> {
  try {
    return await executeApiQuery<GoogleAuthResponse>({ path: AUTH_SESSION_PATH });
  } catch (error: unknown) {
    if (isAuthenticationError(error)) {
      return null;
    }

    throw error;
  }
}

export async function requireAuthenticatedSession(): Promise<void> {
  const session = await getSession();

  if (!session) {
    throw new ApiResponseError(401, { error: "Not authenticated" });
  }
}

export function buildGoogleRedirectLoginUri(apiBaseUrl = getApiBaseUrl()): string {
  return `${apiBaseUrl}${GOOGLE_REDIRECT_PATH}`;
}

export function buildGoogleRedirectReturnPath(
  location: Pick<RedirectLoginLocation, "pathname" | "search">,
): string {
  const returnPath = `${location.pathname}${location.search}`;

  if (returnPath === "/") {
    return "/search";
  }

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

export async function startGoogleLoginWithRedirect(
  location: RedirectLoginLocation,
  navigate: (url: string) => void,
): Promise<void> {
  const session = await getSession();

  if (session) {
    navigate(buildGoogleRedirectReturnPath(location));
    return;
  }

  navigate(buildGoogleRedirectLoginUriWithContext(location));
}

export async function logout(): Promise<void> {
  await executeApiAction({ path: AUTH_LOGOUT_PATH });
}
