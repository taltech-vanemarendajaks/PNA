import { useRef, useState } from "react";
import { TrashIcon } from "@heroicons/react/24/outline";
import type { NumberLogItem } from "../../lib/numberLog";
import { NumberLogComponent } from "./NumberLogComponent";

type NumberLogSwapProps = {
  log: NumberLogItem | null;
};

export function NumberLogSwap({ log }: NumberLogSwapProps) {
  const [isSwapped, setIsSwapped] = useState(false);
  const touchStartXRef = useRef<number | null>(null);

  function handleSwapOff() {
    setIsSwapped(false);
  }

  function handleSwapOffKeyDown(event: React.KeyboardEvent<HTMLButtonElement>) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      handleSwapOff();
    }
  }

  function handleTouchStart(event: React.TouchEvent<HTMLDivElement>) {
    touchStartXRef.current = event.changedTouches[0]?.clientX ?? null;
  }

  function handleTouchEnd(event: React.TouchEvent<HTMLDivElement>) {
    const touchStartX = touchStartXRef.current;
    const touchEndX = event.changedTouches[0]?.clientX;

    touchStartXRef.current = null;

    if (touchStartX == null || touchEndX == null) {
      return;
    }

    const swipeDistance = touchEndX - touchStartX;
    const swipeThreshold = 40;

    if (swipeDistance <= -swipeThreshold) {
      setIsSwapped(true);
      return;
    }

    if (swipeDistance >= swipeThreshold) {
      setIsSwapped(false);
    }
  }

  return (
    <div
      className={`swap swap-flip ${isSwapped ? "swap-active" : ""}`}
      onTouchEnd={handleTouchEnd}
      onTouchStart={handleTouchStart}
    >
      <button
        className="swap-on"
        onClick={handleSwapOff}
        onKeyDown={handleSwapOffKeyDown}
        type="button"
      >
        <div className="flex items-center justify-center h-full w-full">
          <span className="btn btn-outline btn-error p-2 w-16 h-16">
            <TrashIcon className="size-8" />
          </span>
        </div>
      </button>
      <div className="swap-off">
        <NumberLogComponent log={log} />
      </div>
    </div>
  );
}
