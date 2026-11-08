import { useCallback, useEffect, useState } from "react";
import { Badge, Group, Stack, Text } from "@mantine/core";
import { Button } from "@app/ui/Button";
import { useTranslation } from "react-i18next";
import { useFileHandler } from "@app/hooks/useFileHandler";
import {
  docuSignErrorMessage,
  docuSignService,
  type DocuSignEnvelopeStatus,
  type DocuSignSentEnvelope,
} from "@app/services/docuSignService";

const STATUS_COLOR: Record<string, string> = {
  completed: "green",
  declined: "red",
  voided: "red",
  sent: "blue",
  delivered: "blue",
};

interface DocuSignSentListProps {
  /** Changes whenever something new was sent, so the list reloads. */
  refreshKey: number;
  connected: boolean;
}

/** Envelopes sent from this installation, with on-demand status and the signed copy. */
const DocuSignSentList = ({ refreshKey, connected }: DocuSignSentListProps) => {
  const { t } = useTranslation();
  const { addFiles } = useFileHandler();
  const [sent, setSent] = useState<DocuSignSentEnvelope[]>([]);
  const [statuses, setStatuses] = useState<
    Record<string, DocuSignEnvelopeStatus>
  >({});
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    docuSignService
      .envelopes()
      .then(setSent)
      .catch(() => setSent([]));
  }, [refreshKey]);

  const check = useCallback(
    async (envelopeId: string) => {
      setBusy(envelopeId);
      setError(null);
      try {
        const status = await docuSignService.envelopeStatus(envelopeId);
        setStatuses((prev) => ({ ...prev, [envelopeId]: status }));
      } catch (e) {
        setError(
          docuSignErrorMessage(
            e,
            t(
              "docuSign.sent.checkFailed",
              "Could not get the status from DocuSign.",
            ),
          ),
        );
      } finally {
        setBusy(null);
      }
    },
    [t],
  );

  const addSignedCopy = useCallback(
    async (envelope: DocuSignSentEnvelope) => {
      setBusy(envelope.envelopeId);
      setError(null);
      try {
        const file = await docuSignService.signedCopy(
          envelope.envelopeId,
          envelope.fileName,
        );
        await addFiles([file]);
      } catch (e) {
        setError(
          docuSignErrorMessage(
            e,
            t(
              "docuSign.sent.downloadFailed",
              "Could not download the signed copy.",
            ),
          ),
        );
      } finally {
        setBusy(null);
      }
    },
    [addFiles, t],
  );

  if (sent.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        {t("docuSign.sent.empty", "Nothing sent yet.")}
      </Text>
    );
  }

  return (
    <Stack gap="sm">
      {sent.map((envelope) => {
        const status = statuses[envelope.envelopeId];
        return (
          <Stack key={envelope.envelopeId} gap={4}>
            <Group justify="space-between" wrap="nowrap">
              <Text size="sm" fw={500} truncate>
                {envelope.fileName}
              </Text>
              {status && (
                <Badge
                  color={STATUS_COLOR[status.status] ?? "gray"}
                  variant="light"
                >
                  {status.status}
                </Badge>
              )}
            </Group>
            <Text size="xs" c="dimmed">
              {new Date(envelope.sentAt).toLocaleString()} ·{" "}
              {envelope.signerEmails.join(", ")}
            </Text>
            {status?.signers.map((signer) => (
              <Text key={signer.email} size="xs">
                {signer.name}: {signer.status}
              </Text>
            ))}
            <Group gap="xs">
              <Button
                size="sm"
                variant="secondary"
                disabled={!connected || busy !== null}
                loading={busy === envelope.envelopeId}
                onClick={() => check(envelope.envelopeId)}
              >
                {t("docuSign.sent.check", "Check status")}
              </Button>
              {status?.status === "completed" && (
                <Button
                  size="sm"
                  disabled={busy !== null}
                  onClick={() => addSignedCopy(envelope)}
                >
                  {t("docuSign.sent.addSigned", "Add signed copy")}
                </Button>
              )}
            </Group>
          </Stack>
        );
      })}
      {error && (
        <Text size="xs" c="red">
          {error}
        </Text>
      )}
    </Stack>
  );
};

export default DocuSignSentList;
