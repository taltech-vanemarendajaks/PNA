import type { NumberLogItem, NumberResult } from "../../lib/numberLog";
import { DeleteButton } from "./DeleteButton";
import "./NumberLogComponent.css";

type NumberLogComponentProps = {
  log: NumberLogItem | null;
  onDelete?: (searchId: string) => void;
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

export function NumberLogComponent({ log, onDelete }: NumberLogComponentProps) {
  if (!log) {
    return;
  }

  const latestResult = log.results[0];
  const resultCount = log.results.length;

  if (resultCount <= 1) {
    return (
      <div className="bg-base-100 border-base-300 block w-full rounded-box border p-4">
        <div className="min-w-0 w-[90%]">
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
        <div className="hidden justify-end md:flex -mt-8">
          <DeleteButton onClick={() => onDelete?.(log.id)} />
        </div>
      </div>
    );
  }

  return (
    <details className="group collapse collapse-arrow bg-base-100 border-base-300 block w-full border">
      <summary className="collapse-title flex flex-col gap-2 pr-12 after:top-8!">
        <h3 className="font-semibold text-xl text-accent">{`${log.phoneNumber} (${resultCount})`}</h3>
        <div className="text-sm">
          <span className="font-semibold">Date Searched: </span>
          <span>{formatLocalDateTime(log.dateSearched)}</span>
        </div>
        <div className="grid w-[90%] motion-preview-collapse">
          <div className="min-w-0 w-full motion-preview-fade">
            <div className="text-sm">
              {latestResult && (
                <>
                  <span className="font-semibold">Latest find: </span>
                  <span>{formatResult(latestResult)}</span>
                </>
              )}
            </div>
            <div className="hidden justify-end md:flex -mt-8 -mr-25">
              <DeleteButton onClick={() => onDelete?.(log.id)} />
            </div>
          </div>
        </div>
      </summary>
      <div className="collapse-content text-sm">
        <div className="motion-expanded-fade space-y-3">
          {log.results.map((result, index) => (
            <p key={`${result.logDate}-${result.description}`}>
              {index + 1}. {formatResult(result)}
            </p>
          ))}
          <div className="hidden justify-end sm:flex -mt-8">
            <DeleteButton onClick={() => onDelete?.(log?.id)} />
          </div>
        </div>
      </div>
    </details>
  );
}
