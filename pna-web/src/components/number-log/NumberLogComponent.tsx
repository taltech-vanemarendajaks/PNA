export type NumberResult = {
  description: string;
  logDate: string;
};

export type NumberLogItem = {
  phoneNumber: string;
  dateSearched: string;
  results: NumberResult[];
};

type NumberLogComponentProps = {
  log: NumberLogItem;
};

function formatLocalDateTime(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "short",
  }).format(date);
}

function formatResult(result: NumberResult) {
  return `${result.description} (${formatLocalDateTime(result.logDate)})`;
}

export function NumberLogComponent({ log }: NumberLogComponentProps) {
  const latestResult = log.results[0];
  const resultCount = log.results.length;

  if (resultCount <= 1) {
    return (
      <div className="bg-base-100 border-base-300 border rounded-box p-4">
        <h3 className="font-semibold text-xl text-accent">{log.phoneNumber}</h3>
        <p className="text-sm">Date Searched: {formatLocalDateTime(log.dateSearched)}</p>
        {latestResult && <p className="text-sm">Latest find: {formatResult(latestResult)}</p>}
      </div>
    );
  }

  return (
    <details className="group collapse collapse-arrow bg-base-100 border-base-300 border">
      <summary className="collapse-title">
        <h3 className="font-semibold text-xl text-accent">{`${log.phoneNumber} (${resultCount})`}</h3>
        <p className="text-sm">Date Searched: {formatLocalDateTime(log.dateSearched)}</p>
        {latestResult && <p className="group-open:hidden text-sm">{formatResult(latestResult)}</p>}
      </summary>
      <div className="collapse-content text-sm">
        {log.results.map((result, index) => (
          <p key={`${result.logDate}-${result.description}`}>
            {index + 1}. {formatResult(result)}
          </p>
        ))}
      </div>
    </details>
  );
}
