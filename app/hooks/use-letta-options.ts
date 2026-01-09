import { useLettaClient } from "@/providers/LettaProvider"
import { useQuery } from "@tanstack/react-query"

export const getLettaModelsQueryKey = () => ["letta-models"]

export const useGetLettaModels = () => {
  const { lettaClient } = useLettaClient()
  return useQuery({
    queryKey: getLettaModelsQueryKey(),
    queryFn: () => lettaClient.models.list(),
  })
}

export const getLettaEmbeddingModelsQueryKey = () => ["letta-embedding-models"]
export const useGetLettaEmbeddingModels = () => {
  const { lettaClient } = useLettaClient()
  return useQuery({
    queryKey: getLettaEmbeddingModelsQueryKey(),
    queryFn: () => lettaClient.models.embeddings.list(),
  })
}
