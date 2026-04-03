import { type ChangeEvent, useState } from "react";
import { Alert } from "../common/Alert";
import { validatePhoneNumber } from "./SearchComponent.validation";

export function SearchComponent() {
  const [phoneNumber, setPhoneNumber] = useState("");
  const [error, setError] = useState<string | null>(null);

  function handlePhoneNumberChange(event: ChangeEvent<HTMLInputElement>) {
    const nextPhoneNumber = event.currentTarget.value;
    setPhoneNumber(nextPhoneNumber);

    if (error) {
      setError(validatePhoneNumber(nextPhoneNumber));
    }
  }

  const handleSubmit = () => {
    const validationError = validatePhoneNumber(phoneNumber);
    setError(validationError);

    if (validationError) {
      return;
    }
  };

  return (
    <fieldset className="fieldset bg-base-200 border-base-300 rounded-box w-full border p-4">
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

      <button className="btn btn-primary mt-4" type="submit" onClick={handleSubmit}>
        Look Up
      </button>
    </fieldset>
  );
}
