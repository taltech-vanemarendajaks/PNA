import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import type { NumberLogItem } from "../../lib/numberLog";
import { NumberLogComponent } from "./NumberLogComponent";

function formatExpectedDate(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "short",
  }).format(new Date(value));
}

function renderComponent(log: NumberLogItem) {
  return renderToStaticMarkup(<NumberLogComponent log={log} />);
}

describe("NumberLogComponent", () => {
  it("renders a non-collapsible card for a single result", () => {
    const log: NumberLogItem = {
      id: "1",
      phoneNumber: "+372 5551 2048",
      dateSearched: "2026-03-18T09:42:00Z",
      results: [
        {
          description: "Sales person at a car dealership",
          logDate: "2026-03-17T16:10:00Z",
        },
      ],
    };

    const markup = renderComponent(log);

    expect(markup).toContain(log.phoneNumber);
    expect(markup).toContain("Date Searched:");
    expect(markup).toContain(formatExpectedDate(log.dateSearched));
    expect(markup).toContain("Latest find:");
    expect(markup).toContain(log.results[0].description);
    expect(markup).toContain(formatExpectedDate(log.results[0].logDate));
    expect(markup).not.toContain("<details");
  });

  it("renders multiple results in the order provided inside a collapsible details block", () => {
    const log: NumberLogItem = {
      id: "2",
      phoneNumber: "+372 5887 3164",
      dateSearched: "2026-03-20T14:17:00Z",
      results: [
        {
          description: "Customer support representative",
          logDate: "2026-03-15T10:05:00Z",
        },
        {
          description: "Doctor at a hospital",
          logDate: "2026-03-11T08:25:00Z",
        },
      ],
    };

    const markup = renderComponent(log);
    const latestResult = log.results[0];
    const firstEntry = `1. ${latestResult.description} (${formatExpectedDate(latestResult.logDate)})`;
    const secondEntry = `2. Doctor at a hospital (${formatExpectedDate("2026-03-11T08:25:00Z")})`;

    expect(markup).toContain("<details");
    expect(markup).toContain(`${log.phoneNumber} (${log.results.length})`);
    expect(markup).toContain("Date Searched:");
    expect(markup).toContain(formatExpectedDate(log.dateSearched));
    expect(markup).toContain(latestResult.description);
    expect(markup).toContain(formatExpectedDate(latestResult.logDate));
    expect(markup).toContain(firstEntry);
    expect(markup).toContain(secondEntry);
    expect(markup.indexOf(firstEntry)).toBeLessThan(markup.indexOf(secondEntry));
  });
});
