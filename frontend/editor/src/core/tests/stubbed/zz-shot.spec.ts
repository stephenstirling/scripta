import { test, expect } from "@app/tests/helpers/stub-test-base";
import path from "path";
const SAMPLE = path.join(import.meta.dirname, "../test-fixtures/sample.pdf");
test("shots", async ({ page }) => {
  await page.goto("/");
  await page.waitForLoadState("domcontentloaded");
  await page.screenshot({ path: "shots/00-empty.png" });
  await page.locator('[data-testid="file-input"]').first().setInputFiles(SAMPLE);
  await expect(page.getByText(/\/\s*1/).first()).toBeVisible({ timeout: 60_000 });
  await page.waitForTimeout(1500);
  await page.screenshot({ path: "shots/01-viewer.png" });
  await page.getByTestId("title-bar-theme-toggle").click();
  await page.waitForTimeout(800);
  await page.screenshot({ path: "shots/02-dark.png" });
  await page.getByRole("tab", { name: "Convert" }).click();
  await page.waitForTimeout(1200);
  await page.screenshot({ path: "shots/03-dark-convert.png" });
});
