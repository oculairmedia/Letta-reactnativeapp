export type ContextSegmentType = "system" | "tools" | "summary" | "messages" | "unused"

export interface ContextSegment {
  type: ContextSegmentType
  tokens: number
  color: string
}

export interface ContextData {
  contextWindowSizeMax: number
  contextWindowSizeCurrent: number
  numTokensSystem: number
  numTokensFunctionsDefinitions: number
  numTokensExternalMemorySummary: number
  numTokensMessages: number
}
