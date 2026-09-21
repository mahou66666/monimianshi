import request from '@/utils/request'

export function createAlgoTask(userId, algoKey, payload) {
  return request({
    url: `/interview/admin/users/${userId}/algorithms/${algoKey}/tasks`,
    method: 'post',
    data: payload || {}
  }).then(res => res.data)
}

export function getAlgoTask(userId, taskId) {
  return request({
    url: `/interview/admin/users/${userId}/algorithms/tasks/${taskId}`,
    method: 'get',
    silentErrorMessage: true
  }).then(res => res.data)
}

export function cancelAlgoTask(userId, taskId) {
  return request({
    url: `/interview/admin/users/${userId}/algorithms/tasks/${taskId}/cancel`,
    method: 'post'
  }).then(res => res.data)
}

export function getPendingResumeIds(userId, taskId) {
  return request({
    url: `/interview/admin/users/${userId}/algorithms/tasks/${taskId}/pending-resume-ids`,
    method: 'get'
  }).then(res => res.data)
}
