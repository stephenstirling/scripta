import {
  Checkbox,
  Group,
  NumberInput,
  Stack,
  Text,
  Textarea,
  TextInput,
} from "@mantine/core";
import { useTranslation } from "react-i18next";
import { Button } from "@app/ui/Button";
import { Icon } from "@app/ui/Icon";
import type { DocuSignSigner } from "@app/services/docuSignService";

export const DOCUSIGN_MAX_SIGNERS = 10;

export interface DocuSignSendParameters {
  signers: DocuSignSigner[];
  subject: string;
  message: string;
  sequential: boolean;
  /** 1-based; 0 means the last page. */
  signaturePage: number;
}

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export function isValidDocuSignSend(
  parameters: DocuSignSendParameters,
): boolean {
  return (
    parameters.subject.trim().length > 0 &&
    parameters.signers.length > 0 &&
    parameters.signers.every(
      (s) => s.name.trim().length > 0 && EMAIL.test(s.email.trim()),
    )
  );
}

interface DocuSignSendSettingsProps {
  parameters: DocuSignSendParameters;
  onChange: (next: DocuSignSendParameters) => void;
  disabled?: boolean;
}

/** Who signs, the email they receive, and where the signature boxes go. */
const DocuSignSendSettings = ({
  parameters,
  onChange,
  disabled,
}: DocuSignSendSettingsProps) => {
  const { t } = useTranslation();

  const setSigner = (index: number, patch: Partial<DocuSignSigner>) =>
    onChange({
      ...parameters,
      signers: parameters.signers.map((s, i) =>
        i === index ? { ...s, ...patch } : s,
      ),
    });

  return (
    <Stack gap="sm">
      {parameters.signers.map((signer, index) => (
        <Group key={index} gap="xs" align="flex-end" wrap="nowrap">
          <TextInput
            style={{ flex: 1 }}
            size="xs"
            label={index === 0 ? t("docuSign.signer.name", "Name") : undefined}
            aria-label={t("docuSign.signer.name", "Name")}
            value={signer.name}
            onChange={(e) => setSigner(index, { name: e.currentTarget.value })}
            disabled={disabled}
          />
          <TextInput
            style={{ flex: 1.4 }}
            size="xs"
            type="email"
            label={
              index === 0 ? t("docuSign.signer.email", "Email") : undefined
            }
            aria-label={t("docuSign.signer.email", "Email")}
            value={signer.email}
            onChange={(e) => setSigner(index, { email: e.currentTarget.value })}
            disabled={disabled}
          />
          <Button
            variant="tertiary"
            size="sm"
            leftSection={<Icon name="x" size="1rem" />}
            aria-label={t("docuSign.signer.remove", "Remove signer")}
            disabled={disabled || parameters.signers.length === 1}
            onClick={() =>
              onChange({
                ...parameters,
                signers: parameters.signers.filter((_, i) => i !== index),
              })
            }
          />
        </Group>
      ))}
      <Group>
        <Button
          variant="secondary"
          size="sm"
          disabled={
            disabled || parameters.signers.length >= DOCUSIGN_MAX_SIGNERS
          }
          onClick={() =>
            onChange({
              ...parameters,
              signers: [...parameters.signers, { name: "", email: "" }],
            })
          }
        >
          {t("docuSign.signer.add", "Add signer")}
        </Button>
      </Group>
      {parameters.signers.length > 1 && (
        <Checkbox
          size="xs"
          label={t(
            "docuSign.sequential",
            "Signers sign one after another, in this order",
          )}
          checked={parameters.sequential}
          onChange={(e) =>
            onChange({ ...parameters, sequential: e.currentTarget.checked })
          }
          disabled={disabled}
        />
      )}
      <TextInput
        size="xs"
        label={t("docuSign.subject", "Email subject")}
        value={parameters.subject}
        onChange={(e) =>
          onChange({ ...parameters, subject: e.currentTarget.value })
        }
        disabled={disabled}
      />
      <Textarea
        size="xs"
        label={t("docuSign.message", "Message to signers")}
        autosize
        minRows={2}
        value={parameters.message}
        onChange={(e) =>
          onChange({ ...parameters, message: e.currentTarget.value })
        }
        disabled={disabled}
      />
      <NumberInput
        size="xs"
        label={t("docuSign.signaturePage", "Signature page")}
        description={t(
          "docuSign.signaturePageHint",
          "0 puts the signature boxes on the last page.",
        )}
        min={0}
        allowDecimal={false}
        value={parameters.signaturePage}
        onChange={(value) =>
          onChange({
            ...parameters,
            signaturePage: typeof value === "number" ? value : 0,
          })
        }
        disabled={disabled}
      />
      <Text size="xs" c="dimmed">
        {t(
          "docuSign.placementHint",
          "Each signer gets a signature box and a date box at the bottom of that page.",
        )}
      </Text>
    </Stack>
  );
};

export default DocuSignSendSettings;
