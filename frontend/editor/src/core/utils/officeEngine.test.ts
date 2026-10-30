import { describe, test, expect } from "vitest";
import { usesOfficeEngine } from "@app/utils/convertUtils";
import { buildConvertFormData } from "@app/hooks/tools/convert/useConvertOperation";
import { defaultParameters } from "@app/hooks/tools/convert/useConvertParameters";

describe("Office Convert switch", () => {
  test("applies to Office conversions only", () => {
    expect(usesOfficeEngine("pdf", "docx")).toBe(true);
    expect(usesOfficeEngine("pdf", "pptx")).toBe(true);
    expect(usesOfficeEngine("pdf", "xlsx")).toBe(true);
    expect(usesOfficeEngine("docx", "pdf")).toBe(true);
    expect(usesOfficeEngine("html", "pdf")).toBe(false);
    expect(usesOfficeEngine("pdf", "png")).toBe(false);
  });

  test("sends the choice only when one was made", () => {
    const file = new File(["x"], "a.pdf", { type: "application/pdf" });
    const base = {
      ...defaultParameters,
      fromExtension: "pdf",
      toExtension: "docx",
    };
    expect(
      buildConvertFormData(base, [file]).has("useScriptaOfficeConvert"),
    ).toBe(false);
    const chosen = buildConvertFormData(
      { ...base, useScriptaOfficeConvert: true },
      [file],
    );
    expect(chosen.get("useScriptaOfficeConvert")).toBe("true");
    const image = {
      ...base,
      toExtension: "png",
      useScriptaOfficeConvert: true,
    };
    expect(
      buildConvertFormData(image, [file]).has("useScriptaOfficeConvert"),
    ).toBe(false);
  });
});
