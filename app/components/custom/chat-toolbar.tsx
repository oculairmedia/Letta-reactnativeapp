"use client"

import { useAppTheme } from "@/utils/useAppTheme"
import { ArrowUp } from "lucide-react-native"
import { useCallback, useRef, useState } from "react"
import {
  NativeSyntheticEvent,
  TextInput,
  TextInputProps,
  TextInputSubmitEditingEventData,
  View,
  ViewStyle,
} from "react-native"
import { TextField } from "../TextField"
import { Button } from "../Button"

interface ChatToolbarInnerProps {
  onSubmit: (value: string) => void
  disabled?: boolean
}

export function ChatToolbarInner({ onSubmit, disabled = false }: ChatToolbarInnerProps) {
  const [inputValue, setInputValue] = useState("")
  const textInput = useRef<TextInput>(null)

  const onSubmitMessage = useCallback(
    (value: string) => {
      if (value.trim() === "") {
        textInput.current?.blur()
        return
      }

      setTimeout(() => {
        textInput.current?.clear()
      })

      onSubmit(value.trim())
      setInputValue("")
    },
    [textInput, onSubmit],
  )

  const onSubmitEditing = useCallback(
    (e: NativeSyntheticEvent<TextInputSubmitEditingEventData>) => {
      onSubmitMessage(e.nativeEvent.text)
    },
    [onSubmitMessage],
  )

  const { theme } = useAppTheme()

  return (
    <View style={$toolbarContainer}>
      <View style={$toolbarContainerInner}>
        <TextField
          ref={textInput}
          containerStyle={$sendInput}
          onChangeText={setInputValue}
          keyboardAppearance={theme.isDark ? "dark" : "light"}
          inputWrapperStyle={$inputWrapper}
          cursorColor={theme.colors.tint}
          returnKeyType="send"
          blurOnSubmit={false}
          selectionHandleColor={theme.colors.tint}
          selectionColor={theme.colors.tint}
          style={[{ color: theme.colors.tint }, disabled && $disabledSendInput]}
          placeholder="Ask anything"
          autoCapitalize="none"
          autoCorrect={false}
          placeholderTextColor={theme.colors.textDim}
          onSubmitEditing={onSubmitEditing}
          multiline
          textAlignVertical="top"
        />

        <SendButton enabled={!!inputValue.length} onPress={() => onSubmitMessage(inputValue)} />
      </View>
    </View>
  )
}

const $toolbarContainer: ViewStyle = {
  backgroundColor: "transparent",
  position: "relative",
  gap: 8,
  pointerEvents: "box-none",
}

const $toolbarContainerInner: ViewStyle = {
  flexDirection: "row",
  gap: 8,
  alignItems: "stretch",
}

const $inputWrapper: ViewStyle = {
  minHeight: 0,
}

const $sendInput: TextInputProps["style"] = {
  flex: 1,
  pointerEvents: "auto",
}

const $disabledSendInput: TextInputProps["style"] = {
  pointerEvents: "none",
}

function SendButton({ enabled, onPress }: { enabled?: boolean; onPress: () => void }) {
  const { theme } = useAppTheme()
  return (
    <Button
      style={[
        $sendButton,
        { backgroundColor: theme.colors.background },
        !enabled && $disabledSendButton,
      ]}
      disabled={!enabled}
      onPress={onPress}
    >
      <ArrowUp size={26} stroke={theme.colors.tint} />
    </Button>
  )
}

const $sendButton: ViewStyle = {
  justifyContent: "center",
  alignItems: "center",
  aspectRatio: 1,
  minWidth: 0,
  minHeight: 0,
  height: 40,
}

const $disabledSendButton: ViewStyle = {
  opacity: 0.5,
}
