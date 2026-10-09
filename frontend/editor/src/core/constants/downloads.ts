import { RELEASES_URL } from "@app/constants/branding";

// Scripta ships through GitHub Releases; every platform points at the
// releases page, where the right asset can be picked.
export const DOWNLOAD_URLS = {
  WINDOWS: RELEASES_URL,
  MAC: RELEASES_URL,
  LINUX_DOCS: RELEASES_URL,
} as const;

export const DOWNLOAD_BASE_URL = `${RELEASES_URL}/latest`;
