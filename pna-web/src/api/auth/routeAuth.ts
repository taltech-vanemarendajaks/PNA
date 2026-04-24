import { redirect } from "@tanstack/react-router";
import { getSession } from "./auth";

export async function redirectUnauthenticatedUser() {
  const session = await getSession().catch(() => null);

  if (!session) {
    throw redirect({
      to: "/",
    });
  }
}
