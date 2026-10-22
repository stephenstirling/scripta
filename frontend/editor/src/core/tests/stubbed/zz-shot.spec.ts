import { test, expect } from "@app/tests/helpers/stub-test-base";
import path from "path";
const SAMPLE = path.join(import.meta.dirname, "../test-fixtures/sample.pdf");
test("shots", async ({ page }) => {
  await page.route("**/api/v1/scan/capabilities**", (route) =>
    route.fulfill({
      json: {
        sources: [
          { source: "Platen", maxWidth: 2550, maxHeight: 3507 },
          { source: "Feeder", maxWidth: 2550, maxHeight: 4200 },
        ],
        resolutions: [150, 300, 600],
        colorModes: ["RGB24", "Grayscale8", "BlackAndWhite1"],
      },
    }),
  );
  await page.route("**/api/v1/ui-data/ocr-pdf", (route) =>
    route.fulfill({
      json: { languages: ["eng", "deu", "fra", "por", "chi_sim"], bestLanguages: ["eng", "deu", "fra"] },
    }),
  );
  await page.goto("/");
  await page.waitForLoadState("domcontentloaded");
  await page.locator('[data-testid="file-input"]').first().setInputFiles(SAMPLE);
  await expect(page.getByText(/\/\s*1/).first()).toBeVisible({ timeout: 60_000 });
  await page.getByTestId("title-bar-menu").click();
  await page.getByRole("menuitem", { name: /Scan from a scanner/ }).click();
  await page.getByLabel("Scanner address").fill("192.168.1.50");
  await page.getByRole("button", { name: "Check scanner" }).click();
  await page.waitForTimeout(800);
  await page.keyboard.press("Escape");
  await page.waitForTimeout(800);
  const probe = async (label: string) => {
    const r = await page.evaluate(() => {
      const out: Record<string, unknown> = {};
      for (const sel of [".title-bar-chrome", ".title-bar-body", ".app-frame", "[data-tour='workbench']"]) {
        const el = document.querySelector(sel) as HTMLElement | null;
        out[sel] = el ? `${Math.round(el.getBoundingClientRect().top)}-${Math.round(el.getBoundingClientRect().bottom)} st=${el.scrollTop}` : "none";
      }
      return out;
    });
    console.log("PROBE " + label, JSON.stringify(r));
  };
  await probe("after-escape");
  await page.getByText("OCR / Cleanup scans").first().click();
  await page.waitForTimeout(1500);
  await probe("after-ocr");
});
