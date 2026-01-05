import { Platform } from "react-native"
import * as ContextMenu from "zeego/context-menu"
import { MenuItemIconProps } from "zeego/lib/typescript/menu/types"

type IosIconName = MenuItemIconProps["ios"]
type AndroidIconName = MenuItemIconProps["androidIconName"]

interface SimpleContextMenuProps {
  children: React.ReactNode
  actions?: {
    key: string
    title: string
    iosIconName?: IosIconName
    androidIconName?: AndroidIconName
    onPress: () => void
  }[]
}

export const SimpleContextMenu = ({ children, actions }: SimpleContextMenuProps) => {
  return (
    <ContextMenu.Root>
      <ContextMenu.Trigger>{children}</ContextMenu.Trigger>
      <ContextMenu.Content>
        {actions?.map((action) => (
          <ContextMenu.Item key={action.key} onSelect={action.onPress}>
            <ContextMenu.ItemTitle>{action.title}</ContextMenu.ItemTitle>
            {Platform.select({
              ios: action.iosIconName && <ContextMenu.ItemIcon ios={action.iosIconName} />,
              android: action.androidIconName && (
                <ContextMenu.ItemIcon androidIconName={action.androidIconName} />
              ),
            })}
          </ContextMenu.Item>
        ))}
      </ContextMenu.Content>
    </ContextMenu.Root>
  )
}
