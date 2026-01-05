import { LettaClientProvider } from "@/providers/LettaProvider"
import { QueryClientProvider } from "./QueryClientProvider"

export const Providers = ({ children }: { children: React.ReactNode }) => {
  return (
    <LettaClientProvider>
      <QueryClientProvider>{children}</QueryClientProvider>
    </LettaClientProvider>
  )
}
