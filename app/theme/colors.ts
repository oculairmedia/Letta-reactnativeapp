export const palette = {
  neutral100: "#FFFFFF",
  neutral200: "#F4F2F1",
  neutral300: "#D7CEC9",
  neutral400: "#B6ACA6",
  neutral500: "#978F8A",
  neutral600: "#564E4A",
  neutral700: "#3C3836",
  neutral800: "#191015",
  neutral900: "#000000",

  primary100: "#F4E0D9",
  primary200: "#E8C1B4",
  primary300: "#DDA28E",
  primary400: "#D28468",
  primary500: "#C76542",
  primary600: "#A54F31",

  secondary100: "#DCDDE9",
  secondary200: "#BCC0D6",
  secondary300: "#9196B9",
  secondary400: "#626894",
  secondary500: "#41476E",

  accent100: "#FFEED4",
  accent200: "#FFE1B2",
  accent300: "#FDD495",
  accent400: "#FBC878",
  accent500: "#FFBB50",

  angry100: "#F2D6CD",
  angry500: "#C03403",

  overlay20: "rgba(25, 16, 21, 0.2)",
  overlay50: "rgba(25, 16, 21, 0.5)",

  success500: "#008000",
  success100: "#DFF2E5",
} as const

export const colors = {
  /**
   * The palette is available to use, but prefer using the name.
   * This is only included for rare, one-off cases. Try to use
   * semantic names as much as possible.
   */
  palette,
  /**
   * A helper for making something see-thru.
   */
  transparent: "rgba(0, 0, 0, 0)",
  /**
   * alpha 0.5
   */
  transparent50: "rgba(0,0,0,0.05)",
  /**
   * The default text color in many components.
   */
  text: palette.neutral800,
  /**
   * Secondary text information.
   */
  textDim: palette.neutral600,
  /**
   * The default color of the screen background.
   */
  background: palette.neutral100,
  /**
   * The default border color.
   */
  border: palette.neutral400,
  /**
   * The main tinting color.
   */
  tint: palette.primary500,
  /**
   * The inactive tinting color.
   */
  tintInactive: palette.neutral300,
  /**
   * A subtle color used for lines.
   */
  separator: palette.neutral300,
  /**
   * Error messages.
   */
  error: palette.angry500,
  /**
   * Error Background.
   */
  errorBackground: palette.angry100,
  /**
   * Element colors.
   */
  elementColors: {
    button: {
      default: {
        backgroundColor: palette.neutral100,
        pressedBackgroundColor: palette.neutral200,
        borderColor: palette.neutral400,
      },
      filled: {
        backgroundColor: palette.neutral300,
        pressedBackgroundColor: palette.neutral400,
      },
      reversed: {
        backgroundColor: palette.neutral800,
        pressedBackgroundColor: palette.neutral700,
        textColor: palette.neutral100,
      },
      destructive: {
        backgroundColor: palette.primary600,
        pressedBackgroundColor: palette.primary500,
        textColor: palette.neutral100,
      },
    },
    card: {
      shadowColor: palette.neutral800,
      default: {
        backgroundColor: palette.neutral100,
        borderColor: palette.neutral300,
        heading: undefined,
        content: undefined,
        footer: undefined,
      },
      reversed: {
        backgroundColor: palette.neutral900,
        borderColor: palette.neutral500,
        heading: palette.neutral100,
        content: palette.neutral100,
        footer: palette.neutral100,
      },
    },
    textField: {
      backgroundColor: palette.neutral100,
      borderColor: palette.neutral300,
    },
    switch: {
      offBackgroundColor: {
        disabled: palette.neutral400,
        default: palette.neutral300,
      },
      onBackgroundColor: {
        default: palette.neutral900,
      },
      knobOnBackgroundColor: {
        disabled: palette.neutral600,
        default: palette.neutral100,
      },
      knobOffBackgroundColor: {
        disabled: palette.neutral600,
        default: palette.neutral200,
      },
      color: {
        disabled: palette.neutral600,
        off: palette.secondary500,
        on: palette.neutral100,
      },
    },
    checkbox: {
      offBackgroundColor: {
        disabled: palette.neutral400,
        default: palette.neutral200,
      },
      onBackgroundColor: {
        default: palette.neutral900,
      },
      iconTintColor: {
        disabled: palette.neutral600,
        on: palette.neutral100,
      },
      outerBorderColor: {
        disabled: palette.neutral400,
        off: palette.neutral800,
        on: palette.neutral900,
      },
    },
    radio: {
      offBackgroundColor: {
        disabled: palette.neutral400,
        default: palette.neutral200,
      },
      onBackgroundColor: {
        default: palette.neutral100,
      },
      outerBorderColor: {
        disabled: palette.neutral400,
        off: palette.neutral800,
        on: palette.neutral900,
      },
      dotBackgroundColor: {
        disabled: palette.neutral600,
        default: palette.neutral900,
      },
    },
  },
} as const
