import { createFileRoute } from "@tanstack/react-router";
import { redirectUnauthenticatedUser } from "../api/auth/routeAuth";

export const Route = createFileRoute("/settings")({
  beforeLoad: () => redirectUnauthenticatedUser(),
  component: SettingsRoute,
});

function SettingsRoute() {}
