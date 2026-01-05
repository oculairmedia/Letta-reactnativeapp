import { spacing } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import { View, ViewStyle } from "react-native"
import { Button, ButtonProps } from "../Button"
import { Icon, IconTypes } from "../Icon"

export type ChatModeControlProps<T extends string> = {
  mode: T
  setMode: (m: T) => void
  MODES: readonly T[]
  MODE_ICONS: Record<T, IconTypes>
}

export function ChatModeControl<T extends string>({
  mode,
  setMode,
  MODES,
  MODE_ICONS,
}: ChatModeControlProps<T>) {
  const { theme } = useAppTheme()

  return (
    <View style={$segmentedControlContainer}>
      {MODES.map((m) => {
        // const Icon = MODE_ICONS[m] as React.ComponentType<{ size: number; color: string }>
        const selected = mode === m
        return (
          <Button key={m} onPress={() => setMode(m)} style={$segmentButton}>
            <Icon
              size={18}
              color={selected ? theme.colors.tint : theme.colors.text}
              icon={MODE_ICONS[m]}
            />
          </Button>
        )
      })}
    </View>
  )
}

const $segmentedControlContainer: ViewStyle = {
  flexDirection: "row",
}

const $segmentButton: ButtonProps["style"] = {
  paddingHorizontal: spacing.xs,
  paddingVertical: spacing.xs,
  borderWidth: 0,
}
