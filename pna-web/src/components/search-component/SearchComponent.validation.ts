const PHONE_SEARCH_REGEX = /^\+?[\d\s()-]+$/;

export function validatePhoneNumber(value: string): string | null {
  const normalizedPhone = value.replace(/[^\d]/g, "");

  if (normalizedPhone.length === 0) {
    return "Enter a phone number.";
  }

  if (!PHONE_SEARCH_REGEX.test(value)) {
    return "Use digits, spaces, parentheses, dashes, and an optional leading +.";
  }

  if (normalizedPhone.length < 7 || normalizedPhone.length > 15) {
    return "Enter a phone number with 7 to 15 digits.";
  }

  return null;
}
