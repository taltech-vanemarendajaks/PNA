export type NumberResult = {
  description: string;
  logDate: string;
};

export type NumberLogItem = {
  phoneNumber: string;
  dateSearched: string;
  results: NumberResult[];
};
