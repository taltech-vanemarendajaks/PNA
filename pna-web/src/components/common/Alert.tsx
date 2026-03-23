import { CheckCircleIcon, ExclamationTriangleIcon, XCircleIcon } from "@heroicons/react/24/outline";

const alertIconClassName = "size-6 shrink-0";

export function Alert({
  type,
  message,
}: {
  type: "success" | "error" | "warning";
  message: string;
}) {
  return (
    <div role="alert" className={`alert alert-${type} mb-4`}>
      {type === "error" && <XCircleIcon className={alertIconClassName} />}
      {type === "warning" && <ExclamationTriangleIcon className={alertIconClassName} />}
      {type === "success" && <CheckCircleIcon className={alertIconClassName} />}
      <span>{message}</span>
    </div>
  );
}
