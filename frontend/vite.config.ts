import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname, '')
  const apiProxyTarget = env.VITE_API_PROXY_TARGET ?? 'http://localhost:9090/fadaiting-camp'

  return {
  plugins: [
    vue(),
    {
      name: 'exclude-local-login-helpers',
      apply: 'build',
      writeBundle(options) {
        if (options.dir) {
          fs.rmSync(path.join(options.dir, 'local-userscripts'), { recursive: true, force: true })
        }
      }
    },
    {
      name: 'serve-local-userscripts',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          const requestUrl = req.url ?? ''
          if (!requestUrl.startsWith('/local-userscripts/')) {
            return next()
          }

          const fileName = requestUrl.replace('/local-userscripts/', '')
          if (!['admin-login-autofill.user.js', 'admin-login-autofill.runtime.js'].includes(fileName)) {
            return next()
          }
          const filePath = path.resolve(__dirname, 'public', 'local-userscripts', fileName)
          if (!fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) {
            return next()
          }

          res.setHeader('Content-Type', 'application/javascript; charset=utf-8')
          res.end(fs.readFileSync(filePath, 'utf8'))
        })
      }
    }
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    allowedHosts: ['.trycloudflare.com'],
    proxy: {
      '/objects': {
        target: env.VITE_OBJECT_PROXY_TARGET ?? 'http://localhost:9000',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/objects/, '')
      },
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
        rewrite: path => path
      }
    }
  }
  }
})
