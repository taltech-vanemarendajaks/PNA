export type NumberResult = {
  description: string;
  logDate: string;
};

export type NumberLogItem = {
  id: string;
  phoneNumber: string;
  dateSearched: string;
  results: NumberResult[];
};
