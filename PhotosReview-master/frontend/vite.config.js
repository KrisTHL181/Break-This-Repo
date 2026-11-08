import { defineConfig } from 'vite'

const publicIndexFallback = {
  name: 'public-index-fallback',
  configureServer(server) {
    server.middlewares.use((request, _response, next) => {
      if (request.url === '/' || request.url?.startsWith('/?')) {
        request.url = `/index.html${request.url.slice(1)}`
      }
      next()
    })
  },
}

export default defineConfig({
  plugins: [publicIndexFallback],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/data': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})