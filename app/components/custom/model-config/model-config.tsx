import { useEffect, useState } from "react"
import { View, ViewStyle, TextStyle } from "react-native"
import { Text } from "@/components/Text"
import { TextField } from "@/components/TextField"
import { Switch } from "@/components/Toggle/Switch"
import { spacing } from "@/theme"
import { useAgent, useModifyAgent } from "@/hooks/use-agent"
import { Skeleton } from "@/shared/components/animated/skeleton"
import { useAppTheme } from "@/utils/useAppTheme"
import type { ThemedStyle } from "@/theme"

interface ModelConfigProps {
  agentId: string
}

export function ModelConfig({ agentId }: ModelConfigProps) {
  const { data: agent, isLoading } = useAgent(agentId)
  const { mutate: modifyAgent } = useModifyAgent(agentId)
  const { themed } = useAppTheme()

  const [contextWindow, setContextWindow] = useState("")
  const [temperature, setTemperature] = useState("")
  const [maxTokens, setMaxTokens] = useState("")
  const [parallelToolCalls, setParallelToolCalls] = useState(false)

  const modelSettings = agent?.model_settings as {
    temperature?: number
    max_output_tokens?: number
    parallel_tool_calls?: boolean
  } | null

  useEffect(() => {
    if (agent) {
      setContextWindow(agent.llm_config?.context_window?.toString() || "")
      setTemperature(modelSettings?.temperature?.toString() || "")
      setMaxTokens(modelSettings?.max_output_tokens?.toString() || "")
      setParallelToolCalls(modelSettings?.parallel_tool_calls ?? false)
    }
  }, [agent, modelSettings])

  const handleContextWindowSave = () => {
    const value = parseInt(contextWindow, 10)
    if (!isNaN(value)) {
      modifyAgent({ context_window_limit: value })
    }
  }

  const handleTemperatureSave = () => {
    const value = parseFloat(temperature)
    if (!isNaN(value) && value >= 0 && value <= 2) {
      modifyAgent({
        model_settings: {
          ...modelSettings,
          temperature: value,
        },
      })
    }
  }

  const handleMaxTokensSave = () => {
    const value = parseInt(maxTokens, 10)
    if (!isNaN(value)) {
      modifyAgent({
        model_settings: {
          ...modelSettings,
          max_output_tokens: value,
        },
      })
    }
  }

  const handleParallelToolCallsChange = (value: boolean) => {
    setParallelToolCalls(value)
    modifyAgent({
      model_settings: {
        ...modelSettings,
        parallel_tool_calls: value,
      },
    })
  }

  if (isLoading) {
    return (
      <View style={$container}>
        <Skeleton />
      </View>
    )
  }

  return (
    <View style={$container}>
      <Text preset="bold" style={$title}>
        Model Configuration
      </Text>

      <View style={$row}>
        <Text style={themed($label)}>Model</Text>
        <Text style={themed($value)}>{agent?.model || "N/A"}</Text>
      </View>

      <View style={$row}>
        <Text style={themed($label)}>Context Window</Text>
        <TextField
          value={contextWindow}
          onChangeText={setContextWindow}
          onBlur={handleContextWindowSave}
          keyboardType="numeric"
          style={$input}
          containerStyle={$inputContainer}
        />
      </View>

      <View style={$row}>
        <Text style={themed($label)}>Temperature</Text>
        <TextField
          value={temperature}
          onChangeText={setTemperature}
          onBlur={handleTemperatureSave}
          keyboardType="decimal-pad"
          style={$input}
          containerStyle={$inputContainer}
        />
      </View>

      <View style={$row}>
        <Text style={themed($label)}>Max Output Tokens</Text>
        <TextField
          value={maxTokens}
          onChangeText={setMaxTokens}
          onBlur={handleMaxTokensSave}
          keyboardType="numeric"
          style={$input}
          containerStyle={$inputContainer}
        />
      </View>

      <View style={$row}>
        <Text style={themed($label)}>Parallel Tool Calls</Text>
        <Switch value={parallelToolCalls} onValueChange={handleParallelToolCallsChange} />
      </View>
    </View>
  )
}

const $container: ViewStyle = {
  gap: spacing.xxs,
}

const $title: ViewStyle = {
  marginBottom: spacing.xs,
}

const $row: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  justifyContent: "space-between",
  gap: spacing.sm,
}

const $label: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.text,
  flex: 1,
})

const $value: ThemedStyle<TextStyle> = ({ colors }) => ({
  color: colors.textDim,
  flex: 1,
  textAlign: "right",
})

const $inputContainer: ViewStyle = {
  flex: 1,
}

const $input: TextStyle = {
  textAlign: "right",
}
