import type { GoogleAuthResponse } from "../../lib/googleAuth";
import {
  ApiResponseError,
  executeApiAction,
  executeApiQuery,
  getApiBaseUrl,
  hasApiResponseStatus,
} from "../command";
import { AUTH_SESSION_PATH, GOOGLE_REDIRECT_PATH, LOGOUT_PATH } from "./authPaths";

export const DEFAULT_AUTHENTICATED_PATH = "/search";

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
    return DEFAULT_AUTHENTICATED_PATH;
  }

  return returnPath || "/";
}

export function buildGoogleRedirectLoginUriWithContext(
  location: RedirectLoginLocation,
  apiBaseUrl = getApiBaseUrl(),
  returnPath = buildGoogleRedirectReturnPath(location),
): string {
  const query = new URLSearchParams({
    [FRONTEND_ORIGIN_QUERY_PARAMETER]: location.origin,
    [RETURN_PATH_QUERY_PARAMETER]: returnPath,
  });

  return `${buildGoogleRedirectLoginUri(apiBaseUrl)}?${query.toString()}`;
}

export async function startGoogleLoginWithRedirect(
  location: RedirectLoginLocation,
  navigate: (url: string) => void,
  returnPath = buildGoogleRedirectReturnPath(location),
): Promise<void> {
  const session = await getSession();

  if (session) {
    navigate(returnPath);
    return;
  }

  navigate(buildGoogleRedirectLoginUriWithContext(location, undefined, returnPath));
}

export async function logout(): Promise<void> {
  await executeApiAction({ path: LOGOUT_PATH });
}

export function isAuthenticationError(error: unknown): boolean {
  return hasApiResponseStatus(error, [401, 403]);
}

export function leaveAuthenticatedFlow() {
  window.location.assign("/");
}

export async function handleLogout() {
  try {
    await logout();
    leaveAuthenticatedFlow();
  } catch (error: unknown) {
    if (isAuthenticationError(error)) {
      leaveAuthenticatedFlow();
      return;
    }
  }
}
