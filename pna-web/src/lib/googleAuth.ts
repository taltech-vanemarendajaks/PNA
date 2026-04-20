export type GoogleAuthResponse = {
  subject: string;
  email: string | null;
  name: string | null;
  givenName: string | null;
};

export type ApiError = {
  error?: string;
};
