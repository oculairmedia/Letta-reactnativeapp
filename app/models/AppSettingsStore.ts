import { Instance, SnapshotOut, types } from "mobx-state-tree"

export const AppSettingsStoreModel = types
  .model("AppSettingsStore")
  .props({
    appTheme: types.optional(types.enumeration("AppTheme", ["light", "dark"]), "light"),
  })
  .actions((store) => ({
    setAppTheme(value: "light" | "dark") {
      store.appTheme = value
    },
  }))

export interface AppSettingsStore extends Instance<typeof AppSettingsStoreModel> {}
export interface AppSettingsStoreSnapshot extends SnapshotOut<typeof AppSettingsStoreModel> {}
