import type { ThemedStyle, ThemedStyleArray } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import { ComponentType } from "react"
import {
  ActivityIndicator,
  Pressable,
  PressableProps,
  PressableStateCallbackType,
  StyleProp,
  TextStyle,
  View,
  ViewStyle,
} from "react-native"
import { $styles, spacing } from "../theme"
import { Text, TextProps } from "./Text"
type Presets = "default" | "filled" | "reversed" | "destructive" | "icon"

export interface ButtonAccessoryProps {
  style: StyleProp<any>
  pressableState: PressableStateCallbackType
  disabled?: boolean
}

export interface ButtonProps extends PressableProps {
  /**
   * Text which is looked up via i18n.
   */
  tx?: TextProps["tx"]
  /**
   * The text to display if not using `tx` or nested components.
   */
  text?: TextProps["text"]
  /**
   * Optional options to pass to i18n. Useful for interpolation
   * as well as explicitly setting locale or translation fallbacks.
   */
  txOptions?: TextProps["txOptions"]
  /**
   * An optional style override useful for padding & margin.
   */
  style?: StyleProp<ViewStyle>
  /**
   * An optional style override for the "pressed" state.
   */
  pressedStyle?: StyleProp<ViewStyle>
  /**
   * content style override
   */
  contentStyle?: StyleProp<ViewStyle>
  /**
   * An optional style override for the button text.
   */
  textStyle?: StyleProp<TextStyle>
  /**
   * An optional style override for the button text when in the "pressed" state.
   */
  pressedTextStyle?: StyleProp<TextStyle>
  /**
   * An optional style override for the button text when in the "disabled" state.
   */
  disabledTextStyle?: StyleProp<TextStyle>
  /**
   * One of the different types of button presets.
   */
  preset?: Presets
  /**
   * An optional component to render on the right side of the text.
   * Example: `RightAccessory={(props) => <View {...props} />}`
   */
  RightAccessory?: ComponentType<ButtonAccessoryProps>
  /**
   * An optional component to render on the left side of the text.
   * Example: `LeftAccessory={(props) => <View {...props} />}`
   */
  LeftAccessory?: ComponentType<ButtonAccessoryProps>
  /**
   * Children components.
   */
  children?: React.ReactNode
  /**
   * disabled prop, accessed directly for declarative styling reasons.
   * https://reactnative.dev/docs/pressable#disabled
   */
  disabled?: boolean
  /**
   * An optional style override for the disabled state
   */
  disabledStyle?: StyleProp<ViewStyle>
  /**
   * An optional style override for the button text when in the "disabled" state.
   */
  loading?: boolean
}

/**
 * A component that allows users to take actions and make choices.
 * Wraps the Text component with a Pressable component.
 * @see [Documentation and Examples]{@link https://docs.infinite.red/ignite-cli/boilerplate/app/components/Button/}
 * @param {ButtonProps} props - The props for the `Button` component.
 * @returns {JSX.Element} The rendered `Button` component.
 * @example
 * <Button
 *   tx="common:ok"
 *   style={styles.button}
 *   textStyle={styles.buttonText}
 *   onPress={handleButtonPress}
 * />
 */
export function Button(props: ButtonProps) {
  const {
    tx,
    text,
    txOptions,
    style: $viewStyleOverride,
    contentStyle: $contentStyleOverride,
    pressedStyle: $pressedViewStyleOverride,
    textStyle: $textStyleOverride,
    pressedTextStyle: $pressedTextStyleOverride,
    disabledTextStyle: $disabledTextStyleOverride,
    children,
    RightAccessory,
    LeftAccessory,
    disabled: $disabled,
    disabledStyle: $disabledViewStyleOverride,
    loading,
    ...rest
  } = props

  const { themed } = useAppTheme()

  const disabled = $disabled || loading

  const preset: Presets = props.preset ?? "default"
  /**
   * @param {PressableStateCallbackType} root0 - The root object containing the pressed state.
   * @param {boolean} root0.pressed - The pressed state.
   * @returns {StyleProp<ViewStyle>} The view style based on the pressed state.
   */
  function $viewStyle({ pressed }: PressableStateCallbackType): StyleProp<ViewStyle> {
    return [
      themed($viewPresets[preset]),
      $viewStyleOverride,
      !!pressed && themed([$pressedViewPresets[preset], $pressedViewStyleOverride]),
      (!!loading || disabled) && $disabledViewStyle,
      (!!loading || disabled) && $disabledViewStyleOverride,
    ]
  }
  /**
   * @param {PressableStateCallbackType} root0 - The root object containing the pressed state.
   * @param {boolean} root0.pressed - The pressed state.
   * @returns {StyleProp<TextStyle>} The text style based on the pressed state.
   */
  function $textStyle({ pressed }: PressableStateCallbackType): StyleProp<TextStyle> {
    return [
      themed($textPresets[preset]),
      $textStyleOverride,
      !!pressed && themed([$pressedTextPresets[preset], $pressedTextStyleOverride]),
      !!disabled && $disabledTextStyleOverride,
    ]
  }

  return (
    <Pressable
      style={$viewStyle}
      accessibilityRole="button"
      accessibilityState={{ disabled: !!disabled }}
      {...rest}
      disabled={disabled}
    >
      {(state) => (
        <View style={[$contentContainer, $contentStyleOverride]}>
          {!!LeftAccessory && !loading && (
            <LeftAccessory style={$leftAccessoryStyle} pressableState={state} disabled={disabled} />
          )}

          <Text tx={tx} text={text} txOptions={txOptions} style={$textStyle(state)}>
            {children}
          </Text>

          {!!RightAccessory && !loading && (
            <RightAccessory
              style={$rightAccessoryStyle}
              pressableState={state}
              disabled={disabled}
            />
          )}
          {loading && <ActivityIndicator />}
        </View>
      )}
    </Pressable>
  )
}

