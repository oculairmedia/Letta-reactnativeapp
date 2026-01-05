export const palette = {
  neutral900: "#FFFFFF",
  neutral800: "#F4F2F1",
  neutral700: "#D7CEC9",
  neutral600: "#B6ACA6",
  neutral500: "#978F8A",
  neutral400: "#564E4A",
  neutral300: "#3C3836",
  neutral200: "#191015",
  neutral100: "#000000",

  primary600: "#F4E0D9",
  primary500: "#E8C1B4",
  primary400: "#DDA28E",
  primary300: "#D28468",
  primary200: "#C76542",
  primary100: "#A54F31",

  secondary500: "#DCDDE9",
  secondary400: "#BCC0D6",
  secondary300: "#9196B9",
  secondary200: "#626894",
  secondary100: "#41476E",

  accent500: "#FFEED4",
  accent400: "#FFE1B2",
  accent300: "#FDD495",
  accent200: "#FBC878",
  accent100: "#FFBB50",

  angry100: "#F2D6CD",
  angry500: "#C03403",

  overlay20: "rgba(25, 16, 21, 0.2)",
  overlay50: "rgba(25, 16, 21, 0.5)",

  success500: "#006B44",
  success100: "#DFF2E5",
} as const

export const colors = {
  palette,
  transparent: "rgba(0, 0, 0, 0)",
  transparent50: "rgba(255,255,255,0.05)",
  text: palette.neutral800,
  textDim: palette.neutral600,
  background: palette.neutral100,
  border: palette.neutral400,
  tint: palette.primary500,
  tintInactive: palette.neutral300,
  separator: palette.neutral300,
  error: palette.angry500,
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
        backgroundColor: palette.neutral800,
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
        default: palette.neutral100,
      },
      onBackgroundColor: {
        default: palette.secondary500,
      },
      knobOnBackgroundColor: {
        disabled: palette.neutral600,
        default: palette.neutral100,
      },
      knobOffBackgroundColor: {
        disabled: palette.neutral600,
        default: palette.neutral300,
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
        default: palette.secondary500,
      },
      iconTintColor: {
        disabled: palette.neutral600,
        on: palette.neutral100,
      },
      outerBorderColor: {
        disabled: palette.neutral400,
        off: palette.neutral800,
        on: palette.secondary500,
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
        on: palette.secondary500,
      },
      dotBackgroundColor: {
        disabled: palette.neutral600,
        default: palette.secondary500,
      },
    },
  },
} as const
