import { redirect } from "@tanstack/react-router";
import { consumeAccessTokenFromRedirect, getSession } from "./auth";

export async function redirectUnauthenticatedUser() {
  if (typeof window !== "undefined") {
    consumeAccessTokenFromRedirect(window.location);
  }

  const session = await getSession();

  if (!session) {
    throw redirect({ to: "/" });
  }
}
