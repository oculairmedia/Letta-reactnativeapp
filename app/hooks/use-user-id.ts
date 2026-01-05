import { storage } from "@/utils/storage"
import { useQuery } from "@tanstack/react-query"

export function useUserId() {
  return useQuery({
    queryKey: ["user-id"],
    queryFn: async () => {
      // Generate a stable anonymous user ID
      let userId = storage.getString("userId")
      if (!userId) {
        // Generate random UUID-like string
        userId = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
          const r = (Math.random() * 16) | 0
          const v = c === "x" ? r : (r & 0x3) | 0x8
          return v.toString(16)
        })
        storage.set("userId", userId)
      }
      return userId
    },
  })
}
