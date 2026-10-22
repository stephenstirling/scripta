import { test } from "@app/tests/helpers/stub-test-base";
test("home", async ({ page }) => {
  await page.goto("/");
  await page.waitForLoadState("domcontentloaded");
  await page.waitForTimeout(2500);
  await page.screenshot({ path: "shots/20-home.png" });
});
