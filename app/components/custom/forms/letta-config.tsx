import { useAppTheme } from "@/utils/useAppTheme"
import { observer } from "mobx-react-lite"
import { Fragment } from "react"
import { Button, TextField } from "@/components"
import type { ThemedStyle } from "@/theme"
import type { ViewStyle } from "react-native"
import { useLettaClient } from "@/providers/LettaProvider"
import { useStores } from "@/models"
import Config from "@/config"

export const LettaConfigsForm = observer(() => {
  const { setConfig } = useLettaClient()
  const { themed } = useAppTheme()

  const {
    lettaConfigStore: {
      serverUrl: _serverUrl,
      accessToken: _accessToken,
      setServerUrl,
      setAccessToken,
    },
  } = useStores()

  const serverUrl = _serverUrl || Config.lettaBaseUrl
  const accessToken = _accessToken || Config.lettaAccessToken

  function saveConfig() {
    setConfig(serverUrl || Config.lettaBaseUrl, accessToken || Config.lettaAccessToken)
  }

  return (
    <Fragment>
      <TextField
        value={serverUrl}
        onChangeText={setServerUrl}
        containerStyle={themed($textField)}
        autoCapitalize="none"
        autoCorrect={false}
        labelTx="developerScreen:serverUrl"
        placeholderTx="developerScreen:serverUrlPlaceholder"
        helperTx={!serverUrl ? "developerScreen:serverUrlRequired" : undefined}
        status={!serverUrl ? "error" : undefined}
      />
      <TextField
        value={accessToken}
        onChangeText={setAccessToken}
        containerStyle={themed($textField)}
        autoCapitalize="none"
        autoCorrect={false}
        labelTx="developerScreen:apiKey"
        placeholderTx="developerScreen:apiKeyPlaceholder"
        helperTx={!accessToken ? "developerScreen:apiKeyRequired" : undefined}
        status={!accessToken ? "error" : undefined}
      />

      <Button
        testID="save-config-button"
        tx="developerScreen:save"
        style={themed($button)}
        preset="reversed"
        onPress={saveConfig}
      />
    </Fragment>
  )
})

const $textField: ThemedStyle<ViewStyle> = ({ spacing }) => ({
  marginBottom: spacing.lg,
})

const $button: ThemedStyle<ViewStyle> = ({ spacing }) => ({
  marginTop: spacing.lg,
})
