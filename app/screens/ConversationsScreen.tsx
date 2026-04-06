import { Card, Icon, Screen, Text } from "@/components"
import { useLettaHeader } from "@/components/custom/useLettaHeader"
import { SimpleContextMenu } from "@/components/simple-context-menu"
import { useAgents } from "@/hooks/use-agents"
import { useAllConversations } from "@/hooks/use-all-conversations"
import {
  useArchiveConversation,
  useCreateConversation,
  useDeleteConversation,
  useForkConversation,
  useUpdateConversation,
} from "@/hooks/use-conversations"
import { AppStackScreenProps, navigate } from "@/navigators"
import { useAgentStore } from "@/providers/AgentProvider"
import { formatRelativeTime } from "@/shared/utils/formatters"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import { Conversation } from "@letta-ai/letta-client/resources/conversations/conversations"
import { Letta } from "@letta-ai/letta-client"
import Fuse from "fuse.js"
import { FC, useMemo, useState } from "react"
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Modal,
  Pressable,
  RefreshControl,
  ScrollView,
  TextInput,
  TextStyle,
  TouchableOpacity,
  View,
  ViewStyle,
} from "react-native"

interface ConversationCardProps {
  conversation: Conversation
  agentName: string
  onPress: () => void
  onDelete: () => void
  onEdit: () => void
  onArchive: () => void
  onFork: () => void
}

const ConversationCard: FC<ConversationCardProps> = ({
  conversation,
  agentName,
  onPress,
  onDelete,
  onEdit,
  onArchive,
  onFork,
}) => {
  const {
    theme: { colors },
    themed,
  } = useAppTheme()

  const displayTitle = conversation.summary || `Chat with ${agentName}`
  const timeAgo = formatRelativeTime(conversation.last_message_at || conversation.created_at)
  const isArchived = (conversation as any).is_archived ?? false

  return (
    <SimpleContextMenu
      actions={[
        {
          key: "edit",
          title: "Edit Summary",
          iosIconName: { name: "pencil", weight: "bold" },
          androidIconName: "ic_menu_edit",
          onPress: onEdit,
        },
        {
          key: "fork",
          title: "Fork Conversation",
          iosIconName: { name: "arrow.branch", weight: "bold" },
          androidIconName: "ic_menu_share",
          onPress: onFork,
        },
        {
          key: "archive",
          title: isArchived ? "Unarchive" : "Archive",
          iosIconName: { name: isArchived ? "tray.and.arrow.up" : "archivebox", weight: "bold" },
          androidIconName: "ic_menu_archive",
          onPress: onArchive,
        },
        {
          key: "delete",
          title: "Delete",
          iosIconName: { name: "trash", weight: "bold" },
          androidIconName: "ic_menu_delete",
          onPress: onDelete,
        },
      ]}
    >
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
    </SimpleContextMenu>
  )
}

interface AgentPickerModalProps {
  visible: boolean
  agents: Letta.AgentState[] | undefined
  onSelect: (agentId: string) => void
  onDismiss: () => void
  isCreating: boolean
}

