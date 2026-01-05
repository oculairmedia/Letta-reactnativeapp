import { StyleProp, TextStyle, View, ViewStyle } from "react-native"
import { Text } from "./Text"
import { useAppTheme } from "@/utils/useAppTheme"
import { spacing } from "@/theme"

export interface BadgeProps {
  /**
   * The text to display in the badge
   */
  text: string
  /**
   * Optional style override for the badge container
   */
  style?: StyleProp<ViewStyle>
  /**
   * Optional style override for the badge text
   */
  textStyle?: StyleProp<TextStyle>
}

export function Badge(props: BadgeProps) {
  const { text, style: $styleOverride, textStyle: $textStyleOverride } = props
  const {
    theme: { colors },
  } = useAppTheme()

  return (
    <View
      style={[
        {
          backgroundColor: colors.elementColors.button.filled.backgroundColor,
          paddingHorizontal: spacing.xs,
          alignSelf: "flex-start",
        },
        $styleOverride,
      ]}
    >
      <Text
        size="xxs"
        style={[
          {
            color: colors.text,
          },
          $textStyleOverride,
        ]}
      >
        {text}
      </Text>
    </View>
  )
}
