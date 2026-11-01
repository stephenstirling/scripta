import { useEffect, useRef, useState } from "react";
import { Stack, Text } from "@mantine/core";
import { Button } from "@app/ui/Button";
import { useTranslation } from "react-i18next";
import {
  createToolSteps,
  ToolStepProvider,
} from "@app/components/tools/shared/ToolStep";
import { useViewScopedFiles } from "@app/hooks/tools/shared/useViewScopedFiles";
import DocuSignConnectionPanel from "@app/components/tools/docuSign/DocuSignConnectionPanel";
import DocuSignSendSettings, {
  isValidDocuSignSend,
  type DocuSignSendParameters,
} from "@app/components/tools/docuSign/DocuSignSendSettings";
import DocuSignSentList from "@app/components/tools/docuSign/DocuSignSentList";
import { useDocuSignConnection } from "@app/components/tools/docuSign/useDocuSignConnection";
import {
  docuSignErrorMessage,
  docuSignService,
} from "@app/services/docuSignService";
import { BaseToolProps } from "@app/types/tool";

const emptyParameters = (): DocuSignSendParameters => ({
  signers: [{ name: "", email: "" }],
  subject: "",
  message: "",
  sequential: false,
  signaturePage: 0,
});

/**
 * Send the open PDF for signature through the user's own DocuSign account, then follow it and
 * bring the signed copy back. Nothing here changes the document: the signed result arrives as a
 * new file once DocuSign reports the envelope complete.
 */
const DocuSign = ({ onError }: BaseToolProps) => {
  const { t } = useTranslation();
  const selectedFiles = useViewScopedFiles();
  const file = selectedFiles[0];
  const connection = useDocuSignConnection((e, fallback) =>
    docuSignErrorMessage(e, fallback),
  );
  const [parameters, setParameters] =
    useState<DocuSignSendParameters>(emptyParameters);
  const [sending, setSending] = useState(false);
  const [sentNotice, setSentNotice] = useState<string | null>(null);
  const [sentCount, setSentCount] = useState(0);
  const subjectEdited = useRef(false);

  useEffect(() => {
    if (!file || subjectEdited.current) return;
    setParameters((prev) => ({
      ...prev,
      subject: t("docuSign.defaultSubject", "Please sign: {{name}}", {
        name: file.name,
      }),
    }));
  }, [file, t]);

  const connected = connection.status?.connected === true;
  const canSend = connected && !!file && isValidDocuSignSend(parameters);

  const handleSend = async () => {
    if (!file) return;
    setSending(true);
    setSentNotice(null);
    try {
      await docuSignService.send({ file, ...parameters });
      setSentNotice(
        t(
          "docuSign.sentNotice",
          "Sent. DocuSign is emailing {{count}} signer(s).",
          {
            count: parameters.signers.length,
          },
        ),
      );
      setSentCount((n) => n + 1);
    } catch (error) {
      onError?.(
        docuSignErrorMessage(
          error,
          t("docuSign.error.failed", "DocuSign could not send the document."),
        ),
      );
    } finally {
      setSending(false);
    }
  };

  const steps = createToolSteps();

  return (
    <Stack gap="sm" p="sm">
      <ToolStepProvider>
        {steps.create(
          t("docuSign.steps.connection", "DocuSign account"),
          { isVisible: true },
          <DocuSignConnectionPanel connection={connection} />,
        )}
        {steps.create(
          t("docuSign.steps.send", "Send for signature"),
          { isVisible: connected },
          <Stack gap="sm">
            {!file && (
              <Text size="sm" c="dimmed">
                {t("docuSign.noFile", "Open a PDF to send it for signature.")}
              </Text>
            )}
            <DocuSignSendSettings
              parameters={parameters}
              onChange={(next) => {
                if (next.subject !== parameters.subject) {
                  subjectEdited.current = true;
                }
                setParameters(next);
              }}
              disabled={sending}
            />
            <Button
              onClick={handleSend}
              loading={sending}
              disabled={!canSend}
              fullWidth
            >
              {t("docuSign.submit", "Send with DocuSign")}
            </Button>
            {sentNotice && (
              <Text size="sm" c="green">
                {sentNotice}
              </Text>
            )}
          </Stack>,
        )}
        {steps.create(
          t("docuSign.steps.sent", "Sent documents"),
          { isVisible: connected },
          <DocuSignSentList refreshKey={sentCount} connected={connected} />,
        )}
      </ToolStepProvider>
    </Stack>
  );
};

export default DocuSign;
