import { useCallback, useEffect, useRef, useState } from "react";
import {
  docuSignService,
  type DocuSignConnectInfo,
  type DocuSignEnvironment,
  type DocuSignStatus,
} from "@app/services/docuSignService";

const SIGN_IN_POLL_MS = 2000;
const SIGN_IN_POLL_LIMIT = 90;

export interface DocuSignConnection {
  status: DocuSignStatus | null;
  /** Set while waiting for the DocuSign window to finish the sign-in. */
  waitingForSignIn: boolean;
  /** Redirect URI and same-device flag from the last Connect click. */
  connectInfo: DocuSignConnectInfo | null;
  error: string | null;
  save: (key: string, environment: DocuSignEnvironment) => Promise<void>;
  connect: () => Promise<void>;
  disconnect: () => Promise<void>;
}

/** Loads the connection state and drives the browser sign-in, polling until DocuSign reports it done. */
export function useDocuSignConnection(
  errorText: (error: unknown, fallback: string) => string,
): DocuSignConnection {
  const [status, setStatus] = useState<DocuSignStatus | null>(null);
  const [waitingForSignIn, setWaitingForSignIn] = useState(false);
  const [connectInfo, setConnectInfo] = useState<DocuSignConnectInfo | null>(
    null,
  );
  const [error, setError] = useState<string | null>(null);
  const pollTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const errorTextRef = useRef(errorText);
  errorTextRef.current = errorText;

  const stopPolling = useCallback(() => {
    if (pollTimer.current) {
      clearTimeout(pollTimer.current);
      pollTimer.current = null;
    }
    setWaitingForSignIn(false);
  }, []);

  useEffect(() => {
    docuSignService
      .status()
      .then(setStatus)
      .catch((e) => setError(errorTextRef.current(e, "")));
    return stopPolling;
  }, [stopPolling]);

  const pollUntilConnected = useCallback(
    (attempt: number) => {
      pollTimer.current = setTimeout(async () => {
        try {
          const next = await docuSignService.status();
          setStatus(next);
          if (next.connected) {
            stopPolling();
            return;
          }
        } catch {
          // A missed poll is harmless; the next one retries.
        }
        if (attempt >= SIGN_IN_POLL_LIMIT) {
          stopPolling();
        } else {
          pollUntilConnected(attempt + 1);
        }
      }, SIGN_IN_POLL_MS);
    },
    [stopPolling],
  );

  const save = useCallback(
    async (key: string, environment: DocuSignEnvironment) => {
      setError(null);
      try {
        setStatus(await docuSignService.configure(key, environment));
      } catch (e) {
        setError(errorTextRef.current(e, ""));
      }
    },
    [],
  );

  const connect = useCallback(async () => {
    setError(null);
    try {
      const info = await docuSignService.connect();
      setConnectInfo(info);
      window.open(info.url, "_blank", "noopener");
      setWaitingForSignIn(true);
      pollUntilConnected(0);
    } catch (e) {
      setError(errorTextRef.current(e, ""));
    }
  }, [pollUntilConnected]);

  const disconnect = useCallback(async () => {
    setError(null);
    stopPolling();
    try {
      setStatus(await docuSignService.disconnect());
    } catch (e) {
      setError(errorTextRef.current(e, ""));
    }
  }, [stopPolling]);

  return {
    status,
    waitingForSignIn,
    connectInfo,
    error,
    save,
    connect,
    disconnect,
  };
}