const $baseViewStyle: ThemedStyle<ViewStyle> = ({ spacing }) => ({
  borderRadius: spacing.buttonRadius,
  justifyContent: "center",
  alignItems: "center",
  paddingVertical: spacing.sm,
  paddingHorizontal: spacing.sm,
  overflow: "hidden",
})

const $baseTextStyle: ThemedStyle<TextStyle> = ({ typography }) => ({
  fontSize: 16,
  lineHeight: 20,
  fontFamily: typography.primary.medium,
  textAlign: "center",
  flexShrink: 1,
  flexGrow: 0,
  zIndex: 2,
})

const $rightAccessoryStyle: ThemedStyle<ViewStyle> = () => ({
  zIndex: 1,
})
const $leftAccessoryStyle: ThemedStyle<ViewStyle> = () => ({
  zIndex: 1,
})

const $viewPresets: Record<Presets, ThemedStyleArray<ViewStyle>> = {
  default: [
    $styles.row,
    $baseViewStyle,
    ({ colors }) => ({
      borderWidth: 1,
      borderColor: colors.elementColors.button.default.borderColor,
      backgroundColor: colors.elementColors.button.default.backgroundColor,
    }),
  ],
  filled: [
    $styles.row,
    $baseViewStyle,
    ({ colors }) => ({ backgroundColor: colors.elementColors.button.filled.backgroundColor }),
  ],
  reversed: [
    $styles.row,
    $baseViewStyle,
    ({ colors }) => ({ backgroundColor: colors.elementColors.button.reversed.backgroundColor }),
  ],
  destructive: [
    $styles.row,
    $baseViewStyle,
    ({ colors }) => ({ backgroundColor: colors.elementColors.button.destructive.backgroundColor }),
  ],
  icon: [$styles.row, $baseViewStyle],
}

const $textPresets: Record<Presets, ThemedStyleArray<TextStyle>> = {
  default: [$baseTextStyle],
  filled: [$baseTextStyle],
  reversed: [
    $baseTextStyle,
    ({ colors }) => ({ color: colors.elementColors.button.reversed.textColor }),
  ],
  destructive: [
    $baseTextStyle,
    ({ colors }) => ({ color: colors.elementColors.button.destructive.textColor }),
  ],
  icon: [$baseTextStyle],
}

const $pressedViewPresets: Record<Presets, ThemedStyle<ViewStyle>> = {
  default: ({ colors }) => ({
    backgroundColor: colors.elementColors.button.default.pressedBackgroundColor,
  }),
  filled: ({ colors }) => ({
    backgroundColor: colors.elementColors.button.filled.pressedBackgroundColor,
  }),
  reversed: ({ colors }) => ({
    backgroundColor: colors.elementColors.button.reversed.pressedBackgroundColor,
  }),
  destructive: ({ colors }) => ({
    backgroundColor: colors.elementColors.button.destructive.pressedBackgroundColor,
  }),
  icon: () => ({}),
}

const $pressedTextPresets: Record<Presets, ThemedStyle<ViewStyle>> = {
  default: () => ({ opacity: 0.9 }),
  filled: () => ({ opacity: 0.9 }),
  reversed: () => ({ opacity: 0.9 }),
  destructive: () => ({ opacity: 0.9 }),
  icon: () => ({}),
}

const $disabledViewStyle: ViewStyle = {
  opacity: 0.5,
}

const $contentContainer: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  justifyContent: "center",
  gap: spacing.xs,
}
