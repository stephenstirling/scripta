import { ScriptaFile, ScriptaFileStub } from "@app/types/fileContext";
import {
  createChildStub,
  generateProcessedFileMetadata,
} from "@app/contexts/file/fileActions";
import { createScriptaFile } from "@app/types/fileContext";
import { ToolId } from "@app/types/toolId";

/**
 * Create ScriptaFiles and ScriptaFileStubs from exported files
 * Used when saving page editor changes to create version history
 */
export async function createScriptaFilesAndStubs(
  files: File[],
  parentStub: ScriptaFileStub,
  toolId: ToolId,
  /** Shown instead of the tool's name in version history (a policy passes its pipeline name). */
  label?: string,
): Promise<{ scriptaFiles: ScriptaFile[]; stubs: ScriptaFileStub[] }> {
  const scriptaFiles: ScriptaFile[] = [];
  const stubs: ScriptaFileStub[] = [];

  for (const file of files) {
    const processedFileMetadata = await generateProcessedFileMetadata(file);
    const childStub = createChildStub(
      parentStub,
      { toolId, timestamp: Date.now(), ...(label ? { label } : {}) },
      file,
      processedFileMetadata?.thumbnailUrl,
      processedFileMetadata,
    );

    const scriptaFile = createScriptaFile(file, childStub.id);
    scriptaFiles.push(scriptaFile);
    stubs.push(childStub);
  }

  return { scriptaFiles, stubs };
}