const AgentPickerModal: FC<AgentPickerModalProps> = ({
  visible,
  agents,
  onSelect,
  onDismiss,
  isCreating,
}) => {
  const { themed, theme } = useAppTheme()
  const { colors } = theme

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onDismiss}>
      <Pressable style={$modalOverlay} onPress={onDismiss}>
        <View style={themed($modalContent)}>
          <View style={$modalHeader}>
            <Text preset="bold">Select an Agent</Text>
            <TouchableOpacity onPress={onDismiss}>
              <Icon icon="X" size={20} color={colors.text} />
            </TouchableOpacity>
          </View>
          <Text size="sm" style={themed($modalSubtext)}>
            Choose an agent to start a new conversation
          </Text>
          <ScrollView style={$agentList}>
            {agents?.map((agent) => (
              <TouchableOpacity
                key={agent.id}
                style={themed($agentItem)}
                onPress={() => onSelect(agent.id)}
                disabled={isCreating}
              >
                <View style={$agentItemContent}>
                  <Icon icon="Bot" size={18} color={colors.tint} />
                  <View style={$agentItemText}>
                    <Text preset="bold" size="sm" numberOfLines={1}>
                      {agent.name || "Unnamed Agent"}
                    </Text>
                    {agent.model && (
                      <Text size="xxs" style={themed($agentModelText)} numberOfLines={1}>
                        {agent.model}
                      </Text>
                    )}
                  </View>
                </View>
                {isCreating ? (
                  <ActivityIndicator size="small" color={colors.tint} />
                ) : (
                  <Icon icon="Plus" size={18} color={colors.tint} />
                )}
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>
      </Pressable>
    </Modal>
  )
}

export const ConversationsScreen: FC<AppStackScreenProps<"Conversations">> = () => {
  useLettaHeader()

  const {
    theme: { colors },
    themed,
  } = useAppTheme()

  const [showAgentPicker, setShowAgentPicker] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [showArchived, setShowArchived] = useState(false)

  const {
    data: _conversations,
    refetch,
    isFetching,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useAllConversations()
  const { data: agents } = useAgents()
  const { mutate: createConversation, isPending: isCreating } = useCreateConversation()
  const { mutate: deleteConversation } = useDeleteConversation()
  const { mutate: updateConversation } = useUpdateConversation()
  const { mutate: archiveConversation } = useArchiveConversation()
  const { mutate: forkConversation } = useForkConversation()

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

  // Filter conversations by search query and archive status
  const conversations = useMemo(() => {
    // Add agent names to conversations for searching
    const enriched =
      _conversations?.map((c) => ({
        ...c,
        agentName: agentMap[c.agent_id] || "Unknown Agent",
      })) || []

    // Filter by archive status
    const filtered = showArchived ? enriched : enriched.filter((c) => !(c as any).is_archived)

    // Sort by most recent first
    const sorted = filtered.sort((a, b) => {
      const aDate = new Date(a.last_message_at || a.created_at || "").getTime()
      const bDate = new Date(b.last_message_at || b.created_at || "").getTime()
      return bDate - aDate
    })

    if (!searchQuery.trim() || !sorted.length) return sorted

    const fuse = new Fuse(sorted, {
      keys: ["summary", "agentName"],
      threshold: 0.4,
      ignoreLocation: true,
      minMatchCharLength: 1,
    })

    return fuse.search(searchQuery.trim()).map((result) => result.item)
  }, [_conversations, agentMap, searchQuery, showArchived])

  const handleConversationPress = (conversation: Conversation) => {
    setAgentId(conversation.agent_id)
    setConversationId(conversation.id)
    navigate("AgentDrawer", { screen: "AgentTab" })
  }

  const handleDeleteConversation = (conversation: Conversation) => {
    Alert.alert(
      "Delete Conversation",
      "Are you sure you want to delete this conversation? This cannot be undone.",
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Delete",
          style: "destructive",
          onPress: () =>
            deleteConversation({
              conversationId: conversation.id,
              agentId: conversation.agent_id,
            }),
        },
      ],
    )
  }

  const handleEditConversation = (conversation: Conversation) => {
    const currentSummary = conversation.summary || ""
    Alert.prompt(
      "Edit Summary",
      "Enter a new summary for this conversation",
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Save",
          onPress: (newSummary) => {
            if (newSummary && newSummary !== currentSummary) {
              updateConversation({
                conversationId: conversation.id,
                agentId: conversation.agent_id,
                summary: newSummary,
              })
            }
          },
        },
      ],
      "plain-text",
      currentSummary,
    )
  }

  const handleArchiveConversation = (conversation: Conversation) => {
    const isArchived = (conversation as any).is_archived ?? false
    archiveConversation({
      conversationId: conversation.id,
      agentId: conversation.agent_id,
      isArchived: !isArchived,
    })
  }

  const handleForkConversation = (conversation: Conversation) => {
    forkConversation(
      {
        conversationId: conversation.id,
        agentId: conversation.agent_id,
      },
      {
        onSuccess: ({ forked }) => {
          setAgentId(conversation.agent_id)
          setConversationId(forked.id)
          navigate("AgentDrawer", { screen: "AgentTab" })
        },
      },
    )
  }

  const handleNewChat = () => {
    setShowAgentPicker(true)
  }

  const handleAgentSelect = (agentId: string) => {
    createConversation(
      { agentId },
      {
        onSuccess: (newConversation) => {
          setShowAgentPicker(false)
          setAgentId(agentId)
          setConversationId(newConversation.id)
          navigate("AgentDrawer", { screen: "AgentTab" })
        },
      },
    )
  }

  return (
    <Screen style={$root} preset="fixed" contentContainerStyle={$contentContainer}>
      <View style={$header}>
        <View style={themed($searchContainer)}>
          <Icon icon="Search" size={18} color={colors.textDim} />
          <TextInput
            style={themed($searchInput)}
            placeholder="Search conversations..."
            placeholderTextColor={colors.textDim}
            value={searchQuery}
            onChangeText={setSearchQuery}
            autoCapitalize="none"
            autoCorrect={false}
          />
          {searchQuery.length > 0 && (
            <Icon icon="X" size={18} color={colors.textDim} onPress={() => setSearchQuery("")} />
          )}
        </View>
        <TouchableOpacity
          style={themed($archiveToggle)}
          onPress={() => setShowArchived(!showArchived)}
        >
          <Icon
            icon={showArchived ? "Archive" : "ArchiveX"}
            size={18}
            color={showArchived ? colors.tint : colors.textDim}
          />
        </TouchableOpacity>
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
            onDelete={() => handleDeleteConversation(item)}
            onEdit={() => handleEditConversation(item)}
            onArchive={() => handleArchiveConversation(item)}
            onFork={() => handleForkConversation(item)}
          />
        )}
        contentContainerStyle={{ padding: spacing.sm }}
        onEndReached={() => {
          if (hasNextPage && !isFetchingNextPage && !searchQuery.trim()) {
            fetchNextPage()
          }
        }}
        onEndReachedThreshold={0.5}
        ListFooterComponent={
          isFetchingNextPage ? (
            <View style={$loadingMore}>
              <ActivityIndicator size="small" color={colors.tint} />
            </View>
          ) : null
        }
        ListEmptyComponent={
          searchQuery.trim() ? (
            <View style={$emptyState}>
              <Icon icon="Search" size={48} color={colors.textDim} />
              <Text style={themed($emptyText)}>No conversations found</Text>
              <Text size="sm" style={themed($emptySubtext)}>
                No results for &quot;{searchQuery}&quot;
              </Text>
            </View>
          ) : (
            <View style={$emptyState}>
              <Icon icon="MessageSquare" size={48} color={colors.textDim} />
              <Text style={themed($emptyText)}>No conversations yet</Text>
              <Text size="sm" style={themed($emptySubtext)}>
                Start a chat with an agent to begin
              </Text>
            </View>
          )
        }
      />

      <TouchableOpacity style={themed($fab)} onPress={handleNewChat} activeOpacity={0.8}>
        <Icon icon="Plus" size={24} color="#fff" />
      </TouchableOpacity>

      <AgentPickerModal
        visible={showAgentPicker}
        agents={agents}
        onSelect={handleAgentSelect}
        onDismiss={() => setShowAgentPicker(false)}
        isCreating={isCreating}
      />
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
  alignItems: "center",
  padding: spacing.sm,
  gap: spacing.sm,
}

