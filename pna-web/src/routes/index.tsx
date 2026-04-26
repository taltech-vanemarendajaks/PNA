import { createFileRoute } from "@tanstack/react-router";
import { GoogleLoginPanel } from "../components/GoogleLoginPanel";

export const Route = createFileRoute("/")({
  component: IndexRoute,
});

function IndexRoute() {
  return <GoogleLoginPanel />;
}
