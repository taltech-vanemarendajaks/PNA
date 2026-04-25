import { TrashIcon } from "@heroicons/react/24/outline";

type DeleteButtonProps = {
  variant?: "small" | "large";
};

export function DeleteButton({ variant = "small" }: DeleteButtonProps) {
  const sizeClass = variant === "small" ? "w-8 h-8" : "w-16 h-16";
  const iconSizeClass = variant === "small" ? "size-4" : "size-8";

  return (
    <button className={`btn btn-outline btn-error p-2 ${sizeClass}`} type="button">
      <TrashIcon className={iconSizeClass} />
    </button>
  );
}
