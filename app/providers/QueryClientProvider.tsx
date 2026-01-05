import {
  QueryClient,
  QueryClientProvider as TanstackQueryClientProvider,
} from "@tanstack/react-query"

const queryClient = new QueryClient({
  defaultOptions: {
    mutations: {
      onError: (error) => {
        console.error(error)
      },
      retry: false,
    },
  },
})

export const QueryClientProvider = ({ children }: { children: React.ReactNode }) => {
  return <TanstackQueryClientProvider client={queryClient}>{children}</TanstackQueryClientProvider>
}
