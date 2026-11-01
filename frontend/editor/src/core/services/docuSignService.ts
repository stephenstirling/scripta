import apiClient from "@app/services/apiClient";

export type DocuSignEnvironment = "demo" | "production";

export interface DocuSignStatus {
  configured: boolean;
  connected: boolean;
  environment: DocuSignEnvironment;
  integrationKey: string;
  accountName: string | null;
  userName: string | null;
  email: string | null;
}

export interface DocuSignConnectInfo {
  url: string;
  /** Must be registered on the integration key in DocuSign. */
  redirectUri: string;
  /** False when Scripta is open on a different device than the one running it. */
  sameMachine: boolean;
}

export interface DocuSignSigner {
  name: string;
  email: string;
}

export interface DocuSignSentEnvelope {
  envelopeId: string;
  fileName: string;
  subject: string;
  sentAt: string;
  signerEmails: string[];
}

export interface DocuSignRecipientStatus {
  name: string;
  email: string;
  status: string;
}

export interface DocuSignEnvelopeStatus {
  envelopeId: string;
  status: string;
  sentAt: string;
  completedAt: string;
  signers: DocuSignRecipientStatus[];
}

export interface DocuSignSendOptions {
  file: File;
  subject: string;
  message: string;
  signers: DocuSignSigner[];
  sequential: boolean;
  /** 1-based; 0 puts the signature boxes on the last page. */
  signaturePage: number;
}

const BASE = "/api/v1/docusign";
const QUIET = { suppressErrorToast: true } as const;

/** The server's explanation for a failed call, or a generic fallback. */
export function docuSignErrorMessage(error: unknown, fallback: string): string {
  const message = (
    error as { response?: { data?: { message?: unknown } } } | undefined
  )?.response?.data?.message;
  return typeof message === "string" && message ? message : fallback;
}

export const docuSignService = {
  async status(): Promise<DocuSignStatus> {
    return (await apiClient.get<DocuSignStatus>(`${BASE}/status`, QUIET)).data;
  },

  async configure(
    integrationKey: string,
    environment: DocuSignEnvironment,
  ): Promise<DocuSignStatus> {
    return (
      await apiClient.put<DocuSignStatus>(
        `${BASE}/config`,
        { integrationKey, environment },
        QUIET,
      )
    ).data;
  },

  async connect(): Promise<DocuSignConnectInfo> {
    return (await apiClient.get<DocuSignConnectInfo>(`${BASE}/connect`, QUIET))
      .data;
  },

  async disconnect(): Promise<DocuSignStatus> {
    return (
      await apiClient.post<DocuSignStatus>(`${BASE}/disconnect`, null, QUIET)
    ).data;
  },

  async send(options: DocuSignSendOptions): Promise<DocuSignSentEnvelope> {
    const form = new FormData();
    form.append("fileInput", options.file);
    form.append("subject", options.subject);
    form.append("message", options.message);
    form.append("signers", JSON.stringify(options.signers));
    form.append("sequential", String(options.sequential));
    form.append("signaturePage", String(options.signaturePage));
    return (
      await apiClient.post<DocuSignSentEnvelope>(`${BASE}/send`, form, QUIET)
    ).data;
  },

  async envelopes(): Promise<DocuSignSentEnvelope[]> {
    return (
      await apiClient.get<DocuSignSentEnvelope[]>(`${BASE}/envelopes`, QUIET)
    ).data;
  },

  async envelopeStatus(envelopeId: string): Promise<DocuSignEnvelopeStatus> {
    return (
      await apiClient.get<DocuSignEnvelopeStatus>(
        `${BASE}/envelopes/${encodeURIComponent(envelopeId)}`,
        QUIET,
      )
    ).data;
  },

  /** The signed PDF with DocuSign's certificate of completion appended. */
  async signedCopy(envelopeId: string, fileName: string): Promise<File> {
    const response = await apiClient.get<Blob>(
      `${BASE}/envelopes/${encodeURIComponent(envelopeId)}/signed`,
      { responseType: "blob", ...QUIET },
    );
    const base = fileName.replace(/\.pdf$/i, "");
    return new File([response.data], `${base}-signed.pdf`, {
      type: "application/pdf",
    });
  },
};
