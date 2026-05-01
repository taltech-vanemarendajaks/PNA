import type { NumberLogItem } from "../lib/numberLog";
import { buildDescription, buildNumberLogItem } from "../lib/numberLogFormat";
import { executeApiActionWithResponse, executeApiDelete, executeApiQuery } from "./command";

type SearchNumberRequest = {
  number: string;
};

type DeleteNumberRequest = {
  searchId: string;
};

export type SearchNumberResult = {
  id: string;
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
  id: string;
  number: string;
  results: SavedSearchNumberResult[];
  createdAt: string;
};

export type SavedSearchNumberResult = SearchNumberResult & {
  createdAt: string;
};

export const NUMBER_API_BASE_ROUTE = "/api/v1/number";
export const NUMBER_WS_ROUTE = "/api/ws/number";

export async function getAllNumberSearches(): Promise<NumberLogItem[]> {
  const response = await executeApiQuery<SavedNumberSearchResponse[]>({
    path: `${NUMBER_API_BASE_ROUTE}/all`,
  });

  return response.map(buildNumberLogItem);
}

export async function searchNumber(number: string): Promise<NumberLogItem> {
  const response = await executeApiActionWithResponse<SearchNumberResponse, SearchNumberRequest>({
    path: `${NUMBER_API_BASE_ROUTE}/search`,
    body: { number },
  });

  const result = response.result;
  const description = buildDescription(result);
  const timestamp = new Date().toISOString();

  return {
    id: result.id,
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

export async function deleteNumberLog(searchId: string): Promise<void> {
  await executeApiDelete<DeleteNumberRequest>({
    path: `${NUMBER_API_BASE_ROUTE}/search/${searchId}`,
  });
}

export async function deleteAllNumberLogHistory(): Promise<void> {
  await executeApiDelete({
    path: `${NUMBER_API_BASE_ROUTE}/all`,
  });
}