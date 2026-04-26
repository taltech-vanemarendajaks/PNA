import { createFileRoute } from "@tanstack/react-router";
import { TermsOfUse } from "../components/common/Terms";

export const Route = createFileRoute("/terms")({
  component: RouteComponent,
});

function RouteComponent() {
  return <TermsOfUse />;
}
