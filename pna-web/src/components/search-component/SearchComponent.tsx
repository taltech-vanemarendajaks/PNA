import { type ChangeEvent, useState } from "react";
import { isAuthenticationError } from "../../api/command";
import { searchNumber } from "../../api/requests";
import type { NumberLogItem } from "../../lib/numberLog";
import { Alert } from "../common/Alert";
import { NumberLogComponent } from "../number-log/NumberLogComponent";
import { validatePhoneNumber } from "./SearchComponent.validation";

type SearchComponentProps = {
  onUnauthenticated?: () => void;
};

export function getSearchErrorMessage(
  searchError: unknown,
  onUnauthenticated?: () => void,
): string | null {
  if (isAuthenticationError(searchError)) {
    onUnauthenticated?.();
    return null;
  }

  return searchError instanceof Error ? searchError.message : "Search failed.";
}

export function SearchComponent({ onUnauthenticated }: SearchComponentProps) {
  const [phoneNumber, setPhoneNumber] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [resultMessage, setResultMessage] = useState<NumberLogItem | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handlePhoneNumberChange(event: ChangeEvent<HTMLInputElement>) {
    const nextPhoneNumber = event.currentTarget.value;
    setPhoneNumber(nextPhoneNumber);

    if (error) {
      setError(validatePhoneNumber(nextPhoneNumber));
    }
  }

  const handleSubmit = async () => {
    const validationError = validatePhoneNumber(phoneNumber);
    setError(validationError);
    setResultMessage(null);

    if (validationError) {
      return;
    }

    setIsSubmitting(true);

    try {
      const result = await searchNumber(phoneNumber);
      setResultMessage(result);
    } catch (searchError: unknown) {
      const searchErrorMessage = getSearchErrorMessage(searchError, onUnauthenticated);

      if (!searchErrorMessage) {
        setError(null);
        return;
      }

      setError(searchErrorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <fieldset className="fieldset bg-base-200 border-base-300 rounded-box w-full border p-4 mb-4">
        <legend className="fieldset-legend text-2xl text-primary">Search</legend>

        {error ? <Alert type="error" message={error} /> : null}

        <input
          type="tel"
          inputMode="tel"
          autoComplete="tel"
          className="input w-full"
          placeholder="Phone Number"
          value={phoneNumber}
          onChange={handlePhoneNumberChange}
        />

        <button
          className="btn btn-primary mt-4"
          type="button"
          onClick={() => {
            void handleSubmit();
          }}
          disabled={isSubmitting}
        >
          {isSubmitting ? "Searching..." : "Look Up"}
        </button>
      </fieldset>
      <div>
        <NumberLogComponent log={resultMessage} />
      </div>
    </>
  );
}
