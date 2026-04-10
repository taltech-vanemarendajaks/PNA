export type AuthUser = {
  subject: string;
  email: string | null;
  emailVerified: boolean | null;
  name: string | null;
  picture: string | null;
  givenName: string | null;
  familyName: string | null;
};

const AUTH_USER_STORAGE_KEY = "pna.auth.user";
const AUTH_ID_TOKEN_STORAGE_KEY = "pna.auth.idToken";
const LEGACY_GOOGLE_USER_STORAGE_KEY = "pna.auth.googleUser";
const LEGACY_GOOGLE_ID_TOKEN_STORAGE_KEY = "pna.auth.googleIdToken";

function parseStoredUser(stored: string, storage: Storage, key: string): AuthUser | null {
  try {
    return JSON.parse(stored) as AuthUser;
  } catch {
    storage.removeItem(key);
    return null;
  }
}

function normalizeToken(token: string | null): string | null {
  return token && token.trim().length > 0 ? token : null;
}

export function getStoredUser(storage: Storage = window.localStorage): AuthUser | null {
  const primaryStored = storage.getItem(AUTH_USER_STORAGE_KEY);

  if (primaryStored) {
    return parseStoredUser(primaryStored, storage, AUTH_USER_STORAGE_KEY);
  }

  const legacyStored = storage.getItem(LEGACY_GOOGLE_USER_STORAGE_KEY);

  if (!legacyStored) {
    return null;
  }

  const user = parseStoredUser(legacyStored, storage, LEGACY_GOOGLE_USER_STORAGE_KEY);

  if (user) {
    storage.setItem(AUTH_USER_STORAGE_KEY, JSON.stringify(user));
  }

  return user;
}

export function getStoredIdToken(storage: Storage = window.localStorage): string | null {
  const primaryToken = normalizeToken(storage.getItem(AUTH_ID_TOKEN_STORAGE_KEY));

  if (primaryToken) {
    return primaryToken;
  }

  const legacyToken = normalizeToken(storage.getItem(LEGACY_GOOGLE_ID_TOKEN_STORAGE_KEY));

  if (legacyToken) {
    storage.setItem(AUTH_ID_TOKEN_STORAGE_KEY, legacyToken);
  }

  return legacyToken;
}

export function saveSession(
  user: AuthUser,
  idToken: string,
  storage: Storage = window.localStorage,
): void {
  storage.setItem(AUTH_USER_STORAGE_KEY, JSON.stringify(user));
  storage.setItem(AUTH_ID_TOKEN_STORAGE_KEY, idToken);
}

export function clearSession(storage: Storage = window.localStorage): void {
  storage.removeItem(AUTH_USER_STORAGE_KEY);
  storage.removeItem(AUTH_ID_TOKEN_STORAGE_KEY);
  storage.removeItem(LEGACY_GOOGLE_USER_STORAGE_KEY);
  storage.removeItem(LEGACY_GOOGLE_ID_TOKEN_STORAGE_KEY);
}

export function hasStoredSession(storage: Storage = window.localStorage): boolean {
  return getStoredUser(storage) !== null && getStoredIdToken(storage) !== null;
}
