import { Alert } from "react-native"
import slugify from "slugify"

export const foramtToSlug = (text: string) => {
  return slugify(text, { lower: true, strict: true, remove: /[*+~.()'"!:@]/g, trim: true })
}
interface AgentNamePromptOptions {
  defaultName?: string
  onSubmit: (name: string) => void
}

/**
 * Shows a prompt for entering an agent name with validation
 * @param options Configuration options for the prompt
 */
export function showAgentNamePrompt(options: AgentNamePromptOptions) {
  const { defaultName = "New Agent", onSubmit } = options

  Alert.prompt(
    "Agent name",
    "Enter a name for your agent",
    (text) => {
      text = text.trim() || defaultName
      onSubmit(text)
    },
    "plain-text",
    defaultName,
  )
}
