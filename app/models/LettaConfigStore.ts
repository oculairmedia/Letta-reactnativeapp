import Config from "@/config"
import { Instance, SnapshotOut, types } from "mobx-state-tree"

export const LettaConfigStoreModel = types
  .model("LettaConfigStore")
  .props({
    initialized: types.optional(types.boolean, false),
    serverUrl: types.optional(types.string, Config.lettaBaseUrl),
    accessToken: types.optional(types.string, Config.lettaAccessToken),
  })
  .views((store) => ({
    get isConfigured() {
      return store.initialized && !!store.accessToken && !!store.serverUrl
    },
  }))
  .actions((store) => ({
    setServerUrl(value: string) {
      store.serverUrl = value
    },
    setAccessToken(value: string) {
      store.accessToken = value
    },
    save() {
      store.initialized = true
    },
    reset() {
      store.initialized = false
    },
  }))

export interface LettaConfigStore extends Instance<typeof LettaConfigStoreModel> {}
export interface LettaConfigStoreSnapshot extends SnapshotOut<typeof LettaConfigStoreModel> {}
