import { useMemo } from "react"
import { colors } from "@/theme"
import { ContextData, ContextSegment, ContextSegmentType } from "./types"

export function useContextSegments(contextData: ContextData | undefined) {
  const { sum, segments } = useMemo(() => {
    if (!contextData) return { sum: 0, segments: [] }

    const maxContext = contextData.contextWindowSizeMax
    const systemTokens = contextData.numTokensSystem || 0
    const toolsTokens = contextData.numTokensFunctionsDefinitions || 0
    const summaryTokens = contextData.numTokensExternalMemorySummary || 0
    const messageTokens = contextData.numTokensMessages || 0

    const sum = systemTokens + toolsTokens + summaryTokens + messageTokens
    const unusedTokens = Math.max(0, maxContext - sum)

    const segments: ContextSegment[] = [
      {
        type: "system" as ContextSegmentType,
        tokens: systemTokens,
        color: colors.palette.accent300, // blue
      },
      {
        type: "tools" as ContextSegmentType,
        tokens: toolsTokens,
        color: colors.palette.accent100, // sky blue
      },
      {
        type: "summary" as ContextSegmentType,
        tokens: summaryTokens,
        color: colors.palette.neutral300, // light green
      },
      {
        type: "messages" as ContextSegmentType,
        tokens: messageTokens,
        color: colors.palette.angry500, // orange/red
      },
      {
        type: "unused" as ContextSegmentType,
        tokens: unusedTokens,
        color: colors.palette.neutral600, // dark gray
      },
    ].filter((segment) => segment.tokens > 0)

    return { sum, segments }
  }, [contextData])

  return { sum, segments }
}
