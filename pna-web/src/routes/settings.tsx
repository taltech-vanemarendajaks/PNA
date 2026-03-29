import { createFileRoute, redirect } from "@tanstack/react-router";
import { hasStoredSession } from "../auth/session";

export const Route = createFileRoute("/settings")({
  beforeLoad: () => {
    if (!hasStoredSession()) {
      throw redirect({ to: "/" });
    }
  },
  component: SettingsRoute,
});

function SettingsRoute() {}
