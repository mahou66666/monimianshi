import store from '@/store'

function checkPermission(el, binding) {
  const { value } = binding
  const roles = store.getters && store.getters.roles
  const permissionCodes = store.getters && store.getters.permission_codes

  if (value && value instanceof Array) {
    if (value.length > 0) {
      const requiredKeys = value

      const hasRoleMatch = Array.isArray(roles) && roles.some(role => requiredKeys.includes(role))
      const hasCodeMatch = Array.isArray(permissionCodes) && permissionCodes.some(code => requiredKeys.includes(code))
      const hasPermission = hasRoleMatch || hasCodeMatch

      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  } else {
    throw new Error(`need roles/permission codes! Like v-permission=\"['admin','company:manage']\"`)
  }
}

export default {
  inserted(el, binding) {
    checkPermission(el, binding)
  },
  update(el, binding) {
    checkPermission(el, binding)
  }
}
