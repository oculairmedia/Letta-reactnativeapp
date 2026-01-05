export interface BlockHeaderProps {
  label: string
  isModified: boolean
  onSave: () => void
  onReset: () => void
}

export interface MemoryBlockProps {
  agentId: string
}

export interface BlockData {
  id: string
  label: string
  value: string
}
