import { useAppTheme } from "@/utils/useAppTheme"
import { createNativeStackNavigator, NativeStackScreenProps } from "@react-navigation/native-stack"
import { AgentTabNavigator } from "./AgentTabNavigator"

export type AgentNavigatorGroupParamList = {
  AgentTab: undefined
}
export type AgentNavigatorGroupScreenProps<T extends keyof AgentNavigatorGroupParamList> =
  NativeStackScreenProps<AgentNavigatorGroupParamList, T>

const Stack = createNativeStackNavigator<AgentNavigatorGroupParamList>()
export const AgentNavigatorGroup = () => {
  const {
    theme: { colors },
  } = useAppTheme()
  return (
    <Stack.Navigator
      initialRouteName="AgentTab"
      screenOptions={{
        headerShown: false,
        navigationBarColor: colors.background,
        contentStyle: {
          backgroundColor: colors.background,
        },
      }}
    >
      <Stack.Screen name="AgentTab" component={AgentTabNavigator} />
    </Stack.Navigator>
  )
}
