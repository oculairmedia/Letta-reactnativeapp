import { useIsDeveloper } from "@/hooks/is-developer"
import { getAgentsQueryKey } from "@/hooks/use-agents"
import { navigate, type RouteName } from "@/navigators"
import { useLettaClient } from "@/providers/LettaProvider"
import { useHeader } from "@/utils/useHeader"
import { useNavigation, useRoute } from "@react-navigation/native"
import { useQueryClient } from "@tanstack/react-query"
import { useMemo } from "react"
import { HeaderProps } from "../Header"
import type { IconTypes } from "../Icon"

type LeftProps =
  | Pick<HeaderProps, "LeftActionComponent">
  | Pick<
      HeaderProps,
      | "leftIcon"
      | "leftIconColor"
      | "onLeftPress"
      | "leftText"
      | "leftTx"
      | "leftTxOptions"
      | "leftDisabled"
    >

type RightProps =
  | Pick<HeaderProps, "RightActionComponent">
  | Pick<
      HeaderProps,
      | "rightIcon"
      | "rightIconColor"
      | "onRightPress"
      | "rightText"
      | "rightTx"
      | "rightTxOptions"
      | "rightDisabled"
    >

type TitleProps =
  | Pick<HeaderProps, "titleComponent">
  | Pick<
      HeaderProps,
      "title" | "titleTx" | "titleTxOptions" | "titleContainerStyle" | "titleStyle" | "titleMode"
    >

type RouteProps = {
  leftProps: LeftProps
  rightProps: RightProps
  titleProps: TitleProps
}
const defaultDeps: any[] = []
const defaultHeaderProps: HeaderProps = {}

function mergeHeaderProps(incomingProps: HeaderProps, overrideProps: HeaderProps): HeaderProps {
  let props = { ...defaultHeaderProps, ...incomingProps }
  const keys = Object.keys(incomingProps)
  // If any right prop has the word "Right" in it, remove it
  if (keys.some((key) => key.includes("Right"))) {
    const { RightActionComponent: _, ...rest } = props
    props = rest
  }
  // If any left prop has the word "Left" in it, remove it
  if (keys.some((key) => key.includes("Left"))) {
    const { LeftActionComponent: _, ...rest } = props
    props = rest
  }
  // Otherwise, use default RightActionComponent
  return { ...props, ...overrideProps }
}

function createHeaderProps(props: RouteProps): RouteProps {
  return props
}

export function useLettaHeader(headerProps: HeaderProps = {}, deps: any[] = defaultDeps) {
  const navigation = useNavigation()
  const route = useRoute()
  const queryClient = useQueryClient()
  const { resetConfig } = useLettaClient()

  const isDeveloper = useIsDeveloper()

  const routeProps: RouteProps = useMemo(() => {
    const routeName = route.name as RouteName
    switch (routeName) {
      case "AgentList":
        return createHeaderProps({
          leftProps: {
            leftIcon: "x" satisfies IconTypes,
            onLeftPress: () => {
              resetConfig()
              queryClient.clear()
            },
          },
          rightProps: {
            rightIcon: "RotateCw" satisfies IconTypes,
            onRightPress: () => {
              queryClient.invalidateQueries({ queryKey: getAgentsQueryKey() })
            },
          },
          titleProps: {},
        })
      case "Settings":
        return createHeaderProps({
          leftProps: {
            leftIcon: "back" satisfies IconTypes,
            onLeftPress: () => navigation.goBack(),
          },
          rightProps: {},
          titleProps: {
            title: "Agent Settings",
          },
        })
      case "Chat":
        return createHeaderProps({
          leftProps: {
            leftIcon: "LogOut" satisfies IconTypes,
            onLeftPress: () => navigation.goBack(),
          },
          rightProps: {
            rightIcon: "Brain" satisfies IconTypes,
            onRightPress: () => (navigation as any)?.toggleDrawer?.(),
          },
          titleProps: {
            title: "Chat",
          },
        })
      case "Studio":
        return createHeaderProps({
          leftProps: {
            leftIcon: "back" satisfies IconTypes,
            onLeftPress: () => navigation.goBack(),
          },
          rightProps: {},
          titleProps: {
            title: "Studio",
          },
        })
      case "Welcome":
        return createHeaderProps({
          leftProps: {},
          rightProps: isDeveloper
            ? {
                rightIcon: "settings" satisfies IconTypes,
                onRightPress: () => navigate("Developer"),
              }
            : {},
          titleProps: {},
        })
      default:
        return createHeaderProps({
          leftProps: {
            leftIcon: "back" satisfies IconTypes,
            onLeftPress: () => navigation.goBack(),
          },
          rightProps: {},
          titleProps: {},
        })
    }
  }, [navigation, queryClient, resetConfig, route.name, isDeveloper])

  useHeader(
    mergeHeaderProps(
      {
        ...routeProps.titleProps,
        ...routeProps.leftProps,
        ...routeProps.rightProps,
      },
      headerProps,
    ),
    [routeProps, navigation, headerProps, ...deps],
  )
  return null
}
