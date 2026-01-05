import { memo } from "react"

import CodeEditor, {
  CodeEditorStyleType,
  CodeEditorSyntaxStyles,
} from "@rivascva/react-native-code-editor"

export const MemoizedCodeEditor = memo(function CodeEditorComponent({
  content,
}: {
  content: string
}) {
  return (
    <CodeEditor
      language="json"
      initialValue={content}
      readOnly
      syntaxStyle={CodeEditorSyntaxStyles.atomOneDark}
      showLineNumbers={false}
      style={$codeEditor}
    />
  )
})

const $codeEditor: CodeEditorStyleType = {}
