/**
 * Test utilities for creating ScriptaFile objects in tests
 */

import { ScriptaFile, createScriptaFile } from "@app/types/fileContext";

/**
 * Create a ScriptaFile object for testing purposes
 */
export function createTestScriptaFile(
  name: string,
  content: string = "test content",
  type: string = "application/pdf",
): ScriptaFile {
  const file = new File([content], name, { type });
  return createScriptaFile(file);
}

/**
 * Create multiple ScriptaFile objects for testing
 */
export function createTestFilesWithId(
  files: Array<{ name: string; content?: string; type?: string }>,
): ScriptaFile[] {
  return files.map(
    ({ name, content = "test content", type = "application/pdf" }) =>
      createTestScriptaFile(name, content, type),
  );
}
