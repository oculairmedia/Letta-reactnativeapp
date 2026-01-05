import { TouchableOpacity, View, ViewStyle } from "react-native"
import { Text } from "@/components/Text"
import { Check, X } from "lucide-react-native"
import { colors, spacing } from "@/theme"
import { BlockHeaderProps } from "./types"

export function BlockHeader({ label, isModified, onSave, onReset }: BlockHeaderProps) {
  return (
    <View style={$container}>
      <Text preset="bold" text={label} style={$label} />
      {isModified && (
        <View style={$actions}>
          <TouchableOpacity onPress={onSave} style={$actionButton}>
            <Check size={16} color={colors.palette.primary200} />
          </TouchableOpacity>
          <TouchableOpacity onPress={onReset} style={$actionButton}>
            <X size={16} color={colors.palette.angry500} />
          </TouchableOpacity>
        </View>
      )}
    </View>
  )
}

const $container: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
}

const $label: ViewStyle = {}

const $actions: ViewStyle = {
  flexDirection: "row",
  gap: spacing.xs,
}

const $actionButton: ViewStyle = {
  padding: spacing.xs,
}
