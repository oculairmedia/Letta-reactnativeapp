export const normalizeTypeName = (type: string | undefined) => {
  if (!type) return ""
  return type
    .replace("letta_", "")
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ")
}

export const normalizeToolName = (name: string | undefined) => {
  if (!name) return ""
  return name
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ")
}

export const normalizeName = (name: string | undefined) => {
  if (!name) return ""
  return name
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ")
}
