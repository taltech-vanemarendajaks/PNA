import { TrashIcon } from "@heroicons/react/24/outline";

export function DeleteButton() {
  return (
    <button className="btn btn-outline btn-error p-2 w-8 h-8">
      <TrashIcon className="size-4" />
    </button>
  );
}
