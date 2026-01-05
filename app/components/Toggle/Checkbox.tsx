import { useEffect, useRef, useCallback } from "react"
import { ImageStyle, Animated, StyleProp, View, ViewStyle } from "react-native"
import { $styles, spacing } from "../../theme"
import { Icon, IconTypes } from "../Icon"
import { $inputOuterBase, BaseToggleInputProps, ToggleProps, Toggle } from "./Toggle"
import { useAppTheme } from "@/utils/useAppTheme"

export interface CheckboxToggleProps extends Omit<ToggleProps<CheckboxInputProps>, "ToggleInput"> {
  /**
   * Optional style prop that affects the Image component.
   */
  inputDetailStyle?: ImageStyle
  /**
   * Checkbox-only prop that changes the icon used for the "on" state.
   */
  icon?: IconTypes
}

interface CheckboxInputProps extends BaseToggleInputProps<CheckboxToggleProps> {
  icon?: CheckboxToggleProps["icon"]
}
/**
 * @param {CheckboxToggleProps} props - The props for the `Checkbox` component.
 * @see [Documentation and Examples]{@link https://docs.infinite.red/ignite-cli/boilerplate/app/components/Checkbox}
 * @returns {JSX.Element} The rendered `Checkbox` component.
 */
export function Checkbox(props: CheckboxToggleProps) {
  const { icon, ...rest } = props
  const checkboxInput = useCallback(
    (toggleProps: CheckboxInputProps) => <CheckboxInput {...toggleProps} icon={icon} />,
    [icon],
  )
  return <Toggle accessibilityRole="checkbox" {...rest} ToggleInput={checkboxInput} />
}

function CheckboxInput(props: CheckboxInputProps) {
  const {
    on,
    status,
    disabled,
    icon = "Check",
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
    disabled && colors.elementColors.checkbox.offBackgroundColor.disabled,
    status === "error" && colors.errorBackground,
    colors.elementColors.checkbox.offBackgroundColor.default,
  ].filter(Boolean)[0]

  const outerBorderColor = [
    disabled && colors.elementColors.checkbox.outerBorderColor.disabled,
    status === "error" && colors.error,
    !on && colors.elementColors.checkbox.outerBorderColor.off,
    colors.elementColors.checkbox.outerBorderColor.on,
  ].filter(Boolean)[0]

  const onBackgroundColor = [
    disabled && colors.transparent,
    status === "error" && colors.errorBackground,
    colors.elementColors.checkbox.onBackgroundColor.default,
  ].filter(Boolean)[0]

  const iconTintColor = [
    disabled && colors.elementColors.checkbox.iconTintColor.disabled,
    status === "error" && colors.error,
    colors.elementColors.checkbox.iconTintColor.on,
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
        <Icon
          style={[
            $checkboxDetail,
            !!iconTintColor && { tintColor: iconTintColor },
            $detailStyleOverride,
          ]}
          icon={icon ? icon : "Check"}
          color={iconTintColor}
        />
      </Animated.View>
    </View>
  )
}

const $checkboxDetail: ImageStyle = {
  width: 20,
  height: 20,
  resizeMode: "contain",
}

const $inputOuter: StyleProp<ViewStyle> = [
  $inputOuterBase,
  { borderRadius: spacing.checkboxRadius },
]
