import type { NumberLogItem, NumberResult } from "../../lib/numberLog";
import { DeleteButton } from "./DeleteButton";
import "./NumberLogComponent.css";

type NumberLogComponentProps = {
  log: NumberLogItem | null;
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
  if (!log) {
    return;
  }

  const latestResult = log.results[0];
  const resultCount = log.results.length;

  if (resultCount <= 1) {
    return (
      <div className="bg-base-100 border-base-300 border rounded-box p-4">
        <div className="w-[90%]">
          <h3 className="font-semibold text-xl text-accent">{log.phoneNumber}</h3>
          <div className="text-sm">
            <span className="font-semibold">Date Searched: </span>
            <span>{formatLocalDateTime(log.dateSearched)}</span>
          </div>
          <div className="text-sm">
            {latestResult && (
              <>
                <span className="font-semibold">Latest find: </span>
                <span>{formatResult(latestResult)}</span>
              </>
            )}
          </div>
        </div>
        <div className="justify-end -mt-6 hidden md:flex">
          <DeleteButton />
        </div>
      </div>
    );
  }

  return (
    <details className="group collapse collapse-arrow bg-base-100 border-base-300 border">
      <summary className="collapse-title flex flex-col after:top-8!">
        <h3 className="font-semibold text-xl text-accent">{`${log.phoneNumber} (${resultCount})`}</h3>
        <div className="text-sm">
          <span className="font-semibold">Date Searched: </span>
          <span>{formatLocalDateTime(log.dateSearched)}</span>
        </div>
        <div className="grid motion-preview-collapse">
          <div className="overflow-hidden motion-preview-fade">
            <div className="text-sm">
              {latestResult && (
                <>
                  <span className="font-semibold">Latest find: </span>
                  <span>{formatResult(latestResult)}</span>
                </>
              )}
            </div>
            <div className="flex justify-end -mt-6 -mr-8">
              <DeleteButton />
            </div>
          </div>
        </div>
      </summary>
      <div className="collapse-content text-sm">
        <div className="motion-expanded-fade">
          {log.results.map((result, index) => (
            <p key={`${result.logDate}-${result.description}`}>
              {index + 1}. {formatResult(result)}
            </p>
          ))}
          <div className="justify-end -mt-6 hidden md:flex">
            <DeleteButton />
          </div>
        </div>
      </div>
    </details>
  );
}
