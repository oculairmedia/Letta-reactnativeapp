import { StyleProp, ViewStyle, View } from "react-native"
import { Button, ButtonProps } from "@/components"
import { useAppTheme } from "@/utils/useAppTheme"
import { ChevronDown } from "lucide-react-native"
import Animated, {
  useAnimatedStyle,
  useDerivedValue,
  useSharedValue,
  withTiming,
} from "react-native-reanimated"
import { colors, spacing } from "@/theme"

export interface CardAccordionItemProps {
  /**
   * The content to be displayed inside the accordion
   */
  children: React.ReactNode
  /**
   * An optional style override for the container
   */
  style?: StyleProp<ViewStyle>
  /**
   * The duration of the animation in milliseconds
   */
  duration?: number
  /**
   * Whether the accordion is expanded
   */
  isExpanded: boolean
}

export interface CardAccordionProps extends Omit<ButtonProps, "preset"> {
  /**
   * The content to be displayed inside the accordion
   */
  children: React.ReactNode
  /**
   * Whether the accordion is expanded
   */
  isExpanded: boolean
  /**
   * Callback when the accordion is toggled
   */
  onToggle: () => void
  /**
   * The duration of the animation in milliseconds
   */
  duration?: number
  /**
   * An optional style override for the container
   */
  style?: StyleProp<ViewStyle>
}

export function CardAccordionItem({
  children,
  style,
  duration = 300,
  isExpanded,
}: CardAccordionItemProps) {
  const height = useSharedValue(0)

  const derivedHeight = useDerivedValue(() =>
    withTiming(height.value * Number(isExpanded), {
      duration,
    }),
  )

  const bodyStyle = useAnimatedStyle(() => ({
    height: derivedHeight.value,
  }))

  return (
    <Animated.View style={[$animatedView, bodyStyle, style]}>
      <View
        onLayout={(e) => {
          height.value = e.nativeEvent.layout.height
        }}
        style={$wrapper}
      >
        {children}
      </View>
    </Animated.View>
  )
}

export function CardAccordion({
  children,
  isExpanded,
  onToggle,
  duration = 300,
  style,
  ...rest
}: CardAccordionProps) {
  const { theme } = useAppTheme()

  return (
    <View style={[$container, style]}>
      <Button
        contentStyle={$button}
        RightAccessory={({ style: accessoryStyle }) => (
          <Animated.View
            style={[
              accessoryStyle,
              {
                transform: [{ rotate: isExpanded ? "180deg" : "0deg" }],
              },
            ]}
          >
            <ChevronDown size={20} color={theme.colors.tint} />
          </Animated.View>
        )}
        onPress={onToggle}
        {...rest}
      />
      <CardAccordionItem isExpanded={isExpanded} duration={duration}>
        {children}
      </CardAccordionItem>
    </View>
  )
}

const $container: ViewStyle = {
  overflow: "hidden",
  backgroundColor: colors.background,
  borderColor: colors.tint,
  borderWidth: 1,
  borderRadius: 8,
}

const $wrapper: ViewStyle = {
  width: "100%",
  position: "absolute",
  padding: spacing.sm,
}

const $animatedView: ViewStyle = {
  width: "100%",
  overflow: "hidden",
}

const $button: ViewStyle = {
  width: "100%",
  overflow: "hidden",
  justifyContent: "space-between",
  padding: spacing.sm,
  backgroundColor: "transparent",
}
