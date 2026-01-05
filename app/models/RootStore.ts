import { Instance, SnapshotOut, types } from "mobx-state-tree"
import { AppSettingsStoreModel } from "./AppSettingsStore"
import { AuthenticationStoreModel } from "./AuthenticationStore"
import { LettaConfigStoreModel } from "./LettaConfigStore"

/**
 * A RootStore model.
 */
export const RootStoreModel = types.model("RootStore").props({
  authenticationStore: types.optional(AuthenticationStoreModel, {}),
  lettaConfigStore: types.optional(LettaConfigStoreModel, {}),
  appSettingsStore: types.optional(AppSettingsStoreModel, {}),
})

/**
 * The RootStore instance.
 */
export interface RootStore extends Instance<typeof RootStoreModel> {}
/**
 * The data of a RootStore.
 */
export interface RootStoreSnapshot extends SnapshotOut<typeof RootStoreModel> {}
