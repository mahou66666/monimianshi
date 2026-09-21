import request from '@/utils/request'

export function fetchUsers(params) {
  return request({
    url: '/interview/admin/users',
    method: 'get',
    params
  }).then(res => res.data)
}

export function fetchPaidServiceOptions() {
  return request({
    url: '/interview/admin/users/paid-service-options',
    method: 'get'
  }).then(res => res.data)
}

export function fetchPermissionOptions() {
  return request({
    url: '/interview/admin/users/permissions/options',
    method: 'get'
  }).then(res => res.data)
}

export function createAdminUser(data) {
  return request({
    url: '/interview/admin/users',
    method: 'post',
    data
  }).then(res => res.data)
}

export function fetchUserDetail(userId) {
  return request({
    url: `/interview/admin/users/${userId}`,
    method: 'get'
  }).then(res => res.data)
}

export function updateAdminUser(userId, data) {
  return request({
    url: `/interview/admin/users/${userId}`,
    method: 'put',
    data
  }).then(res => res.data)
}

export function deleteAdminUser(userId) {
  return request({
    url: `/interview/admin/users/${userId}`,
    method: 'delete'
  }).then(res => res.data)
}

export function fetchUserStatusPanel(userId) {
  return request({
    url: `/interview/admin/users/${userId}/status-panel`,
    method: 'get'
  }).then(res => res.data)
}

export function fetchUserPermissions(userId) {
  return request({
    url: `/interview/admin/users/${userId}/permissions`,
    method: 'get'
  }).then(res => res.data)
}

export function fetchUserPermissionsBatch(userIds) {
  return request({
    url: '/interview/admin/users/permissions/batch',
    method: 'post',
    data: {
      userIds: userIds || []
    }
  }).then(res => res.data)
}

export function assignUserPermissions(userId, permissionIds) {
  return request({
    url: `/interview/admin/users/${userId}/permissions`,
    method: 'put',
    data: {
      permissionIds: permissionIds || []
    }
  }).then(res => res.data)
}

export function disableUser(userId) {
  return request({
    url: `/interview/admin/users/${userId}/disable`,
    method: 'post'
  }).then(res => res.data)
}

export function enableUser(userId) {
  return request({
    url: `/interview/admin/users/${userId}/enable`,
    method: 'post'
  }).then(res => res.data)
}
