import { useMutation, UseMutationOptions } from "@tanstack/react-query"
import { useLettaClient } from "@/providers/LettaProvider"
import type * as Letta from "@letta-ai/letta-client"

export const useModifyBlock = (mutationOptions: UseMutationOptions<any, Error, any> = {}) => {
  const { lettaClient } = useLettaClient()

  return useMutation({
    mutationFn: ({ id, block }: { id: string; block: Letta.BlockUpdateParams }) =>
      lettaClient.blocks.update(id, block),
    ...mutationOptions,
  })
}
