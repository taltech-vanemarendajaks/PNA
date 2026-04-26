import { createFileRoute, redirect } from "@tanstack/react-router";
import { getSession } from "../api/auth/auth";

async function requireAuthenticatedSession() {
  const session = await getSession();

  if (!session) {
    throw redirect({ to: "/" });
  }

  return session;
}

export const Route = createFileRoute("/settings")({
  beforeLoad: async () => requireAuthenticatedSession(),
  component: SettingsRoute,
});

function SettingsRoute() {}
