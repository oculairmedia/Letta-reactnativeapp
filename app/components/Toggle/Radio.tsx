import { useAppTheme } from "@/utils/useAppTheme"
import { useEffect, useRef } from "react"
import { Animated, StyleProp, View, ViewStyle } from "react-native"
import { $styles, spacing } from "../../theme"
import { $inputOuterBase, BaseToggleInputProps, Toggle, ToggleProps } from "./Toggle"

export interface RadioToggleProps extends Omit<ToggleProps<RadioInputProps>, "ToggleInput"> {
  /**
   * Optional style prop that affects the dot View.
   */
  inputDetailStyle?: ViewStyle
}

interface RadioInputProps extends BaseToggleInputProps<RadioToggleProps> {}

/**
 * @param {RadioToggleProps} props - The props for the `Radio` component.
 * @see [Documentation and Examples]{@link https://docs.infinite.red/ignite-cli/boilerplate/app/components/Radio}
 * @returns {JSX.Element} The rendered `Radio` component.
 */
export function Radio(props: RadioToggleProps) {
  return <Toggle accessibilityRole="radio" {...props} ToggleInput={RadioInput} />
}

function RadioInput(props: RadioInputProps) {
  const {
    on,
    status,
    disabled,
    outerStyle: $outerStyleOverride,
    innerStyle: $innerStyleOverride,
    detailStyle: $detailStyleOverride,
  } = props

  const {
    theme: { colors },
  } = useAppTheme()

  const opacity = useRef(new Animated.Value(0))

  useEffect(() => {
    Animated.timing(opacity.current, {
      toValue: on ? 1 : 0,
      duration: 300,
      useNativeDriver: true,
    }).start()
  }, [on])

  const offBackgroundColor = [
    disabled && colors.elementColors.radio.offBackgroundColor.disabled,
    status === "error" && colors.errorBackground,
    colors.elementColors.radio.offBackgroundColor.default,
  ].filter(Boolean)[0]

  const outerBorderColor = [
    disabled && colors.elementColors.radio.outerBorderColor.disabled,
    status === "error" && colors.error,
    !on && colors.elementColors.radio.outerBorderColor.off,
    colors.elementColors.radio.outerBorderColor.on,
  ].filter(Boolean)[0]

  const onBackgroundColor = [
    disabled && colors.transparent,
    status === "error" && colors.errorBackground,
    colors.elementColors.radio.onBackgroundColor.default,
  ].filter(Boolean)[0]

  const dotBackgroundColor = [
    disabled && colors.elementColors.radio.dotBackgroundColor.disabled,
    status === "error" && colors.error,
    colors.elementColors.radio.dotBackgroundColor.default,
  ].filter(Boolean)[0]

  return (
    <View
      style={[
        $inputOuter,
        { backgroundColor: offBackgroundColor, borderColor: outerBorderColor },
        $outerStyleOverride,
      ]}
    >
      <Animated.View
        style={[
          $styles.toggleInner,
          { backgroundColor: onBackgroundColor },
          $innerStyleOverride,
          { opacity: opacity.current },
        ]}
      >
        <View
          style={[$radioDetail, { backgroundColor: dotBackgroundColor }, $detailStyleOverride]}
        />
      </Animated.View>
    </View>
  )
}

const $radioDetail: ViewStyle = {
  width: 12,
  height: 12,
  borderRadius: spacing.radioRadius,
}

const $inputOuter: StyleProp<ViewStyle> = [$inputOuterBase, { borderRadius: spacing.radioRadius }]
