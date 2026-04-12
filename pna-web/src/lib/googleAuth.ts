export type GoogleAuthResponse = {
  subject: string;
  email: string | null;
  emailVerified: boolean | null;
  name: string | null;
  picture: string | null;
  givenName: string | null;
  familyName: string | null;
};

export type ApiError = {
  error?: string;
};
