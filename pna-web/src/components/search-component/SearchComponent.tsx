import { type ChangeEvent, useState } from "react";
import { Alert } from "../common/Alert";
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

      {error ? <Alert type="error" message={error} /> : null}

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
