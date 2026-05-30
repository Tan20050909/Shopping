import { defineStore } from 'pinia'
import { api, getUserToken, setUserToken } from '../api/client'

export const useUserStore = defineStore('user', {
  state: () => ({
    profile: null,
    token: getUserToken()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    syncToken() {
      this.token = getUserToken()
    },
    logout() {
      setUserToken('')
      sessionStorage.removeItem('shopping_auth_role')
      this.token = ''
      this.profile = null
    },
    async loadMe() {
      this.syncToken()
      this.profile = await api('/api/user/profile')
    }
  }
})
