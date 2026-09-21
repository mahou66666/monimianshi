import store from '@/store'

/**
 * @param {Array} value
 * @returns {Boolean}
 * @example see @/views/permission/directive.vue
 */
export default function checkPermission(value) {
  if (value && value instanceof Array && value.length > 0) {
    const roles = store.getters && store.getters.roles
    const permissionCodes = store.getters && store.getters.permission_codes
    const requiredKeys = value

    const hasRoleMatch = Array.isArray(roles) && roles.some(role => requiredKeys.includes(role))
    const hasCodeMatch = Array.isArray(permissionCodes) && permissionCodes.some(code => requiredKeys.includes(code))
    const hasPermission = hasRoleMatch || hasCodeMatch
    return hasPermission
  } else {
    console.error(`need roles/permission codes! Like v-permission=\"['admin','company:manage']\"`)
    return false
  }
}
