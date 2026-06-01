import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      '@user': path.resolve(__dirname, '../../user/frontend/src')
    }
  },
  build: {
    minify: false,
    rollupOptions: {
      output: {
        inlineDynamicImports: true
      }
    }
  },
  server: {
    port: 3000,
    fs: {
      allow: [
        path.resolve(__dirname),
        path.resolve(__dirname, '../../user/frontend')
      ]
    },
    proxy: {
      '/api/user': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/api/auth': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/api/admin': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
