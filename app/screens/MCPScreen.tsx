import { Card, Icon, Screen, Text } from "@/components"
import { Badge } from "@/components/Badge"
import { Button } from "@/components/Button"
import { AddMCPServerModal } from "@/components/custom/modals/add-mcp-server-modal"
import { useLettaHeader } from "@/components/custom/useLettaHeader"
import { SimpleContextMenu } from "@/components/simple-context-menu"
import { useAddMCPServer, useDeleteMCPServer, useMCPList, useMCPTools } from "@/hooks/use-mcp"
import { AppStackScreenProps } from "@/navigators"
import { BareAccordion } from "@/shared/components/animated/BareAccordion"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import {
  McpServerCreateParams,
  SseMcpServer,
  StdioMcpServer,
  StreamableHTTPMcpServer,
} from "@letta-ai/letta-client/resources/mcp-servers"
import { FC, Fragment, useMemo, useState } from "react"
import { Alert, FlatList, RefreshControl, TextStyle, View, ViewStyle } from "react-native"

interface MCPTool {
  name: string
  description?: string | null
  serverName: string
}

interface MCPServerCardProps {
  server: SseMcpServer | StdioMcpServer | StreamableHTTPMcpServer
  tools: MCPTool[]
  isLoadingTools: boolean
  onDelete: () => void
}

const isSseServer = (
  server: SseMcpServer | StdioMcpServer | StreamableHTTPMcpServer,
): server is SseMcpServer => {
  return (server as SseMcpServer).mcp_server_type === "sse"
}
const isStreamableHTTPServer = (
  server: SseMcpServer | StdioMcpServer | StreamableHTTPMcpServer,
): server is StreamableHTTPMcpServer => {
  return (server as StreamableHTTPMcpServer).mcp_server_type === "streamable_http"
}

