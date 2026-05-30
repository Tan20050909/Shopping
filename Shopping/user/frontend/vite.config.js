import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api/data-center': 'http://localhost:8081',
      '/api/upload': 'http://localhost:8081',
      '/api': 'http://localhost:8082',
      '/uploads': 'http://localhost:8081'
    }
  }
})
