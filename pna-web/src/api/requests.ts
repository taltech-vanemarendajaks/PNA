import type { NumberLogItem } from "../lib/numberLog";
import { buildDescription, buildNumberLogItem } from "../lib/numberLogFormat";
import { executeApiActionWithResponse, executeApiQuery } from "./command";

type SearchNumberRequest = {
  number: string;
};

export type SearchNumberResult = {
  country: string | null;
  countryCode: number | null;
  regionCode: string | null;
  numberType: string | null;
  internationalFormat: string | null;
  carrier: string | null;
  timeZones: string[] | null;
};

export type SearchNumberResponse = {
  result: SearchNumberResult;
};

export type SavedNumberSearchResponse = {
  id: number;
  number: string;
  result: SearchNumberResult;
  createdAt: string;
};

export async function getAllNumberSearches(): Promise<NumberLogItem[]> {
  const response = await executeApiQuery<SavedNumberSearchResponse[]>({
    path: "/api/v1/number/all",
  });

  return response.map(buildNumberLogItem);
}

export async function searchNumber(number: string): Promise<NumberLogItem> {
  const response = await executeApiActionWithResponse<SearchNumberResponse, SearchNumberRequest>({
    path: "/api/v1/number/search",
    body: { number },
  });

  const result = response.result;
  const description = buildDescription(result);
  const timestamp = new Date().toISOString();

  return {
    phoneNumber: result.internationalFormat ?? number,
    dateSearched: timestamp,
    results: [
      {
        description,
        logDate: timestamp,
      },
    ],
  };
}
