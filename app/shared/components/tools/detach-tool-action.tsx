import { Button } from "@/components/Button"
import { Icon } from "@/components/Icon"
import { useAgentId } from "@/hooks/use-agentId-param"
import { useDetachToolFromAgent } from "@/hooks/use-letta-tools"
import { colors } from "@/theme"
import { Letta } from "@letta-ai/letta-client"
import { Alert } from "react-native"

export const DetachToolAction = ({ tool }: { tool: Letta.Tool }) => {
  const [agentId] = useAgentId()
  const { mutate: removeTool, isPending } = useDetachToolFromAgent()
  return (
    <Button
      preset="icon"
      onPress={() => {
        Alert.alert("Are you sure?", "This will remove the tool from the agent.", [
          { text: "Cancel", style: "cancel" },
          {
            text: "Remove",
            style: "destructive",
            onPress: () => removeTool({ agentId, toolId: tool.id! }),
          },
        ])
      }}
      loading={isPending}
      RightAccessory={() => <Icon icon="Trash" color={colors.error} />}
    />
  )
}
