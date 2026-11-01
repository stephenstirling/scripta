import { useEffect, useState } from "react";
import { Alert, Group, Select, Stack, Text, TextInput } from "@mantine/core";
import { useTranslation } from "react-i18next";
import { Button } from "@app/ui/Button";
import type { DocuSignConnection } from "@app/components/tools/docuSign/useDocuSignConnection";
import type { DocuSignEnvironment } from "@app/services/docuSignService";

/** The redirect URI DocuSign sends the browser back to; plain HTTP is only accepted for localhost. */
function defaultRedirectUri(): string {
  const port = window.location.port ? `:${window.location.port}` : "";
  return `http://localhost${port}/api/v1/docusign/callback`;
}

const DocuSignConnectionPanel = ({
  connection,
}: {
  connection: DocuSignConnection;
}) => {
  const { t } = useTranslation();
  const { status, waitingForSignIn, connectInfo, error } = connection;
  const [key, setKey] = useState("");
  const [environment, setEnvironment] = useState<DocuSignEnvironment>("demo");

  useEffect(() => {
    if (status) {
      setKey(status.integrationKey);
      setEnvironment(status.environment);
    }
  }, [status]);

  if (!status) {
    return (
      <Text size="sm" c="dimmed">
        {t("docuSign.connection.loading", "Checking DocuSign…")}
      </Text>
    );
  }

  const errorAlert = error && (
    <Alert color="red" variant="light">
      {error}
    </Alert>
  );

  if (status.connected) {
    return (
      <Stack gap="xs">
        <Text size="sm">
          {t("docuSign.connection.connectedAs", "Connected as {{who}}", {
            who: status.userName || status.email || "",
          })}
        </Text>
        <Text size="xs" c="dimmed">
          {[status.accountName, status.email].filter(Boolean).join(" · ")}
          {" · "}
          {status.environment === "demo"
            ? t("docuSign.environment.demo", "Developer sandbox")
            : t("docuSign.environment.production", "Production")}
        </Text>
        <Group>
          <Button variant="secondary" size="sm" onClick={connection.disconnect}>
            {t("docuSign.connection.disconnect", "Disconnect")}
          </Button>
        </Group>
        {errorAlert}
      </Stack>
    );
  }

  const redirectUri = connectInfo?.redirectUri ?? defaultRedirectUri();
  const unchanged =
    key.trim() === status.integrationKey && environment === status.environment;

  return (
    <Stack gap="xs">
      <TextInput
        label={t("docuSign.connection.integrationKey", "Integration key")}
        description={t(
          "docuSign.connection.integrationKeyHint",
          "From DocuSign Settings → Apps and Keys. Set the app to use the authorization code grant with PKCE.",
        )}
        value={key}
        onChange={(e) => setKey(e.currentTarget.value)}
        autoComplete="off"
      />
      <Select
        label={t("docuSign.connection.environment", "Account type")}
        value={environment}
        onChange={(value) =>
          value && setEnvironment(value as DocuSignEnvironment)
        }
        data={[
          {
            value: "demo",
            label: t("docuSign.environment.demo", "Developer sandbox"),
          },
          {
            value: "production",
            label: t("docuSign.environment.production", "Production"),
          },
        ]}
        allowDeselect={false}
      />
      <Text size="xs" c="dimmed">
        {t(
          "docuSign.connection.redirectHint",
          "Add this redirect URI to the integration key in DocuSign:",
        )}{" "}
        <code>{redirectUri}</code>
      </Text>
      {connectInfo && !connectInfo.sameMachine && (
        <Alert color="yellow" variant="light">
          {t(
            "docuSign.connection.otherDevice",
            "You are using Scripta from another device. DocuSign redirects to localhost, so sign-in only completes in a browser on the computer running Scripta.",
          )}
        </Alert>
      )}
      <Group>
        <Button
          size="sm"
          variant="secondary"
          disabled={unchanged || !key.trim()}
          onClick={() => connection.save(key.trim(), environment)}
        >
          {t("docuSign.connection.save", "Save")}
        </Button>
        <Button
          size="sm"
          disabled={!status.configured || !unchanged || waitingForSignIn}
          loading={waitingForSignIn}
          onClick={connection.connect}
        >
          {waitingForSignIn
            ? t("docuSign.connection.waiting", "Waiting for DocuSign…")
            : t("docuSign.connection.connect", "Connect DocuSign")}
        </Button>
      </Group>
      {errorAlert}
    </Stack>
  );
};

export default DocuSignConnectionPanel;
