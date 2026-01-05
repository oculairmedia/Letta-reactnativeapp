import { StyleProp, TextStyle, View, ViewStyle } from "react-native"

import { translate } from "@/i18n/translate"
import type { ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import { Button, ButtonProps } from "./Button"
import { Icon, IconTypes, IconProps } from "./Icon"
import { Text, TextProps } from "./Text"

interface EmptyStateProps {
  /**
   * An optional prop that specifies the text/image set to use for the empty state.
   */
  preset?: "generic"
  /**
   * Style override for the container.
   */
  style?: StyleProp<ViewStyle>
  /**
   * An icon to be displayed above the heading.
   */
  icon?: IconTypes
  /**
   * Size of the icon. Defaults to 48.
   */
  iconSize?: number
  /**
   * Style overrides for image.
   */
  iconStyle?: StyleProp<IconProps["style"]>
  /**
   * Pass any additional props directly to the Icon component.
   */
  IconProps?: Omit<IconProps, "source">
  /**
   * The heading text to display if not using `headingTx`.
   */
  heading?: TextProps["text"]
  /**
   * Heading text which is looked up via i18n.
   */
  headingTx?: TextProps["tx"]
  /**
   * Optional heading options to pass to i18n. Useful for interpolation
   * as well as explicitly setting locale or translation fallbacks.
   */
  headingTxOptions?: TextProps["txOptions"]
  /**
   * Style overrides for heading text.
   */
  headingStyle?: StyleProp<TextStyle>
  /**
   * Pass any additional props directly to the heading Text component.
   */
  HeadingTextProps?: TextProps
  /**
   * The content text to display if not using `contentTx`.
   */
  content?: TextProps["text"]
  /**
   * Content text which is looked up via i18n.
   */
  contentTx?: TextProps["tx"]
  /**
   * Optional content options to pass to i18n. Useful for interpolation
   * as well as explicitly setting locale or translation fallbacks.
   */
  contentTxOptions?: TextProps["txOptions"]
  /**
   * Style overrides for content text.
   */
  contentStyle?: StyleProp<TextStyle>
  /**
   * Pass any additional props directly to the content Text component.
   */
  ContentTextProps?: TextProps
  /**
   * The button text to display if not using `buttonTx`.
   */
  button?: TextProps["text"]
  /**
   * Button text which is looked up via i18n.
   */
  buttonTx?: TextProps["tx"]
  /**
   * Optional button options to pass to i18n. Useful for interpolation
   * as well as explicitly setting locale or translation fallbacks.
   */
  buttonTxOptions?: TextProps["txOptions"]
  /**
   * Style overrides for button.
   */
  buttonStyle?: ButtonProps["style"]
  /**
   * Style overrides for button text.
   */
  buttonTextStyle?: ButtonProps["textStyle"]
  /**
   * Called when the button is pressed.
   */
  buttonOnPress?: ButtonProps["onPress"]
  /**
   * Pass any additional props directly to the Button component.
   */
  ButtonProps?: ButtonProps
  /**
   * Whether the button is loading.
   */
  loading?: boolean
}

interface EmptyStatePresetItem {
  icon: IconTypes
  heading: TextProps["text"]
  content: TextProps["text"]
  button: TextProps["text"]
}

/**
 * A component to use when there is no data to display. It can be utilized to direct the user what to do next.
 * @see [Documentation and Examples]{@link https://docs.infinite.red/ignite-cli/boilerplate/app/components/EmptyState/}
 * @param {EmptyStateProps} props - The props for the `EmptyState` component.
 * @returns {JSX.Element} The rendered `EmptyState` component.
 */
export function EmptyState(props: EmptyStateProps) {
  const {
    theme,
    themed,
    theme: { spacing },
  } = useAppTheme()

  const EmptyStatePresets = {
    generic: {
      icon: "Frown" as IconTypes,
      heading: translate("emptyStateComponent:generic.heading"),
      content: translate("emptyStateComponent:generic.content"),
      button: translate("emptyStateComponent:generic.button"),
    } as EmptyStatePresetItem,
  } as const

  const preset = EmptyStatePresets[props.preset ?? "generic"]

  const {
    button = preset.button,
    buttonTx,
    buttonOnPress,
    buttonTxOptions,
    content = preset.content,
    contentTx,
    contentTxOptions,
    heading = preset.heading,
    headingTx,
    headingTxOptions,
    icon = preset.icon,
    iconSize = 48,
    style: $containerStyleOverride,
    buttonStyle: $buttonStyleOverride,
    buttonTextStyle: $buttonTextStyleOverride,
    contentStyle: $contentStyleOverride,
    headingStyle: $headingStyleOverride,
    iconStyle: $imageStyleOverride,
    ButtonProps,
    ContentTextProps,
    HeadingTextProps,
    IconProps,
    loading,
  } = props

  const isIconPresent = !!icon
  const isHeadingPresent = !!(heading || headingTx)
  const isContentPresent = !!(content || contentTx)
  const isButtonPresent = !!(button || buttonTx)

  const $containerStyles = [$containerStyleOverride]
  const $iconStyles = [
    $image,
    isHeadingPresent || isContentPresent || isButtonPresent ? { marginBottom: spacing.xxxs } : null,
    $imageStyleOverride,
    IconProps?.style,
  ].filter(Boolean) as IconProps["style"]
  const $headingStyles = [
    themed($heading),
    isIconPresent && { marginTop: spacing.xxxs },
    (isContentPresent || isButtonPresent) && { marginBottom: spacing.xxxs },
    $headingStyleOverride,
    HeadingTextProps?.style,
  ]
  const $contentStyles = [
    themed($content),
    (isIconPresent || isHeadingPresent) && { marginTop: spacing.xxxs },
    isButtonPresent && { marginBottom: spacing.xxxs },
    $contentStyleOverride,
    ContentTextProps?.style,
  ]
  const $buttonStyles = [
    (isIconPresent || isHeadingPresent || isContentPresent) && { marginTop: spacing.xl },
    $buttonStyleOverride,
    ButtonProps?.style,
  ]

  return (
    <View style={$containerStyles}>
      {isIconPresent && (
        <Icon
          icon={icon}
          {...IconProps}
          style={$iconStyles}
          color={theme.colors.text}
          size={iconSize}
        />
      )}

      {isHeadingPresent && (
        <Text
          preset="subheading"
          text={heading}
          tx={headingTx}
          txOptions={headingTxOptions}
          {...HeadingTextProps}
          style={$headingStyles}
        />
      )}

      {isContentPresent && (
        <Text
          text={content}
          tx={contentTx}
          txOptions={contentTxOptions}
          {...ContentTextProps}
          style={$contentStyles}
        />
      )}

      {isButtonPresent && (
        <Button
          onPress={buttonOnPress}
          text={button}
          tx={buttonTx}
          txOptions={buttonTxOptions}
          textStyle={$buttonTextStyleOverride}
          {...ButtonProps}
          style={$buttonStyles}
          loading={loading}
        />
      )}
    </View>
  )
}

const $image: IconProps["style"] = { alignSelf: "center" }
const $heading: ThemedStyle<TextStyle> = ({ spacing }) => ({
  textAlign: "center",
  paddingHorizontal: spacing.lg,
})
const $content: ThemedStyle<TextStyle> = ({ spacing }) => ({
  textAlign: "center",
  paddingHorizontal: spacing.lg,
})
