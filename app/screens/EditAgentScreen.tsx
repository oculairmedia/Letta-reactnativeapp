import { Screen } from "@/components"
import { EditAgentForm } from "@/components/custom/forms/edit-agent-form"
import { useLettaHeader } from "@/components/custom/useLettaHeader"
import { useEditAgent } from "@/hooks/use-edit-agent"
import { AppStackScreenProps, navigate } from "@/navigators"
import { spacing } from "@/theme"
import { Letta } from "@letta-ai/letta-client"
import { observer } from "mobx-react-lite"
import { FC } from "react"
import { View, ViewStyle } from "react-native"
import { useSafeAreaInsets } from "react-native-safe-area-context"

interface EditAgentScreenProps extends AppStackScreenProps<"EditAgent"> {}

export const EditAgentScreen: FC<EditAgentScreenProps> = observer(function EditAgentScreen({
  route,
}) {
  useLettaHeader()
  const { mutate: editAgent, isPending: isEditingAgent } = useEditAgent({
    onSuccess: () => {
      navigate("AgentList")
    },
  })

  const handleSubmit = (agentData: Letta.AgentUpdateParams & { id: string }) => {
    editAgent(agentData)
  }

  const { bottom } = useSafeAreaInsets()

  return (
    <Screen style={[$root, { marginBottom: bottom + spacing.md }]} preset="scroll">
      <View style={$content}>
        <EditAgentForm
          onSubmit={handleSubmit}
          isPending={isEditingAgent}
          agentId={route.params.agentId}
        />
      </View>
    </Screen>
  )
})

const $root: ViewStyle = {
  flex: 1,
}

const $content: ViewStyle = {
  padding: spacing.md,
}
