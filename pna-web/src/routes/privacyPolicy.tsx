import { createFileRoute } from "@tanstack/react-router";
import { PrivacyPolicy } from "../components/common/PrivacyPolicy";

export const Route = createFileRoute("/privacyPolicy")({
  component: RouteComponent,
});

function RouteComponent() {
  return PrivacyPolicy();
}
