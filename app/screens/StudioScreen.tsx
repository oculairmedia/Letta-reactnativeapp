import { Screen } from "@/components"
import { StudioAgentForm } from "@/components/custom/forms/studio-agent-form"
import { useLettaHeader } from "@/components/custom/useLettaHeader"
import { useCreateAgent } from "@/hooks/use-create-agent"
import { AppStackScreenProps, navigate } from "@/navigators"
import { agentStore } from "@/providers/AgentProvider"
import { spacing } from "@/theme"
import { Letta } from "@letta-ai/letta-client"
import { observer } from "mobx-react-lite"
import { FC } from "react"
import { View, ViewStyle } from "react-native"
import { useSafeAreaInsets } from "react-native-safe-area-context"
interface StudioScreenProps extends AppStackScreenProps<"Studio"> {}

const chatWithAgent = (agentId: string) => {
  agentStore.setAgentId(agentId)
  navigate("AgentDrawer", { screen: "AgentTab" })
}

export const StudioScreen: FC<StudioScreenProps> = observer(function StudioScreen() {
  useLettaHeader()
  const { mutate: createAgent, isPending: isCreatingAgent } = useCreateAgent({
    onSuccess: (data) => {
      chatWithAgent(data.id)
    },
  })

  const handleSubmit = (agentData: Letta.AgentCreateParams) => {
    createAgent(agentData)
  }

  const { bottom } = useSafeAreaInsets()

  return (
    <Screen style={[$root, { marginBottom: bottom + spacing.md }]} preset="scroll">
      <View style={$content}>
        <StudioAgentForm onSubmit={handleSubmit} isPending={isCreatingAgent} />
      </View>
    </Screen>
  )
})

const $root: ViewStyle = {
  flex: 1,
  padding: spacing.md,
}

const $content: ViewStyle = {
  flex: 1,
  flexDirection: "column",
  gap: spacing.xs,
}
