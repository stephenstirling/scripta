import { useEffect, useState } from "react";
import apiClient from "@app/services/apiClient";

let cached: string[] | null = null;

/**
 * Languages the server has a Tesseract "best" model for. Empty until the lookup
 * returns, and empty for good where none are installed, so callers hide the
 * Accuracy setting in both cases.
 */
export function useOcrBestLanguages(): string[] {
  const [languages, setLanguages] = useState<string[]>(cached ?? []);

  useEffect(() => {
    if (cached) return;
    let cancelled = false;
    apiClient
      .get<{ bestLanguages?: string[] }>("/api/v1/ui-data/ocr-pdf")
      .then(({ data }) => {
        cached = data.bestLanguages ?? [];
        if (!cancelled) setLanguages(cached);
      })
      .catch(() => {
        // A failed lookup leaves the setting hidden; the standard models still work.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return languages;
}
