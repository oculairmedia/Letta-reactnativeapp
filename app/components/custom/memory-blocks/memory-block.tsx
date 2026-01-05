import { Fragment, useEffect, useState } from "react"
import { TextStyle } from "react-native"
import { TextField } from "@/components/TextField"
import { Skeleton } from "@/shared/components/animated/skeleton"
import { BlockHeader } from "./block-header"

interface MemoryBlockItemProps {
  label: string
  value: string
  isLoading: boolean
  onSave: (value: string) => void
}

export function MemoryBlockItem({ label, value, isLoading, onSave }: MemoryBlockItemProps) {
  const [currentValue, setCurrentValue] = useState(value)
  const isModified = currentValue !== value

  useEffect(() => {
    setCurrentValue(value)
  }, [value])

  return (
    <Fragment>
      <BlockHeader
        label={label}
        isModified={isModified}
        onSave={() => onSave(currentValue)}
        onReset={() => {
          setCurrentValue(value)
        }}
      />
      {isLoading ? (
        <Skeleton />
      ) : (
        <TextField
          value={currentValue}
          onChangeText={setCurrentValue}
          style={$blockValue}
          multiline
        />
      )}
    </Fragment>
  )
}

const $blockValue: TextStyle = {
  fontSize: 12,
  lineHeight: 16,
}
