import { useLettaClient } from "@/providers/LettaProvider"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useMemo } from "react"

export const getUseMCPListKey = () => ["mcp-list"]
export const getUseMCPToolsKey = () => ["mcp-tools"]

export function useMCPList() {
  const { lettaClient } = useLettaClient()
  return useQuery({
    queryKey: getUseMCPListKey(),
    queryFn: () => lettaClient.tools.listMcpServers(),
  })
}

export function useMCPTools() {
  const { lettaClient } = useLettaClient()
  const { data: mcpServers, isLoading: isLoadingServers } = useMCPList()
  const serverList = useMemo(
    () =>
      mcpServers
        ? Object.entries(mcpServers).map(([name, server]) => ({
            ...server,
            serverName: name,
          }))
        : [],
    [mcpServers],
  )

  return useQuery({
    queryKey: getUseMCPToolsKey(),
    queryFn: async () => {
      if (!serverList.length) return []

      const tools = await Promise.all(
        serverList.map(async (server) => {
          try {
            const serverTools = await lettaClient.tools.listMcpToolsByServer(server.serverName)
            return serverTools.map((tool) => ({
              ...tool,
              serverName: server.serverName,
              serverType: server.type,
            }))
          } catch (error) {
            console.error(`Failed to fetch tools for server ${server.serverName}:`, error)
            return []
          }
        }),
      )
      return tools.flat()
    },
    enabled: !isLoadingServers && serverList.length > 0,
  })
}

export function useFetchMCPToolByMCPServer() {
  const { lettaClient } = useLettaClient()
  return useMutation({
    mutationFn: (payload: Parameters<typeof lettaClient.tools.addMcpTool>) =>
      lettaClient.tools.addMcpTool(...payload),
  })
}

export function useAddMCPServer() {
  const { lettaClient } = useLettaClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: Parameters<typeof lettaClient.tools.addMcpServer>[0]) =>
      lettaClient.tools.addMcpServer(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: getUseMCPListKey() })
      queryClient.invalidateQueries({ queryKey: getUseMCPToolsKey() })
    },
  })
}

export function useDeleteMCPServer() {
  const { lettaClient } = useLettaClient()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: Parameters<typeof lettaClient.tools.deleteMcpServer>[0]) =>
      lettaClient.tools.deleteMcpServer(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: getUseMCPListKey() })
      queryClient.invalidateQueries({ queryKey: getUseMCPToolsKey() })
    },
  })
}
