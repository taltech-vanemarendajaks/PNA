import { type ChangeEvent, useState } from "react";
import { searchNumber } from "../../api/requests";
import { validatePhoneNumber } from "./SearchComponent.validation";

export function SearchComponent() {
  const [phoneNumber, setPhoneNumber] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [resultMessage, setResultMessage] = useState<string | null>(null);
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

    const result = await searchNumber(phoneNumber);

    if (result.status === "success") {
      setResultMessage(result.data.message);
    }

    if (result.status === "error") {
      setError(result.message);
    }

    setIsSubmitting(false);
  };

  return (
    <fieldset className="fieldset bg-base-200 border-base-300 rounded-box w-full border p-4">
      <legend className="fieldset-legend text-2xl text-primary">Search</legend>

      {error ? (
        <div role="alert" className="alert alert-error mb-4">
          <svg
            aria-hidden="true"
            xmlns="http://www.w3.org/2000/svg"
            className="h-6 w-6 shrink-0 stroke-current"
            fill="none"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <span>{error}</span>
        </div>
      ) : null}

      {resultMessage ? (
        <div role="status" className="alert alert-success mb-4">
          <span>{resultMessage}</span>
        </div>
      ) : null}

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
  );
}
