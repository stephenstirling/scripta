import {
  ScriptaFile,
  FileId,
  ScriptaFileStub,
  createScriptaFile,
  ProcessedFileMetadata,
  createNewScriptaFileStub,
} from "@app/types/fileContext";

/**
 * Builds parallel inputFileIds and inputScriptaFileStubs arrays from the valid input files.
 * Falls back to a fresh stub when the file is not found in the current context state
 * (e.g. it was removed between operation start and this point).
 */
export function buildInputTracking(
  validFiles: ScriptaFile[],
  selectors: {
    getScriptaFileStub: (id: FileId) => ScriptaFileStub | undefined;
  },
): { inputFileIds: FileId[]; inputScriptaFileStubs: ScriptaFileStub[] } {
  const inputFileIds: FileId[] = [];
  const inputScriptaFileStubs: ScriptaFileStub[] = [];
  for (const file of validFiles) {
    const fileId = file.fileId;
    const record = selectors.getScriptaFileStub(fileId);
    if (record) {
      inputFileIds.push(fileId);
      inputScriptaFileStubs.push(record);
    } else {
      console.debug(`No file stub found for file: ${file.name}`);
      inputFileIds.push(fileId);
      inputScriptaFileStubs.push(createNewScriptaFileStub(file, fileId));
    }
  }
  return { inputFileIds, inputScriptaFileStubs };
}

/**
 * Creates parallel outputScriptaFileStubs and outputScriptaFiles arrays from processed files.
 * The stubFactory determines how each stub is constructed (child version vs fresh root).
 */
export function buildOutputPairs(
  processedFiles: File[],
  thumbnails: string[],
  metadataArray: Array<ProcessedFileMetadata | undefined>,
  stubFactory: (
    file: File,
    thumbnail: string,
    metadata: ProcessedFileMetadata | undefined,
    index: number,
  ) => ScriptaFileStub,
): {
  outputScriptaFileStubs: ScriptaFileStub[];
  outputScriptaFiles: ScriptaFile[];
} {
  const outputScriptaFileStubs = processedFiles.map((file, index) =>
    stubFactory(file, thumbnails[index], metadataArray[index], index),
  );
  const outputScriptaFiles = processedFiles.map((file, index) =>
    createScriptaFile(file, outputScriptaFileStubs[index].id),
  );
  return { outputScriptaFileStubs, outputScriptaFiles };
}
