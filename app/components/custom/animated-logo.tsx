import { useEffect, useState } from "react"
import { Image, ImageStyle, Keyboard, useWindowDimensions, ViewStyle } from "react-native"

import Animated from "react-native-reanimated"

import { useAnimatedStyle, useSharedValue, withTiming } from "react-native-reanimated"
const welcomeLogo = require("../../../assets/images/logo.png")

function useKeyboardOpen() {
  const [keyboardOpen, setKeyboardOpen] = useState(false)
  useEffect(() => {
    const off = Keyboard.addListener("keyboardWillShow", () => {
      setKeyboardOpen(true)
    })
    const off2 = Keyboard.addListener("keyboardWillHide", () => {
      setKeyboardOpen(false)
    })
    return () => {
      off.remove()
      off2.remove()
    }
  }, [])

  return keyboardOpen
}

export function AnimatedLogo() {
  const isOpen = useKeyboardOpen()
  const translateY = useSharedValue(0)
  const { height } = useWindowDimensions()
  useEffect(() => {
    translateY.value = withTiming(isOpen ? height * -0.25 : 0, {
      duration: 200,
    })
  }, [isOpen, translateY, height])

  const animatedStyle = useAnimatedStyle(() => {
    return {
      transform: [{ translateY: translateY.value }],
    }
  })

  return (
    <Animated.View style={[$animatedLogo, animatedStyle]}>
      <Image source={welcomeLogo} style={$animatedLogoImage} />
    </Animated.View>
  )
}

const $animatedLogo: ViewStyle = {
  justifyContent: "center",
  alignItems: "center",
  flex: 1,
}

const $animatedLogoImage: ImageStyle = {
  width: 128,
  height: 128,
  opacity: 0.3,
  resizeMode: "contain",
}
