import { useAppTheme } from "@/utils/useAppTheme"
import { icons, LucideIcon, LucideProps } from "lucide-react-native"
import { ComponentType, createElement, useMemo } from "react"
import {
  I18nManager,
  StyleProp,
  TouchableOpacity,
  TouchableOpacityProps,
  View,
  ViewProps,
  ViewStyle,
} from "react-native"

type lucideIconTypes = keyof typeof icons
type registryIconTypes = keyof typeof iconRegistry
export type IconTypes = lucideIconTypes | registryIconTypes

export interface IconProps extends TouchableOpacityProps {
  /**
   * The name of the icon
   */
  icon: IconTypes

  /**
   * An optional tint color for the icon
   */
  color?: string

  /**
   * An optional size for the icon. If not provided, the icon will be sized to the icon's resolution.
   */
  size?: number

  /**
   * Style overrides for the icon image
   */
  style?: LucideProps["style"]

  /**
   * Style overrides for the icon container
   */
  containerStyle?: StyleProp<ViewStyle>

  /**
   * An optional function to be called when the icon is pressed
   */
  onPress?: TouchableOpacityProps["onPress"]

  /**
   * An optional disabled state for the icon
   */
  disabled?: boolean
}

/**
 * A component to render a registered icon.
 * It is wrapped in a <TouchableOpacity /> if `onPress` is provided, otherwise a <View />.
 * Supports both local icons from iconRegistry and Lucide icons.
 * @see [Documentation and Examples]{@link https://docs.infinite.red/ignite-cli/boilerplate/app/components/Icon/}
 * @param {IconProps} props - The props for the `Icon` component.
 * @returns {JSX.Element} The rendered `Icon` component.
 */
export function Icon(props: IconProps) {
  const { icon, color, size, containerStyle: $containerStyleOverride, ...WrapperProps } = props

  const isPressable = !!WrapperProps.onPress
  const Wrapper = (WrapperProps?.onPress ? TouchableOpacity : View) as ComponentType<
    TouchableOpacityProps | ViewProps
  >

  const { theme } = useAppTheme()

  // Check if the icon exists in Lucide icons
  const isLucideIcon = useMemo(() => icon in icons, [icon])
  const isRegistryIcon = useMemo(() => icon in iconRegistry, [icon])

  if (!isLucideIcon && !isRegistryIcon) {
    console.warn(`Icon "${icon}" not found in Lucide icons or registry. Falling back to ladybug.`)
  }

  const IconComponent = isLucideIcon
    ? (icons as Record<string, LucideIcon>)[icon]
    : // A polyfill for existing icons that are not in Lucide | TODO: Remove this once we have a proper icon registry
      isRegistryIcon
      ? (icons as Record<string, LucideIcon>)[iconRegistry[icon as registryIconTypes]]
      : // A polyfill for existing icons that are not in Lucide | TODO: Remove this once we have a proper icon registry
        (icons as Record<string, LucideIcon>)["Bug"]

  return (
    <Wrapper
      accessibilityRole={isPressable ? "imagebutton" : undefined}
      {...WrapperProps}
      style={$containerStyleOverride}
      disabled={props.disabled}
    >
      {createElement(IconComponent, {
        color: color ?? theme.colors.text,
        size: size ?? 24,
        style: props.style,
      })}
    </Wrapper>
  )
}

export const iconRegistry: Record<
  | "bell"
  | "back"
  | "caretLeft"
  | "caretRight"
  | "check"
  | "hidden"
  | "ladybug"
  | "lock"
  | "menu"
  | "more"
  | "settings"
  | "view"
  | "x",
  lucideIconTypes
> = {
  bell: "Bell",
  back: I18nManager.isRTL ? "ArrowRight" : "ArrowLeft",
  caretLeft: "ArrowLeft",
  caretRight: "ArrowRight",
  check: "Check",
  hidden: "EyeOff",
  ladybug: "Bug",
  lock: "Lock",
  menu: "Menu",
  more: "Ellipsis",
  settings: "Settings",
  view: "Eye",
  x: "X",
}
