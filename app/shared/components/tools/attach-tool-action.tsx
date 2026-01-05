import { Button } from "@/components/Button"
import { Icon } from "@/components/Icon"
import { useAgentId } from "@/hooks/use-agentId-param"
import { useAttachToolToAgent } from "@/hooks/use-letta-tools"
import { Letta } from "@letta-ai/letta-client"

export const AttachToolAction = ({
  tool,
  onSuccess,
}: {
  tool: Letta.Tool
  onSuccess?: () => void
}) => {
  const [agentId] = useAgentId()
  const { mutate: attachTool, isPending } = useAttachToolToAgent({
    onSuccess,
  })
  return (
    <Button
      preset="icon"
      onPress={() => attachTool({ agentId, toolId: tool.id! })}
      loading={isPending}
      RightAccessory={() => <Icon icon="Plus" />}
    />
  )
}
