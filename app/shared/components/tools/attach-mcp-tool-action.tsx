import { Button } from "@/components/Button"
import { Icon } from "@/components/Icon"
import { useAgentId } from "@/hooks/use-agentId-param"
import { useAttachToolToAgent } from "@/hooks/use-letta-tools"
import { useFetchMCPToolByMCPServer } from "@/hooks/use-mcp"
import { Tool } from "@letta-ai/letta-client/resources/tools"

export const AttachMCPToolAction = ({
  tool,
  onSuccess,
}: {
  tool: { serverName: string; name: string }
  onSuccess?: () => void
}) => {
  const [agentId] = useAgentId()
  const { mutate: attachTool, isPending: isAttachingTool } = useAttachToolToAgent({
    onSuccess,
  })
  const { mutate: addMCPTool, isPending: isAddingMCPTool } = useFetchMCPToolByMCPServer()
  return (
    <Button
      preset="icon"
      onPress={() =>
        addMCPTool([tool.serverName, tool.name], {
          onSuccess: (tool: Tool) => {
            attachTool({ agentId, toolId: tool.id! })
          },
        })
      }
      loading={isAttachingTool || isAddingMCPTool}
      RightAccessory={() => <Icon icon="Plus" />}
    />
  )
}