const MCPServerCard: FC<MCPServerCardProps> = ({ server, tools, isLoadingTools, onDelete }) => {
  const { themed } = useAppTheme()
  const [isExpanded, setIsExpanded] = useState(false)

  const toolCount = tools.length
  const toolCountText = isLoadingTools
    ? "Loading tools..."
    : `${toolCount} tool${toolCount === 1 ? "" : "s"}`

  return (
    <SimpleContextMenu
      actions={[
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
        heading={server.server_name || "Unnamed MCP Server"}
        ContentComponent={
          <View style={$serverContentContainer}>
            {isSseServer(server) || isStreamableHTTPServer(server) ? (
              <Text style={themed($serverContentTextStyle)} size="xs">
                URL: {server.server_url}
              </Text>
            ) : (
              <Fragment>
                <Text style={themed($serverContentTextStyle)} size="xs">
                  Command: {server.command}
                </Text>
                <Text style={themed($serverContentTextStyle)} size="xs">
                  Args: {server.args.join(" ")}
                </Text>
              </Fragment>
            )}

            <View style={$toolCountContainer}>
              <Badge text={toolCountText} />
            </View>

            {toolCount > 0 && (
              <BareAccordion
                isExpanded={isExpanded}
                onToggle={() => setIsExpanded(!isExpanded)}
                style={$toolsAccordion}
                triggerNode={({ animatedChevron }) => (
                  <View style={$toolsHeader}>
                    <Text preset="bold" size="xs">
                      {isExpanded ? "Hide Tools" : "Show Tools"}
                    </Text>
                    {animatedChevron}
                  </View>
                )}
              >
                <View style={$toolsList}>
                  {tools.map((tool, index) => (
                    <View key={tool.name + index} style={$toolItem}>
                      <Text preset="bold" size="xs">
                        {tool.name}
                      </Text>
                      {tool.description && (
                        <Text style={themed($toolDescription)} size="xxs" numberOfLines={2}>
                          {tool.description}
                        </Text>
                      )}
                    </View>
                  ))}
                </View>
              </BareAccordion>
            )}
          </View>
        }
        RightComponent={<Badge text={server.mcp_server_type!} />}
      />
    </SimpleContextMenu>
  )
}

export const MCPScreen: FC<AppStackScreenProps<"MCP">> = () => {
  useLettaHeader()

  const [isAddModalVisible, setIsAddModalVisible] = useState(false)
  const { data: servers, refetch, isFetching } = useMCPList()
  const { data: allTools, isLoading: isLoadingTools, refetch: refetchTools } = useMCPTools()
  const addServerMutation = useAddMCPServer()
  const deleteServerMutation = useDeleteMCPServer()

  const {
    theme: { colors },
  } = useAppTheme()

  // Group tools by server name
  const toolsByServer = useMemo(() => {
    const grouped: Record<string, MCPTool[]> = {}
    if (!allTools) return grouped

    for (const tool of allTools) {
      const serverName = tool.serverName || "Unknown"
      if (!grouped[serverName]) {
        grouped[serverName] = []
      }
      grouped[serverName].push({
        name: tool.name || "Unnamed Tool",
        description: tool.description,
        serverName: tool.serverName,
      })
    }
    return grouped
  }, [allTools])

  const handleAddServer = (serverData: McpServerCreateParams) => {
    addServerMutation.mutate(serverData)
    setIsAddModalVisible(false)
  }

  const handleDeleteServer = (serverName: string) => {
    Alert.alert("Delete MCP Server", "Are you sure you want to delete this server?", [
      {
        text: "Cancel",
        style: "cancel",
      },
      {
        text: "Delete",
        style: "destructive",
        onPress: () => {
          deleteServerMutation.mutate(serverName)
        },
      },
    ])
  }

  const handleRefresh = () => {
    refetch()
    refetchTools()
  }

  return (
    <Screen style={$root} preset="fixed" contentContainerStyle={$contentContainer}>
      <View style={$header}>
        <Button
          onPress={() => setIsAddModalVisible(true)}
          text="Add MCP Server"
          loading={addServerMutation.isPending}
          disabled={addServerMutation.isPending}
          LeftAccessory={() => (
            <Icon icon="Plus" size={20} color={colors.elementColors.card.default.content} />
          )}
        />
      </View>

      <FlatList
        data={servers || []}
        bounces={!!servers && servers.length > 0}
        keyExtractor={(item) => item.server_name}
        refreshControl={<RefreshControl refreshing={isFetching} onRefresh={handleRefresh} />}
        refreshing={isFetching}
        ItemSeparatorComponent={() => <View style={{ height: spacing.sm }} />}
        renderItem={({ item }) => (
          <MCPServerCard
            server={item}
            tools={toolsByServer[item.server_name] || []}
            isLoadingTools={isLoadingTools}
            onDelete={() => handleDeleteServer(item.server_name)}
          />
        )}
        ListEmptyComponent={<Text>No MCP Servers</Text>}
        contentContainerStyle={{ padding: spacing.sm }}
      />

      <AddMCPServerModal
        visible={isAddModalVisible}
        onDismiss={() => setIsAddModalVisible(false)}
        onSubmit={handleAddServer}
        isPending={addServerMutation.isPending}
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
  justifyContent: "flex-end",
  alignItems: "center",
  padding: spacing.sm,
  gap: spacing.sm,
}

const $serverContentContainer: ViewStyle = {
  flex: 1,
  gap: spacing.xs,
}

const $serverContentTextStyle: ThemedStyle<TextStyle> = () => ({
  opacity: 0.8,
})

const $toolCountContainer: ViewStyle = {
  flexDirection: "row",
  marginTop: spacing.xs,
}

const $toolsAccordion: ViewStyle = {
  marginTop: spacing.xs,
}

const $toolsHeader: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  gap: spacing.xs,
  paddingVertical: spacing.xs,
}

const $toolsList: ViewStyle = {
  gap: spacing.xs,
  paddingTop: spacing.xs,
}

const $toolItem: ViewStyle = {
  paddingVertical: spacing.xxs,
  paddingHorizontal: spacing.xs,
  backgroundColor: "rgba(0,0,0,0.05)",
  borderRadius: spacing.xxs,
}

const $toolDescription: ThemedStyle<TextStyle> = () => ({
  opacity: 0.7,
})
