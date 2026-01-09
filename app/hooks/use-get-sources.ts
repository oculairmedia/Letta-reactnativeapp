import { useLettaClient } from "@/providers/LettaProvider"
import { FolderCreateParams } from "@letta-ai/letta-client/resources/folders/folders"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Uploadable } from "@letta-ai/letta-client/core/uploads"

const getSourcesQueryKey = () => ["sources"]
const getSourceQueryKey = (sourceId: string) => ["source", sourceId]
const getSourceFilesQueryKey = (sourceId: string) => ["source", sourceId, "files"]
export const getAgentSourcesQueryKey = (agentId: string) => ["agent", agentId, "sources"]

export const useGetSources = () => {
  const { lettaClient } = useLettaClient()

  return useQuery({
    queryKey: getSourcesQueryKey(),
    queryFn: () => lettaClient.folders.list(),
  })
}

export const useGetSource = (sourceId: string) => {
  const { lettaClient } = useLettaClient()
  return useQuery({
    queryKey: getSourceQueryKey(sourceId),
    queryFn: () => lettaClient.folders.retrieve(sourceId),
  })
}

export const useAddSource = () => {
  const queryClient = useQueryClient()
  const { lettaClient } = useLettaClient()

  return useMutation({
    mutationFn: (source: FolderCreateParams) => lettaClient.folders.create(source),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: getSourcesQueryKey() })
    },
  })
}

export const useDeleteSource = () => {
  const queryClient = useQueryClient()
  const { lettaClient } = useLettaClient()
  return useMutation({
    mutationFn: (sourceId: string) => lettaClient.folders.delete(sourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: getSourcesQueryKey() })
    },
  })
}

export const useAddFileToSource = () => {
  const queryClient = useQueryClient()
  const { lettaClient } = useLettaClient()

  return useMutation({
    mutationFn: ({ sourceId, file }: { sourceId: string; file: Uploadable }) =>
      lettaClient.folders.files.upload(sourceId, { file }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: getSourcesQueryKey() })
      queryClient.invalidateQueries({ queryKey: getSourceFilesQueryKey(variables.sourceId) })
    },
  })
}

export const useDeleteFileFromSource = () => {
  const queryClient = useQueryClient()
  const { lettaClient } = useLettaClient()
  return useMutation({
    mutationFn: ({ sourceId, fileId }: { sourceId: string; fileId: string }) =>
      lettaClient.folders.files.delete(fileId, { folder_id: sourceId }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: getSourcesQueryKey() })
      queryClient.invalidateQueries({ queryKey: getSourceFilesQueryKey(variables.sourceId) })
    },
  })
}

export const useGetSourceFiles = (sourceId: string) => {
  const { lettaClient } = useLettaClient()
  return useQuery({
    queryKey: getSourceFilesQueryKey(sourceId),
    queryFn: () => lettaClient.folders.files.list(sourceId),
  })
}

export const useGetAgentSources = (agentId: string) => {
  const { lettaClient } = useLettaClient()
  return useQuery({
    queryKey: getAgentSourcesQueryKey(agentId),
    queryFn: () => lettaClient.agents.folders.list(agentId),
  })
}

export const useAttachSourceToAgent = () => {
  const queryClient = useQueryClient()
  const { lettaClient } = useLettaClient()
  return useMutation({
    mutationFn: ({ agentId, sourceId }: { agentId: string; sourceId: string }) =>
      lettaClient.agents.folders.attach(sourceId, { agent_id: agentId }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: getAgentSourcesQueryKey(variables.agentId) })
    },
  })
}

export const useDetachSourceFromAgent = () => {
  const queryClient = useQueryClient()
  const { lettaClient } = useLettaClient()
  return useMutation({
    mutationFn: ({ agentId, sourceId }: { agentId: string; sourceId: string }) =>
      lettaClient.agents.folders.detach(sourceId, { agent_id: agentId }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: getAgentSourcesQueryKey(variables.agentId) })
    },
  })
}
