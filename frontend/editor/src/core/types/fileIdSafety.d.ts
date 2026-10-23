/**
 * Type safety declarations to prevent file.name/UUID confusion
 */

import { FileId, ScriptaFile } from "@app/types/fileContext";

declare global {
  namespace FileIdSafety {
    // Mark functions that should never accept file.name as parameters
    type SafeFileIdFunction<T extends (...args: never[]) => unknown> =
      T extends (...args: infer P) => infer _R
        ? P extends readonly [string, ...unknown[]]
          ? never // Reject string parameters in first position for FileId functions
          : T
        : T;

    // Mark functions that should only accept ScriptaFile, not regular File
    type ScriptaFileOnlyFunction<T extends (...args: never[]) => unknown> =
      T extends (...args: infer P) => infer _R
        ? P extends readonly [File, ...unknown[]]
          ? never // Reject File parameters in first position for ScriptaFile functions
          : T
        : T;

    // Utility type to enforce ScriptaFile usage
    type RequireScriptaFile<T> = T extends File ? ScriptaFile : T;
  }

  // Extend Window interface for debugging
  interface Window {
    __FILE_ID_DEBUG?: boolean;
  }
}

// Augment FileContext types to prevent bypassing ScriptaFile
declare module "../contexts/FileContext" {
  export interface StrictFileContextActions {
    pinFile: (file: ScriptaFile) => void; // Must be ScriptaFile
    unpinFile: (file: ScriptaFile) => void; // Must be ScriptaFile
    addFiles: (
      files: File[],
      options?: { insertAfterPageId?: string },
    ) => Promise<ScriptaFile[]>; // Returns ScriptaFile
    consumeFiles: (
      inputFileIds: FileId[],
      outputFiles: File[],
    ) => Promise<ScriptaFile[]>; // Returns ScriptaFile
  }

  export interface StrictFileContextSelectors {
    getFile: (id: FileId) => ScriptaFile | undefined; // Returns ScriptaFile
    getFiles: (ids?: FileId[]) => ScriptaFile[]; // Returns ScriptaFile[]
    isFilePinned: (file: ScriptaFile) => boolean; // Must be ScriptaFile
  }
}

export {};
