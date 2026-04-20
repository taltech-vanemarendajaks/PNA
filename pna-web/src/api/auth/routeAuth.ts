import { redirect } from "@tanstack/react-router";
import { getSession } from "./auth";

export async function redirectUnauthenticatedUser() {
  const session = await getSession();

  if (!session) {
    throw redirect({ to: "/" });
  }
}
