import { createFileRoute } from "@tanstack/react-router";
import { GoogleLoginPanel } from "../components/GoogleLoginPanel";
import { AboutSection } from "../components/AboutSection";

export const Route = createFileRoute("/")({
  component: IndexRoute,
});

function IndexRoute() {
  return (
    <div className="space-y-6">
      <GoogleLoginPanel />
      <AboutSection />
    </div>
  );
}
