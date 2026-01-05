import { Button } from "@/components/Button"
import { SourcesModal } from "@/components/custom/modals/sources-modal"
import { Icon } from "@/components/Icon"
import { useGetAgentSources } from "@/hooks/use-get-sources"
import { spacing } from "@/theme"
import { FC, useState } from "react"
import { View, ViewStyle } from "react-native"

interface AgentSourcesProps {
  agentId: string
}

export const AgentSources: FC<AgentSourcesProps> = ({ agentId }) => {
  const { data: agentSources } = useGetAgentSources(agentId)
  const [showModal, setShowModal] = useState(false)

  return (
    <View style={$container}>
      <Button
        onPress={() => setShowModal(true)}
        text={`${agentSources?.length} attached sources`}
        RightAccessory={() => <Icon icon="Plus" size={20} />}
      />
      <SourcesModal visible={showModal} onDismiss={() => setShowModal(false)} />
    </View>
  )
}

const $container: ViewStyle = {
  gap: spacing.xs,
}
