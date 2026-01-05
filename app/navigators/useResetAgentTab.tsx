import { agentStore } from "@/providers/AgentProvider"
import { autorun } from "mobx"
import { useEffect } from "react"
import { resetRoot } from "."
export function useResetAgentTab() {
  useEffect(() => {
    const unsub = autorun(() => {
      if (!agentStore.agentId) {
        console.log("NO AGENT ID :: RESETING")
        resetRoot({
          index: 0,
          routes: [{ name: "AgentList" }],
        })
      }
    })
    return () => unsub()
  }, [])
}
