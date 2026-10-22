import { test, expect } from "@app/tests/helpers/stub-test-base";
import path from "path";
const SAMPLE = path.join(import.meta.dirname, "../test-fixtures/sample.pdf");
test("recent", async ({ page }) => {
  await page.goto("/");
  await page.waitForLoadState("domcontentloaded");
  await page.locator('[data-testid="file-input"]').first().setInputFiles(SAMPLE);
  await expect(page.getByText(/\/\s*1/).first()).toBeVisible({ timeout: 60_000 });
  await page.getByText("Compress", { exact: true }).first().click();
  await page.waitForTimeout(800);
  await page.getByRole("tab", { name: "All tools" }).click();
  await page.waitForTimeout(800);
  await page.getByText("OCR / Cleanup scans").first().click();
  await page.waitForTimeout(800);
  await page.getByRole("tab", { name: "All tools" }).click();
  await page.waitForTimeout(1000);
  await page.screenshot({ path: "shots/21-recent.png" });
});
