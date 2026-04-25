import { createFileRoute } from "@tanstack/react-router";
import { redirectUnauthenticatedUser } from "../api/auth/routeAuth";
import { SettingsComponent } from "../components/settings/SettingsComponent";

export const Route = createFileRoute("/settings")({
  beforeLoad: () => redirectUnauthenticatedUser(),
  component: SettingsRoute,
});

function SettingsRoute() {
  return (
    <div className="mx-auto w-full max-w-3xl">
      <SettingsComponent />
    </div>
  );
}