const $searchContainer: ThemedStyle<ViewStyle> = ({ colors }) => ({
  flex: 1,
  flexDirection: "row",
  alignItems: "center",
  backgroundColor: colors.palette.overlay20,
  borderRadius: 8,
  paddingHorizontal: spacing.sm,
  paddingVertical: spacing.xs,
  gap: spacing.xs,
})

const $searchInput: ThemedStyle<TextStyle> = ({ colors }) => ({
  flex: 1,
  fontSize: 16,
  color: colors.text,
  paddingVertical: spacing.xxs,
})

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

const $modalOverlay: ViewStyle = {
  flex: 1,
  backgroundColor: "rgba(0,0,0,0.5)",
  justifyContent: "center",
  alignItems: "center",
  padding: spacing.lg,
}

const $modalContent: ThemedStyle<ViewStyle> = ({ colors }) => ({
  backgroundColor: colors.background,
  borderRadius: 12,
  width: "100%",
  maxWidth: 400,
  maxHeight: "70%",
  padding: spacing.md,
})

const $modalHeader: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
  marginBottom: spacing.xs,
}

const $modalSubtext: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
  marginBottom: spacing.md,
})

const $agentList: ViewStyle = {
  maxHeight: 300,
}

const $agentItem: ThemedStyle<ViewStyle> = ({ colors }) => ({
  flexDirection: "row",
  alignItems: "center",
  justifyContent: "space-between",
  paddingVertical: spacing.sm,
  paddingHorizontal: spacing.sm,
  borderBottomWidth: 1,
  borderBottomColor: colors.palette.overlay20,
})

const $agentItemContent: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  gap: spacing.sm,
  flex: 1,
}

const $agentItemText: ViewStyle = {
  flex: 1,
}

const $agentModelText: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
})

const $loadingMore: ViewStyle = {
  paddingVertical: spacing.md,
  alignItems: "center",
}

const $archiveToggle: ThemedStyle<ViewStyle> = ({ colors }) => ({
  padding: spacing.xs,
  backgroundColor: colors.palette.overlay20,
  borderRadius: 8,
})
