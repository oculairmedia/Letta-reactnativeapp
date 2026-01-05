import { DropdownMenuItemTitle } from "./dropdown-menu"

import { DropdownMenuItem } from "./dropdown-menu"

import { DropdownMenuContent } from "./dropdown-menu"

import { DropdownMenuRoot, DropdownMenuTrigger } from "./dropdown-menu"
import { Icon, IconProps, IconTypes } from "./Icon"

interface DropdownMenuBuilderProps {
  children: React.ReactNode
  icon?: IconTypes
  iconProps?: Omit<IconProps, "icon">
  actions?: {
    key: string
    title: string
    icon?: React.ReactNode
    onPress: () => void
  }[]
}

export const WithSimpleDropdownMenu = ({
  children,
  actions,
  icon: _Icon,
  iconProps,
}: DropdownMenuBuilderProps) => {
  const isIconPresent = !!_Icon
  const trigger = isIconPresent ? <Icon icon={_Icon} {...iconProps} /> : children
  return (
    <DropdownMenuRoot>
      <DropdownMenuTrigger>{trigger}</DropdownMenuTrigger>
      <DropdownMenuContent>
        {actions?.map((item) => (
          <DropdownMenuItem key={item.key} onSelect={item.onPress}>
            <DropdownMenuItemTitle>{item.title}</DropdownMenuItemTitle>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenuRoot>
  )
}
