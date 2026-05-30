import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@user/styles.css'
import App from './App.vue'
import router from './router'

const AUTH_ROLE_KEY = 'shopping_auth_role'

if (!sessionStorage.getItem(AUTH_ROLE_KEY)) {
  if (localStorage.getItem('merchantUser')) sessionStorage.setItem(AUTH_ROLE_KEY, 'merchant')
  else if (localStorage.getItem('shopping_user_token')) sessionStorage.setItem(AUTH_ROLE_KEY, 'user')
}

const app = createApp(App)
app.use(ElementPlus)
app.use(createPinia())
app.use(router)
app.mount('#app')
