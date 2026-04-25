import type { SavedNumberSearchResponse, SearchNumberResult } from "../api/requests";
import type { NumberLogItem } from "./numberLog";

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

export function buildNumberLogItem(search: SavedNumberSearchResponse): NumberLogItem {
  const latestResult = search.results[0];

  return {
    id: search.id,
    phoneNumber: latestResult?.internationalFormat ?? search.number,
    dateSearched: search.createdAt,
    results: search.results.map((result) => ({
      description: buildDescription(result),
      logDate: result.createdAt,
    })),
  };
}
