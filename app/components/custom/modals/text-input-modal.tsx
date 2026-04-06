import { Icon } from "@/components/Icon"
import { Text } from "@/components/Text"
import { spacing, ThemedStyle } from "@/theme"
import { useAppTheme } from "@/utils/useAppTheme"
import { FC, useEffect, useState } from "react"
import {
  Modal,
  Pressable,
  TextInput,
  TextStyle,
  TouchableOpacity,
  View,
  ViewStyle,
} from "react-native"

interface TextInputModalProps {
  visible: boolean
  title: string
  message?: string
  defaultValue?: string
  placeholder?: string
  onSubmit: (value: string) => void
  onDismiss: () => void
  submitText?: string
  cancelText?: string
}

export const TextInputModal: FC<TextInputModalProps> = ({
  visible,
  title,
  message,
  defaultValue = "",
  placeholder,
  onSubmit,
  onDismiss,
  submitText = "Save",
  cancelText = "Cancel",
}) => {
  const { themed, theme } = useAppTheme()
  const { colors } = theme
  const [value, setValue] = useState(defaultValue)

  // Reset value when modal opens with a new defaultValue
  useEffect(() => {
    if (visible) {
      setValue(defaultValue)
    }
  }, [visible, defaultValue])

  const handleSubmit = () => {
    onSubmit(value)
  }

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onDismiss}>
      <Pressable style={$modalOverlay} onPress={onDismiss}>
        <Pressable style={themed($modalContent)} onPress={(e) => e.stopPropagation()}>
          <View style={$modalHeader}>
            <Text preset="bold">{title}</Text>
            <TouchableOpacity onPress={onDismiss}>
              <Icon icon="X" size={20} color={colors.text} />
            </TouchableOpacity>
          </View>

          {message && (
            <Text size="sm" style={themed($messageText)}>
              {message}
            </Text>
          )}

          <TextInput
            style={themed($textInput)}
            value={value}
            onChangeText={setValue}
            placeholder={placeholder}
            placeholderTextColor={colors.textDim}
            autoFocus
            multiline
          />

          <View style={$buttonRow}>
            <TouchableOpacity style={themed($cancelButton)} onPress={onDismiss}>
              <Text size="sm" style={themed($cancelText)}>
                {cancelText}
              </Text>
            </TouchableOpacity>
            <TouchableOpacity style={themed($submitButton)} onPress={handleSubmit}>
              <Text size="sm" style={$submitText}>
                {submitText}
              </Text>
            </TouchableOpacity>
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  )
}

const $modalOverlay: ViewStyle = {
  flex: 1,
  backgroundColor: "rgba(0,0,0,0.5)",
  justifyContent: "center",
  alignItems: "center",
  padding: spacing.lg,
}

const $modalContent: ThemedStyle<ViewStyle> = ({ colors }) => ({
  backgroundColor: colors.background,
  borderRadius: 12,
  width: "100%",
  maxWidth: 400,
  padding: spacing.md,
})

const $modalHeader: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
  marginBottom: spacing.sm,
}

const $messageText: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
  marginBottom: spacing.sm,
})

const $textInput: ThemedStyle<TextStyle> = ({ colors }) => ({
  backgroundColor: colors.palette.overlay20,
  color: colors.text,
  borderRadius: 8,
  paddingHorizontal: spacing.sm,
  paddingVertical: spacing.sm,
  fontSize: 16,
  minHeight: 80,
  textAlignVertical: "top",
  marginBottom: spacing.md,
})

const $buttonRow: ViewStyle = {
  flexDirection: "row",
  justifyContent: "flex-end",
  gap: spacing.sm,
}

const $cancelButton: ThemedStyle<ViewStyle> = ({ colors }) => ({
  paddingVertical: spacing.xs,
  paddingHorizontal: spacing.md,
  borderRadius: 8,
  borderWidth: 1,
  borderColor: colors.palette.overlay20,
})

const $cancelText: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
})

const $submitButton: ThemedStyle<ViewStyle> = ({ colors }) => ({
  paddingVertical: spacing.xs,
  paddingHorizontal: spacing.md,
  borderRadius: 8,
  backgroundColor: colors.tint,
})

const $submitText: TextStyle = {
  color: "#fff",
  fontWeight: "500",
}
