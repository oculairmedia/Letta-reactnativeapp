import { StyleProp, View, ViewStyle } from "react-native"

export function ChatContainer({
  children,
  style,
}: {
  children: React.ReactNode
  style?: StyleProp<ViewStyle>
}) {
  return (
    <View style={[$chatContainer, style]}>
      <View style={[$chatContainerInner, style]}>{children}</View>
    </View>
  )
}

const $chatContainer: ViewStyle = {
  alignItems: "stretch",
}

const $chatContainerInner: ViewStyle = {
  flex: 1,
  flexGrow: 1,
}
