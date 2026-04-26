import { useRef, useState } from "react";
import type { NumberLogItem } from "../../lib/numberLog";
import { NumberLogComponent } from "./NumberLogComponent";
import { DeleteButton } from "./DeleteButton";

type NumberLogSwapProps = {
  log: NumberLogItem;
  onDelete: (searchId: string) => void;
};

export function NumberLogSwap({ log, onDelete }: NumberLogSwapProps) {
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
      className={`swap swap-flip grid w-full place-content-stretch *:w-full ${isSwapped ? "swap-active" : ""}`}
      onTouchEnd={handleTouchEnd}
      onTouchStart={handleTouchStart}
    >
      <button
        className="swap-on w-full"
        onClick={handleSwapOff}
        onKeyDown={handleSwapOffKeyDown}
        type="button"
      >
        <div className="flex items-center justify-center h-full w-full">
          <DeleteButton variant="large" onClick={() => onDelete(log.id)} />
        </div>
      </button>
      <div className="swap-off w-full">
        <NumberLogComponent log={log} />
      </div>
    </div>
  );
}
