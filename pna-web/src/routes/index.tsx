import { createFileRoute, redirect } from "@tanstack/react-router";
import { consumeAccessTokenFromRedirect, getSession } from "../api/auth/auth";
import { GoogleLoginPanel } from "../components/GoogleLoginPanel";

export async function redirectAuthenticatedUser() {
  if (typeof window !== "undefined") {
    consumeAccessTokenFromRedirect(window.location);
  }

  const session = await getSession();

  if (session) {
    throw redirect({ to: "/search" });
  }
}

export const Route = createFileRoute("/")({
  beforeLoad: async () => redirectAuthenticatedUser(),
  component: IndexRoute,
});

function IndexRoute() {
  return <GoogleLoginPanel />;
}
