import { login, logout as logoutApi } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import router, { resetRouter } from '@/router'

const USER_CACHE_KEY = 'InterviewElf-User'

function saveUserCache(user) {
  try {
    window.localStorage.setItem(USER_CACHE_KEY, JSON.stringify(user || null))
  } catch (e) {
    return
  }
}

function loadUserCache() {
  try {
    const raw = window.localStorage.getItem(USER_CACHE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

function clearUserCache() {
  try {
    window.localStorage.removeItem(USER_CACHE_KEY)
  } catch (e) {
    return
  }
}

function normalizePermissionCodes(codes) {
  if (!Array.isArray(codes)) return []
  return Array.from(new Set(codes
    .map(v => (v == null ? '' : String(v).trim()))
    .filter(v => !!v)))
}

function resolveRoles(permissionCodes) {
  if (permissionCodes.includes('company:manage')) {
    return ['admin']
  }
  return ['staff']
}

const state = {
  token: getToken(),
  name: '',
  avatar: '',
  introduction: '',
  roles: [],
  permissionCodes: []
}

const mutations = {
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_INTRODUCTION: (state, introduction) => {
    state.introduction = introduction
  },
  SET_NAME: (state, name) => {
    state.name = name
  },
  SET_AVATAR: (state, avatar) => {
    state.avatar = avatar
  },
  SET_ROLES: (state, roles) => {
    state.roles = roles
  },
  SET_PERMISSION_CODES: (state, permissionCodes) => {
    state.permissionCodes = permissionCodes
  }
}

const actions = {
  // user login
  login({ commit }, userInfo) {
    const { phone, password } = userInfo
    return new Promise((resolve, reject) => {
      login({ phone: phone.trim(), password: password }).then(response => {
        const { data } = response
        const permissionCodes = normalizePermissionCodes(data.permissionCodes)
        commit('SET_TOKEN', data.token)
        commit('SET_PERMISSION_CODES', permissionCodes)
        setToken(data.token)
        saveUserCache({
          id: data.userId,
          userName: data.userName,
          phone: phone.trim(),
          permissionCodes
        })
        resolve()
      }).catch(error => {
        reject(error)
      })
    })
  },

  // get user info
  getInfo({ commit, state }) {
    return new Promise((resolve, reject) => {
      if (!state.token) {
        reject('No token')
        return
      }

      const user = loadUserCache()
      if (!user) {
        reject('Verification failed, please Login again.')
        return
      }

      const permissionCodes = normalizePermissionCodes(user.permissionCodes)
      const roles = resolveRoles(permissionCodes)
      commit('SET_ROLES', roles)
      commit('SET_PERMISSION_CODES', permissionCodes)
      commit('SET_NAME', user.userName || user.phone || '')
      commit('SET_AVATAR', '')
      commit('SET_INTRODUCTION', '')
      resolve({ roles, permissionCodes })
    })
  },

  // user logout
  logout({ commit, dispatch }) {
    return new Promise((resolve) => {
      logoutApi().catch((e) => {
        console.log(e)
      }).finally(() => {
        commit('SET_TOKEN', '')
        commit('SET_ROLES', [])
        commit('SET_PERMISSION_CODES', [])
        clearUserCache()
        removeToken()
        resetRouter()

        dispatch('tagsView/delAllViews', null, { root: true })
        resolve()
      })
    })
  },

  // remove token
  resetToken({ commit }) {
    return new Promise(resolve => {
      commit('SET_TOKEN', '')
      commit('SET_ROLES', [])
      commit('SET_PERMISSION_CODES', [])
      clearUserCache()
      removeToken()
      resolve()
    })
  },

  // dynamically modify permissions
  async changeRoles({ commit, dispatch }, role) {
    const token = role + '-token'

    commit('SET_TOKEN', token)
    setToken(token)

    const { roles, permissionCodes } = await dispatch('getInfo')

    resetRouter()

    // generate accessible routes map based on roles and permission codes
    const accessRoutes = await dispatch('permission/generateRoutes', { roles, permissionCodes }, { root: true })
    // dynamically add accessible routes
    router.addRoutes(accessRoutes)

    // reset visited views and cached views
    dispatch('tagsView/delAllViews', null, { root: true })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
