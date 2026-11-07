import { useEffect, useState } from "react";
import { Modal, Select, Stack, Text, TextInput } from "@mantine/core";
import { useTranslation } from "react-i18next";
import { Button } from "@app/ui/Button";
import apiClient from "@app/services/apiClient";
import { useFileHandler } from "@app/hooks/useFileHandler";

interface ScannerCapabilities {
  sources: { source: string; maxWidth: number; maxHeight: number }[];
  resolutions: number[];
  colorModes: string[];
}

interface ScanModalProps {
  opened: boolean;
  onClose: () => void;
}

const HOST_STORAGE_KEY = "scripta.scannerHost";
const FALLBACK_RESOLUTIONS = [200, 300, 600];

// What the server accepts, keyed by the eSCL mode the scanner reports.
const COLOR_MODE_BY_ESCL: Record<string, string> = {
  RGB24: "color",
  Grayscale8: "gray",
  BlackAndWhite1: "bw",
};

function rememberedHost(): string {
  try {
    return window.localStorage.getItem(HOST_STORAGE_KEY) ?? "";
  } catch {
    return "";
  }
}

function rememberHost(host: string) {
  try {
    window.localStorage.setItem(HOST_STORAGE_KEY, host);
  } catch {
    // Not remembering the address is an inconvenience, not a failure.
  }
}

function errorMessage(error: unknown, fallback: string): string {
  const response = (
    error as { response?: { data?: { detail?: string; message?: string } } }
  ).response;
  return response?.data?.detail ?? response?.data?.message ?? fallback;
}

/**
 * Scans pages from a network scanner (eSCL/AirScan) through the server and adds
 * the result to the workspace as a PDF. The scanner must be on the same private
 * network as the server, which is where the request originates.
 */
export default function ScanModal({ opened, onClose }: ScanModalProps) {
  const { t } = useTranslation();
  const { addFiles } = useFileHandler();

  const [host, setHost] = useState(rememberedHost);
  const [capabilities, setCapabilities] = useState<ScannerCapabilities | null>(
    null,
  );
  const [source, setSource] = useState<string | null>(null);
  const [resolution, setResolution] = useState<string | null>("300");
  const [colorMode, setColorMode] = useState<string | null>("color");
  const [busy, setBusy] = useState<"checking" | "scanning" | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!opened) {
      setCapabilities(null);
      setError(null);
      setBusy(null);
    }
  }, [opened]);

  const checkScanner = async () => {
    setBusy("checking");
    setError(null);
    try {
      const { data } = await apiClient.get<ScannerCapabilities>(
        "/api/v1/scan/capabilities",
        { params: { host: host.trim() } },
      );
      setCapabilities(data);
      rememberHost(host.trim());
      setSource(data.sources[0]?.source ?? null);
      const resolutions = data.resolutions.length
        ? data.resolutions
        : FALLBACK_RESOLUTIONS;
      setResolution(String(resolutions.includes(300) ? 300 : resolutions[0]));
      const colors = data.colorModes
        .map((mode) => COLOR_MODE_BY_ESCL[mode])
        .filter(Boolean);
      setColorMode(colors.includes("color") ? "color" : (colors[0] ?? null));
    } catch (e) {
      setCapabilities(null);
      setError(
        errorMessage(
          e,
          t(
            "scan.checkFailed",
            "Could not reach a scanner at that address. It must be on your private network and support AirScan (eSCL).",
          ),
        ),
      );
    } finally {
      setBusy(null);
    }
  };

  const scan = async () => {
    setBusy("scanning");
    setError(null);
    try {
      const form = new FormData();
      form.append("host", host.trim());
      form.append("source", source ?? "Platen");
      form.append("resolution", resolution ?? "300");
      form.append("colorMode", colorMode ?? "color");
      const { data } = await apiClient.post<Blob>(
        "/api/v1/scan/scan-to-pdf",
        form,
        { responseType: "blob" },
      );
      const name = `scan-${new Date().toISOString().slice(0, 16).replace(/[:T]/g, "-")}.pdf`;
      await addFiles([new File([data], name, { type: "application/pdf" })]);
      onClose();
    } catch (e) {
      setError(
        errorMessage(
          e,
          t(
            "scan.scanFailed",
            "The scan failed. Check the scanner and try again.",
          ),
        ),
      );
    } finally {
      setBusy(null);
    }
  };

  const resolutionOptions = (
    capabilities?.resolutions.length
      ? capabilities.resolutions
      : FALLBACK_RESOLUTIONS
  ).map((value) => ({ value: String(value), label: `${value} dpi` }));

  const colorOptions = (capabilities?.colorModes ?? [])
    .map((mode) => COLOR_MODE_BY_ESCL[mode])
    .filter(Boolean)
    .map((value) => ({
      value,
      label:
        value === "color"
          ? t("scan.color", "Colour")
          : value === "gray"
            ? t("scan.gray", "Greyscale")
            : t("scan.bw", "Black and white"),
    }));

  const sourceOptions = (capabilities?.sources ?? []).map(({ source: id }) => ({
    value: id,
    label:
      id === "Platen"
        ? t("scan.platen", "Flatbed glass")
        : t("scan.feeder", "Document feeder"),
  }));

  return (
    <Modal
      opened={opened}
      onClose={onClose}
      title={t("scan.title", "Scan from a scanner")}
      centered
    >
      <Stack gap="md">
        <TextInput
          label={t("scan.address", "Scanner address")}
          description={t(
            "scan.addressHint",
            "The scanner's IP address on your network, e.g. 192.168.1.50",
          )}
          value={host}
          onChange={(event) => setHost(event.currentTarget.value)}
          disabled={busy !== null}
          data-autofocus
        />
        <Button
          variant="secondary"
          onClick={checkScanner}
          loading={busy === "checking"}
          disabled={!host.trim() || busy === "scanning"}
        >
          {t("scan.check", "Check scanner")}
        </Button>

        {capabilities && (
          <>
            <Select
              label={t("scan.source", "Source")}
              data={sourceOptions}
              value={source}
              onChange={setSource}
              disabled={busy !== null}
              allowDeselect={false}
            />
            <Select
              label={t("scan.resolution", "Resolution")}
              data={resolutionOptions}
              value={resolution}
              onChange={setResolution}
              disabled={busy !== null}
              allowDeselect={false}
            />
            <Select
              label={t("scan.colorMode", "Colour")}
              data={colorOptions}
              value={colorMode}
              onChange={setColorMode}
              disabled={busy !== null}
              allowDeselect={false}
            />
            <Button
              onClick={scan}
              loading={busy === "scanning"}
              disabled={!source || !colorMode}
            >
              {t("scan.start", "Scan")}
            </Button>
          </>
        )}

        {error && (
          <Text c="red" size="sm" role="alert">
            {error}
          </Text>
        )}
      </Stack>
    </Modal>
  );
}
