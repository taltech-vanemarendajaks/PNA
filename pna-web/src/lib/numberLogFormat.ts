import type { SearchNumberResult } from "../api/requests";

export function buildDescription(result: SearchNumberResult): string {
  let description = "";

  const fields = [
    ["Carrier", result.carrier],
    ["Country", result.country],
    ["Country Code", result.countryCode],
    ["Region Code", result.regionCode],
    ["Number Type", result.numberType],
  ];

  for (const [label, value] of fields) {
    description = `${description}${label}: ${value || "Unknown"}; `;
  }

  if (result.timeZones && result.timeZones.length > 0) {
    return `${description}Time Zones: ${result.timeZones.join(", ")}`;
  }

  return description;
}
