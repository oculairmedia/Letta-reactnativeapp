import { Card, Icon, Screen, Text } from "@/components"
import { useLettaHeader } from "@/components/custom/useLettaHeader"
import { useAgents } from "@/hooks/use-agents"
import { useAllConversations } from "@/hooks/use-all-conversations"
import { useCreateConversation } from "@/hooks/use-conversations"
import { AppStackScreenProps, navigate } from "@/navigators"
import { useAgentStore } from "@/providers/AgentProvider"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import { Conversation } from "@letta-ai/letta-client/resources/conversations/conversations"
import { FC, useMemo } from "react"
import {
  FlatList,
  RefreshControl,
  TextStyle,
  TouchableOpacity,
  View,
  ViewStyle,
} from "react-native"

const formatRelativeTime = (dateString: string | null | undefined): string => {
  if (!dateString) return ""
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return "now"
  if (diffMins < 60) return `${diffMins}m ago`
  if (diffHours < 24) return `${diffHours}h ago`
  if (diffDays < 7) return `${diffDays}d ago`
  return new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric" }).format(date)
}

interface ConversationCardProps {
  conversation: Conversation
  agentName: string
  onPress: () => void
}

const ConversationCard: FC<ConversationCardProps> = ({ conversation, agentName, onPress }) => {
  const {
    theme: { colors },
    themed,
  } = useAppTheme()

  const displayTitle = conversation.summary || `Chat with ${agentName}`
  const timeAgo = formatRelativeTime(conversation.last_message_at || conversation.created_at)

  return (
    <Card
      onPress={onPress}
      style={$conversationCard}
      HeadingComponent={
        <View style={$cardHeader}>
          <Text preset="bold" numberOfLines={1} style={$cardTitle}>
            {displayTitle}
          </Text>
          <Text size="xxs" style={themed($timeText)}>
            {timeAgo}
          </Text>
        </View>
      }
      ContentComponent={
        <View style={$cardContent}>
          <Icon icon="Bot" size={14} color={colors.textDim} />
          <Text size="xs" style={themed($agentText)} numberOfLines={1}>
            {agentName}
          </Text>
        </View>
      }
      RightComponent={
        <Icon icon="caretRight" size={16} color={colors.elementColors.card.default.content} />
      }
    />
  )
}

export const ConversationsScreen: FC<AppStackScreenProps<"Conversations">> = () => {
  useLettaHeader()

  const {
    theme: { colors },
    themed,
  } = useAppTheme()

  const { data: conversations, refetch, isFetching } = useAllConversations()
  const { data: agents } = useAgents()
  const { mutate: createConversation } = useCreateConversation()

  const setAgentId = useAgentStore((s) => s.setAgentId)
  const setConversationId = useAgentStore((s) => s.setConversationId)

  // Create a map of agent IDs to agent names
  const agentMap = useMemo(() => {
    const map: Record<string, string> = {}
    if (agents) {
      for (const agent of agents) {
        map[agent.id] = agent.name || "Unnamed Agent"
      }
    }
    return map
  }, [agents])

  const handleConversationPress = (conversation: Conversation) => {
    setAgentId(conversation.agent_id)
    setConversationId(conversation.id)
    navigate("AgentDrawer", { screen: "AgentTab" })
  }

  const handleNewChat = () => {
    // Navigate to agent list to select an agent for new chat
    navigate("AgentList")
  }

  return (
    <Screen style={$root} preset="fixed" contentContainerStyle={$contentContainer}>
      <View style={$header}>
        <TouchableOpacity style={themed($agentsButton)} onPress={() => navigate("AgentList")}>
          <Icon icon="Bot" size={18} color={colors.textDim} />
          <Text size="sm" style={themed($agentsButtonText)}>
            Agents
          </Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={conversations || []}
        bounces={!!conversations?.length}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={isFetching} onRefresh={refetch} />}
        refreshing={isFetching}
        ItemSeparatorComponent={() => <View style={{ height: spacing.xs }} />}
        renderItem={({ item }) => (
          <ConversationCard
            conversation={item}
            agentName={agentMap[item.agent_id] || "Unknown Agent"}
            onPress={() => handleConversationPress(item)}
          />
        )}
        contentContainerStyle={{ padding: spacing.sm }}
        ListEmptyComponent={
          <View style={$emptyState}>
            <Icon icon="MessageSquare" size={48} color={colors.textDim} />
            <Text style={themed($emptyText)}>No conversations yet</Text>
            <Text size="sm" style={themed($emptySubtext)}>
              Start a chat with an agent to begin
            </Text>
          </View>
        }
      />

      <TouchableOpacity
        style={themed($fab)}
        onPress={handleNewChat}
        activeOpacity={0.8}
      >
        <Icon icon="Plus" size={24} color="#fff" />
      </TouchableOpacity>
    </Screen>
  )
}

const $root: ViewStyle = {
  flex: 1,
}

const $contentContainer: ViewStyle = {
  flex: 1,
  paddingBottom: spacing.lg,
}

const $header: ViewStyle = {
  flexDirection: "row",
  justifyContent: "flex-end",
  padding: spacing.sm,
  gap: spacing.sm,
}

const $agentsButton: ThemedStyle<ViewStyle> = ({ colors }) => ({
  flexDirection: "row",
  alignItems: "center",
  gap: spacing.xs,
  paddingHorizontal: spacing.sm,
  paddingVertical: spacing.xs,
  backgroundColor: colors.palette.overlay20,
  borderRadius: 8,
})

const $agentsButtonText: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
})

const $conversationCard: ViewStyle = {
  minHeight: 0,
  paddingVertical: spacing.xs,
}

const $cardHeader: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
  gap: spacing.sm,
}

const $cardTitle: TextStyle = {
  flex: 1,
}

const $timeText: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
})

const $cardContent: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  gap: spacing.xxs,
  marginTop: spacing.xxs,
}

const $agentText: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
})

const $emptyState: ViewStyle = {
  alignItems: "center",
  justifyContent: "center",
  paddingVertical: spacing.xxl,
  gap: spacing.sm,
}

const $emptyText: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
})

const $emptySubtext: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
  opacity: 0.7,
})

const $fab: ThemedStyle<ViewStyle> = ({ colors }) => ({
  position: "absolute",
  bottom: spacing.lg,
  right: spacing.md,
  width: 56,
  height: 56,
  borderRadius: 28,
  backgroundColor: colors.tint,
  alignItems: "center",
  justifyContent: "center",
  elevation: 4,
  shadowColor: "#000",
  shadowOffset: { width: 0, height: 2 },
  shadowOpacity: 0.25,
  shadowRadius: 4,
})
