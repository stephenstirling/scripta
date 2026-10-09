import { describe, expect, test } from "vitest";
import {
  defaultParameters,
  formatBatesLabel,
  validateBatesNumberingParameters,
} from "@app/components/tools/batesNumbering/useBatesNumberingParameters";
import {
  batesFromApiParams,
  batesToApiParams,
} from "@app/components/tools/batesNumbering/useBatesNumberingOperation";

describe("Bates numbering parameters", () => {
  test("formats labels with prefix, padding and suffix", () => {
    expect(
      formatBatesLabel({ prefix: "ACME", suffix: "-C", digits: 6 }, 42),
    ).toBe("ACME000042-C");
  });

  test("clamps digits when previewing", () => {
    expect(formatBatesLabel({ prefix: "", suffix: "", digits: 0 }, 7)).toBe(
      "7",
    );
  });

  test("rejects negative starts and out-of-range digits", () => {
    expect(validateBatesNumberingParameters(defaultParameters)).toBe(true);
    expect(
      validateBatesNumberingParameters({ ...defaultParameters, startNumber: -1 }),
    ).toBe(false);
    expect(
      validateBatesNumberingParameters({ ...defaultParameters, digits: 16 }),
    ).toBe(false);
  });

  test("round-trips through the API parameters", () => {
    const params = { ...defaultParameters, prefix: "X", nameByRange: true };
    expect(batesFromApiParams(batesToApiParams(params))).toEqual(params);
  });
});
