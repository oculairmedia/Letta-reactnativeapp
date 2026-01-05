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

export interface AccordionItemProps {
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

export interface AccordionProps extends ButtonProps {
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

export function AccordionItem({ children, style, duration = 300, isExpanded }: AccordionItemProps) {
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

export function Accordion({
  children,
  isExpanded,
  onToggle,
  duration = 300,
  style,
  ...rest
}: AccordionProps) {
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
            <ChevronDown
              size={20}
              color={
                rest.preset === "reversed" || rest.preset === "destructive"
                  ? theme.colors.background
                  : theme.colors.text
              }
            />
          </Animated.View>
        )}
        onPress={onToggle}
        {...rest}
      />
      <AccordionItem isExpanded={isExpanded} duration={duration}>
        {children}
      </AccordionItem>
    </View>
  )
}

const $container: ViewStyle = {
  overflow: "hidden",
}

const $wrapper: ViewStyle = {
  width: "100%",
  position: "absolute",
}

const $animatedView: ViewStyle = {
  width: "100%",
  overflow: "hidden",
}

const $button: ViewStyle = {
  width: "100%",
  overflow: "hidden",
  justifyContent: "space-between",
}
