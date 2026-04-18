const ACCESS_TOKEN_STORAGE_KEY = "pna.accessToken";

const memoryStorage = createMemoryStorage();

function getStorage(): Storage {
  if (typeof window !== "undefined") {
    return window.localStorage;
  }

  return memoryStorage;
}

export function getStoredAccessToken(storage: Storage = getStorage()): string | null {
  const token = storage.getItem(ACCESS_TOKEN_STORAGE_KEY);
  return token?.trim() ? token : null;
}

export function storeAccessToken(token: string, storage: Storage = getStorage()): void {
  storage.setItem(ACCESS_TOKEN_STORAGE_KEY, token);
}

export function clearStoredAccessToken(storage: Storage = getStorage()): void {
  storage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
}

function createMemoryStorage(): Storage {
  const values = new Map<string, string>();

  return {
    get length() {
      return values.size;
    },
    clear() {
      values.clear();
    },
    getItem(key: string) {
      return values.get(key) ?? null;
    },
    key(index: number) {
      return Array.from(values.keys())[index] ?? null;
    },
    removeItem(key: string) {
      values.delete(key);
    },
    setItem(key: string, value: string) {
      values.set(key, value);
    },
  };
}
