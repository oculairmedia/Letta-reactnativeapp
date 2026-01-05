import { AgentDrawerContent } from "@/components/custom/agent-drawer"
import { AgentProvider } from "@/providers/AgentProvider"
import { createDrawerNavigator, DrawerScreenProps } from "@react-navigation/drawer"
import { CompositeScreenProps, NavigatorScreenParams } from "@react-navigation/native"
import { AgentTabParamList } from "."
import { AgentNavigatorGroup } from "./AgentNavigator"
import { AppStackParamList, AppStackScreenProps } from "./AppNavigator"
export type AgentDrawerParamList = {
  Agent: NavigatorScreenParams<AgentTabParamList>
}

/**
 * Helper for automatically generating navigation prop types for each route.
 *
 * More info: https://reactnavigation.org/docs/typescript/#organizing-types
 */
export type AgentDrawerScreenProps<T extends keyof AgentDrawerParamList> = CompositeScreenProps<
  DrawerScreenProps<AgentDrawerParamList, T>,
  AppStackScreenProps<keyof AppStackParamList>
>

const Drawer = createDrawerNavigator<AgentDrawerParamList>()
export const AgentDrawerNavigator = () => {
  return (
    <AgentProvider>
      <Drawer.Navigator
        initialRouteName="Agent"
        screenOptions={{ headerShown: false, drawerType: "back", drawerPosition: "right" }}
        drawerContent={(props) => <AgentDrawerContent {...props} />}
      >
        <Drawer.Screen name="Agent" component={AgentNavigatorGroup} />
      </Drawer.Navigator>
    </AgentProvider>
  )
}
