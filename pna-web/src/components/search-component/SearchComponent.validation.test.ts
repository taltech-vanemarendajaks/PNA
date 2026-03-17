import { describe, expect, it } from "vitest";
import { validatePhoneNumber } from "./SearchComponent.validation";

describe("validatePhoneNumber", () => {
  it("rejects blank input", () => {
    expect(validatePhoneNumber("")).toBe("Enter a phone number.");
  });

  it("rejects formatting-only input", () => {
    expect(validatePhoneNumber("---")).toBe("Enter a phone number.");
  });

  it("rejects invalid characters", () => {
    expect(validatePhoneNumber("123abc4567")).toBe(
      "Use digits, spaces, parentheses, dashes, and an optional leading +.",
    );
  });

  it("rejects too few digits", () => {
    expect(validatePhoneNumber("123-45")).toBe("Enter a phone number with 7 to 15 digits.");
  });

  it("rejects too many digits", () => {
    expect(validatePhoneNumber("+1234567890123456")).toBe(
      "Enter a phone number with 7 to 15 digits.",
    );
  });

  it("accepts a 7-digit phone number", () => {
    expect(validatePhoneNumber("1234567")).toBeNull();
  });

  it("accepts a formatted international phone number", () => {
    expect(validatePhoneNumber("+1 (234) 567-8901")).toBeNull();
  });
});
