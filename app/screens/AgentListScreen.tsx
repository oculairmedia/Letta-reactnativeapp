import { Card, Icon, Screen, Text } from "@/components"
import { Badge } from "@/components/Badge"
import { Button } from "@/components/Button"
import { useLettaHeader } from "@/components/custom/useLettaHeader"
import { SimpleContextMenu } from "@/components/simple-context-menu"
import { useAgents } from "@/hooks/use-agents"
import { useCreateAgent } from "@/hooks/use-create-agent"
import { useDeleteAgent } from "@/hooks/use-delete-agent"
import { useUserId } from "@/hooks/use-user-id"
import { AppStackScreenProps, navigate } from "@/navigators"
import { agentStore } from "@/providers/AgentProvider"
import { normalizeName } from "@/shared/utils/normalizers"
import { spacing, ThemedStyle } from "@/theme"
import { showAgentNamePrompt } from "@/utils/agent-name-prompt"
import { useAppTheme } from "@/utils/useAppTheme"
import { Letta } from "@letta-ai/letta-client"
import { observer } from "mobx-react-lite"
import { FC, useMemo } from "react"
import { FlatList, RefreshControl, TextStyle, View, ViewStyle } from "react-native"

interface AgentCardProps {
  agent: Letta.AgentState
  onPress: () => void
}

const chatWithAgent = (agentId: string) => {
  agentStore.setAgentId(agentId)
  navigate("AgentDrawer", { screen: "AgentTab" })
}

const AgentCard: FC<AgentCardProps> = ({ agent }) => {
  const { data: userId } = useUserId()
  const {
    theme: { colors },
    themed,
  } = useAppTheme()

  const deleteAgent = useDeleteAgent()

  const customTools = useMemo(() => {
    return agent.tools.filter((t) => t.tool_type === "custom")
  }, [agent.tools])

  const otherTags = useMemo(() => {
    return agent.tags.filter((tag) => tag !== userId)
  }, [agent.tags, userId])

  return (
    <SimpleContextMenu
      actions={[
        {
          key: "delete",
          title: "Delete",
          iosIconName: { name: "trash", weight: "bold" },
          androidIconName: "ic_menu_delete",
          onPress: () => {
            deleteAgent.mutate({ agentId: agent.id })
          },
        },
      ]}
    >
      <Card
        onPress={() => {
          chatWithAgent(agent.id)
        }}
        disabled={deleteAgent.isPending}
        heading={agent.name || "Unnamed Agent"}
        ContentComponent={
          <View style={$agentContentContainer}>
            <Text style={themed($agentContentTextStyle)} numberOfLines={2}>
              {agent.description}
            </Text>
            <View style={$agentContentFooter}>
              <Text size="xxs">
                last usage:{" "}
                {new Intl.DateTimeFormat("en-US", {
                  year: "numeric",
                  month: "short",
                  day: "numeric",
                  hour: "numeric",
                  minute: "numeric",
                }).format(new Date(agent.updated_at ?? ""))}
              </Text>
            </View>
            <View style={$agentMetadataContainer}>
              {!!otherTags.length && (
                <View style={$toolBadgesContainer}>
                  {otherTags.map((tag) => (
                    <Badge key={tag} text={tag} style={themed($badgeStyle)} />
                  ))}
                </View>
              )}
              <View style={$statsContainer}>
                <View style={$toolBadgesContainer}>
                  {customTools.map((t) => (
                    <Badge key={t.name} text={normalizeName(t.name ?? undefined)} />
                  ))}
                </View>
                <View style={$statsRow}>
                  <Text size="xxs">{agent.model}</Text>
                  <Text size="xxs">{agent.blocks.length} blocks</Text>
                  <Text size="xxs">{agent.sources?.length || 0} sources</Text>
                </View>
              </View>
            </View>
          </View>
        }
        RightComponent={
          <Icon icon="caretRight" size={20} color={colors.elementColors.card.default.content} />
        }
      />
    </SimpleContextMenu>
  )
}

export const AgentListScreen: FC<AppStackScreenProps<"AgentList">> = observer(
  function AgentListScreen() {
    useLettaHeader()

    const { data: _agents, refetch, isFetching } = useAgents()

    const agents = useMemo(() => {
      return _agents?.sort((a, b) => {
        return (b.updated_at?.getTime() ?? 0) - (a.updated_at?.getTime() ?? 0)
      })
    }, [_agents])

    const { mutate: createAgent, isPending: isCreatingAgent } = useCreateAgent({
      onSuccess: (data) => {
        chatWithAgent(data.id)
      },
    })

    const {
      theme: { colors },
    } = useAppTheme()

    return (
      <Screen style={$root} preset="fixed" contentContainerStyle={$contentContainer}>
        <View style={$header}>
          <View style={$headerRow}>
            <Button
              onPress={() => navigate("Studio")}
              text="Studio"
              style={$headerButton}
              disabled={isCreatingAgent}
              LeftAccessory={() => (
                <Icon
                  icon="FlaskConical"
                  size={20}
                  color={colors.elementColors.card.default.content}
                />
              )}
            />
            <Button
              onPress={() => {
                showAgentNamePrompt({
                  onSubmit: (name) => createAgent({ name }),
                })
              }}
              text="New Agent"
              style={$headerButton}
              loading={isCreatingAgent}
              disabled={isCreatingAgent}
              LeftAccessory={() => (
                <Icon icon="Bot" size={20} color={colors.elementColors.card.default.content} />
              )}
            />
          </View>
          <View style={$headerRow}>
            <Button
              onPress={() => navigate("MCP")}
              text="MCP"
              style={$headerButton}
              disabled={isCreatingAgent}
              LeftAccessory={() => (
                <Icon icon="Server" size={20} color={colors.elementColors.card.default.content} />
              )}
            />
          </View>
        </View>
        <FlatList
          data={agents}
          bounces={!!agents?.length}
          keyExtractor={(item) => item.id}
          refreshControl={<RefreshControl refreshing={isFetching} onRefresh={refetch} />}
          refreshing={isFetching}
          ItemSeparatorComponent={() => <View style={{ height: spacing.sm }} />}
          renderItem={({ item }) => (
            <AgentCard
              agent={item}
              onPress={() => {
                chatWithAgent(item.id)
              }}
            />
          )}
          contentContainerStyle={{ padding: spacing.sm }}
        />
      </Screen>
    )
  },
)

const $root: ViewStyle = {
  flex: 1,
}

const $contentContainer: ViewStyle = {
  flex: 1,
  paddingBottom: spacing.lg,
}

const $header: ViewStyle = {
  padding: spacing.sm,
  gap: spacing.sm,
}

const $headerRow: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
  gap: spacing.sm,
}

const $headerButton: ViewStyle = {
  flex: 1,
}

const $agentContentTextStyle: ThemedStyle<TextStyle> = () => ({
  opacity: 0.8,
})

const $agentContentFooter: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
}

const $agentContentContainer: ViewStyle = {
  flex: 1,
}

const $toolBadgesContainer: ViewStyle = {
  flexDirection: "row",
  flexWrap: "wrap",
  gap: spacing.xxs,
}

const $agentMetadataContainer: ViewStyle = {
  gap: spacing.xs,
  marginTop: spacing.xs,
}

const $statsContainer: ViewStyle = {
  gap: spacing.xxs,
}

const $statsRow: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
}

const $badgeStyle: ThemedStyle<ViewStyle> = ({ colors }) => ({
  backgroundColor: colors.elementColors.button.filled.backgroundColor,
  opacity: 0.7,
})
