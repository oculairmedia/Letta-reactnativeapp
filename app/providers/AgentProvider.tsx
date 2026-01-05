import { createContext, useContext, Fragment } from "react"
import { makeAutoObservable } from "mobx"
import { observer } from "mobx-react-lite"

class AgentStore {
  agentId: string = ""

  constructor() {
    makeAutoObservable(this)
  }

  setAgentId(id: string) {
    this.agentId = id
  }
}

export const agentStore = new AgentStore()

// Create a React Context
const AgentContext = createContext<AgentStore | undefined>(undefined)

export const AgentProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return <AgentContext.Provider value={agentStore}>{children}</AgentContext.Provider>
}

// Custom hook to use the store
export const useAgentContext = () => {
  const context = useContext(AgentContext)
  if (!context) {
    throw new Error("useAgent must be used within an AgentProvider")
  }
  return context
}

// Ensure MobX reactivity in consuming components
export const ObserverAgent = observer(({ children }: { children: React.ReactNode }) => (
  <Fragment>{children}</Fragment>
))
