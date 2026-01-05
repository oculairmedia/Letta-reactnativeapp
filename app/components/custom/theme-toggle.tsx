import { Button, Icon, Switch } from "@/components"
import { useAppTheme } from "@/utils/useAppTheme"
import { observer } from "mobx-react-lite"
import { FC, Fragment } from "react"

interface ThemeToggleProps {
  mode?: "switch" | "icon"
}
export const ThemeToggle: FC<ThemeToggleProps> = observer(({ mode = "switch" }) => {
  const { theme, setThemeContextOverride } = useAppTheme()

  const handleThemeToggle = () => {
    setThemeContextOverride(theme.isDark ? "light" : "dark")
  }

  return (
    <Fragment>
      {mode === "switch" && (
        <Switch
          accessibilityMode="icon"
          accessibilityOnIcon="Sun"
          accessibilityOffIcon="Moon"
          value={theme.isDark}
          onValueChange={handleThemeToggle}
        />
      )}

      {mode === "icon" && (
        <Button preset="icon" onPress={handleThemeToggle}>
          <Icon icon="Sun" size={24} color={theme.colors.text} />
        </Button>
      )}
    </Fragment>
  )
})
