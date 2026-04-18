import type { GoogleAuthResponse } from "../../lib/googleAuth";
import { ApiResponseError, executeApiQuery, getApiBaseUrl, isAuthenticationError } from "../command";
import { AUTH_SESSION_PATH, GOOGLE_REDIRECT_PATH } from "./authPaths";
import { clearStoredAccessToken, storeAccessToken } from "./tokenStorage";

const FRONTEND_ORIGIN_QUERY_PARAMETER = "frontendOrigin";
const RETURN_PATH_QUERY_PARAMETER = "returnPath";
const ACCESS_TOKEN_PARAMETER = "accessToken";

type RedirectLoginLocation = Pick<Location, "origin" | "pathname" | "search" | "protocol">;

export async function getSession(): Promise<GoogleAuthResponse | null> {
  try {
    return await executeApiQuery<GoogleAuthResponse>({ path: AUTH_SESSION_PATH });
  } catch (error: unknown) {
    if (isAuthenticationError(error)) {
      clearStoredAccessToken();
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

export function consumeAccessTokenFromRedirect(location: Pick<Location, "hash" | "search" | "pathname">): string | null {
  const fragment = location.hash.startsWith("#") ? location.hash.slice(1) : location.hash;
  const fragmentParams = new URLSearchParams(fragment);
  const queryParams = new URLSearchParams(location.search);
  const accessToken = fragmentParams.get(ACCESS_TOKEN_PARAMETER) ?? queryParams.get(ACCESS_TOKEN_PARAMETER);

  if (!accessToken?.trim()) {
    return null;
  }

  storeAccessToken(accessToken);
  fragmentParams.delete(ACCESS_TOKEN_PARAMETER);
  queryParams.delete(ACCESS_TOKEN_PARAMETER);

  if (typeof window !== "undefined") {
    const nextQuery = queryParams.toString();
    const nextFragment = fragmentParams.toString();
    const nextUrl = `${location.pathname}${nextQuery ? `?${nextQuery}` : ""}${nextFragment ? `#${nextFragment}` : ""}`;
    window.history.replaceState({}, document.title, nextUrl);
  }

  return accessToken;
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
  clearStoredAccessToken();
}
