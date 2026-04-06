import { FC } from "react"
import { View, ViewStyle, TextStyle } from "react-native"
import { Text } from "./Text"
import { Icon } from "./Icon"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import { useConnectivity } from "@/hooks/use-connectivity"

interface OfflineBannerProps {
  style?: ViewStyle
}

export const OfflineBanner: FC<OfflineBannerProps> = ({ style }) => {
  const { themed, theme } = useAppTheme()
  const { isOffline, isConnected, isServerReachable } = useConnectivity()

  if (!isOffline) {
    return null
  }

  const message = !isConnected
    ? "No internet connection"
    : !isServerReachable
      ? "Cannot reach server"
      : "Connection issues"

  return (
    <View style={[themed($banner), style]}>
      <Icon icon="WifiOff" size={16} color={theme.colors.palette.neutral100} />
      <Text size="xs" style={$bannerText}>
        {message}
      </Text>
    </View>
  )
}

const $banner: ThemedStyle<ViewStyle> = ({ colors }) => ({
  flexDirection: "row",
  alignItems: "center",
  justifyContent: "center",
  gap: spacing.xs,
  paddingVertical: spacing.xs,
  paddingHorizontal: spacing.sm,
  backgroundColor: colors.error,
})

const $bannerText: TextStyle = {
  color: "#fff",
  fontWeight: "500",
}
