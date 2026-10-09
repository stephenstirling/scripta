import { describe, expect, test } from "vitest";
import {
  defaultParameters,
  validateHeaderFooterParameters,
} from "@app/components/tools/headerFooter/useHeaderFooterParameters";
import {
  headerFooterFromApiParams,
  headerFooterToApiParams,
} from "@app/components/tools/headerFooter/useHeaderFooterOperation";

describe("Header & footer parameters", () => {
  test("needs text in at least one slot", () => {
    expect(validateHeaderFooterParameters(defaultParameters)).toBe(true);
    expect(
      validateHeaderFooterParameters({
        ...defaultParameters,
        footerCenter: "  ",
      }),
    ).toBe(false);
  });

  test("round-trips through the API parameters", () => {
    const params = {
      ...defaultParameters,
      headerLeft: "{filename}",
      pagesToApply: "2-4",
    };
    expect(headerFooterFromApiParams(headerFooterToApiParams(params))).toEqual(
      params,
    );
  });
});
