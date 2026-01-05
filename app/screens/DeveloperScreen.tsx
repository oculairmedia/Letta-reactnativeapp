import { Screen, Text } from "@/components"

import { LettaConfigsForm } from "@/components/custom/forms/letta-config"
import { ThemeToggle } from "@/components/custom/theme-toggle"
import { useLettaHeader } from "@/components/custom/useLettaHeader"
import { isRTL } from "@/i18n"
import { $styles, type ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import { observer } from "mobx-react-lite"
import { FC } from "react"
import { Image, ImageStyle, TextStyle, View, ViewStyle } from "react-native"
import { useSafeAreaInsets } from "react-native-safe-area-context"
import { AppStackScreenProps } from "../navigators"

const welcomeFace = require("../../assets/images/welcome-face.png")

interface DeveloperScreenProps extends AppStackScreenProps<"Developer"> {}

export const DeveloperScreen: FC<DeveloperScreenProps> = observer(function DeveloperScreen(_props) {
  const { themed, theme } = useAppTheme()

  useLettaHeader()

  const { bottom } = useSafeAreaInsets()

  return (
    <Screen preset="fixed" contentContainerStyle={$styles.flex1}>
      <View style={themed($topContainer)}>
        <Text
          testID="developer-heading"
          style={themed($welcomeHeading)}
          tx="developerScreen:title"
          preset="heading"
        />
        <Text tx="developerScreen:description" preset="subheading" />
        <ThemeToggle />

        <Image
          style={$welcomeFace}
          source={welcomeFace}
          resizeMode="contain"
          tintColor={theme.colors.text}
        />
      </View>

      <View style={[themed([$bottomContainer]), { paddingBottom: bottom }]}>
        <Text
          testID="config-heading"
          tx="developerScreen:serverDetails"
          preset="formLabel"
          style={themed($subheading)}
        />
        <LettaConfigsForm />
      </View>
    </Screen>
  )
})

const $topContainer: ThemedStyle<ViewStyle> = ({ spacing }) => ({
  flexShrink: 1,
  flexGrow: 0,
  justifyContent: "center",
  paddingHorizontal: spacing.lg,
})

const $bottomContainer: ThemedStyle<ViewStyle> = ({ spacing }) => ({
  flexShrink: 1,
  flexGrow: 0.5,
  borderTopLeftRadius: spacing.sheetRadius,
  borderTopRightRadius: spacing.sheetRadius,
  paddingHorizontal: spacing.lg,
  justifyContent: "flex-end",
  paddingTop: spacing.md,
})

const $welcomeFace: ImageStyle = {
  height: 169,
  width: 269,
  position: "absolute",
  bottom: -47,
  right: -80,
  transform: [{ scaleX: isRTL ? -1 : 1 }],
}

const $welcomeHeading: ThemedStyle<TextStyle> = ({ spacing }) => ({
  marginBottom: spacing.md,
})

const $subheading: ThemedStyle<TextStyle> = ({ spacing }) => ({
  marginBottom: spacing.lg,
})
