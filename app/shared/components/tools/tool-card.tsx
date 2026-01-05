import { Card, CardProps } from "@/components"
import { normalizeToolName } from "@/shared/utils/normalizers"
import { Letta } from "@letta-ai/letta-client"
import { ViewStyle } from "react-native"

interface ToolCardProps extends CardProps {
  tool: Letta.Tool
}
export const ToolCard = ({ tool, ...props }: ToolCardProps) => {
  return (
    <Card
      key={tool.id}
      heading={normalizeToolName(tool.name ?? undefined)}
      content={tool.description?.trim()}
      ContentTextProps={{ numberOfLines: 2 }}
      style={$toolCard}
      {...props}
    />
  )
}

const $toolCard: ViewStyle = {
  marginBottom: 0,
  minHeight: 0,
}
