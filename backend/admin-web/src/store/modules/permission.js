import { asyncRoutes, constantRoutes } from '@/router'

/**
 * Determine whether current user can access route.
 * Priority:
 * 1) meta.perms -> permission code check
 * 2) meta.roles -> role check
 * 3) otherwise allow
 */
function hasPermission(roles, permissionCodes, route) {
  if (route.meta && Array.isArray(route.meta.perms) && route.meta.perms.length > 0) {
    return permissionCodes.some(code => route.meta.perms.includes(code))
  }
  if (route.meta && route.meta.roles) {
    return roles.some(role => route.meta.roles.includes(role))
  }
  return true
}

/**
 * Filter asynchronous routing tables by recursion
 * @param routes asyncRoutes
 * @param roles
 * @param permissionCodes
 */
export function filterAsyncRoutes(routes, roles, permissionCodes) {
  const res = []

  routes.forEach(route => {
    const tmp = { ...route }
    if (hasPermission(roles, permissionCodes, tmp)) {
      if (tmp.children) {
        tmp.children = filterAsyncRoutes(tmp.children, roles, permissionCodes)
      }
      res.push(tmp)
    }
  })

  return res
}

const state = {
  routes: [],
  addRoutes: [],
  sidebarRoutes: [],
  legacyRoutes: []
}

const mutations = {
  SET_ROUTES: (state, payload) => {
    const { accessedRoutes, sidebarRoutes, legacyRoutes } = payload
    state.addRoutes = accessedRoutes
    state.routes = constantRoutes.concat(accessedRoutes)
    state.sidebarRoutes = sidebarRoutes
    state.legacyRoutes = legacyRoutes
  }
}

function splitRoutes(accessedRoutes) {
  const interviewElf = accessedRoutes.find(r => r.path === '/interviewelf')
  const sidebarRoutes = interviewElf && interviewElf.children
    ? interviewElf.children
      .filter(c => c && !c.hidden)
      .map(c => ({
        ...c,
        path: c.path && c.path.startsWith('/') ? c.path : `/interviewelf/${c.path}`
      }))
    : []

  const constantLegacyRoutes = constantRoutes.filter(r => !r.hidden)
  const asyncLegacyRoutes = accessedRoutes.filter(r => r.path !== '/interviewelf')
  const legacyRoutes = constantLegacyRoutes.concat(asyncLegacyRoutes)

  return { sidebarRoutes, legacyRoutes }
}

const actions = {
  generateRoutes({ commit }, authPayload) {
    return new Promise(resolve => {
      const roles = Array.isArray(authPayload)
        ? authPayload
        : (authPayload && Array.isArray(authPayload.roles) ? authPayload.roles : [])
      const permissionCodes = authPayload && Array.isArray(authPayload.permissionCodes)
        ? authPayload.permissionCodes
        : []

      const accessedRoutes = filterAsyncRoutes(asyncRoutes, roles, permissionCodes)

      const { sidebarRoutes, legacyRoutes } = splitRoutes(accessedRoutes)
      commit('SET_ROUTES', { accessedRoutes, sidebarRoutes, legacyRoutes })
      resolve(accessedRoutes)
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
