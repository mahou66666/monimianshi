import request from '@/utils/request'
import { createAlgoTask, getAlgoTask, cancelAlgoTask, getPendingResumeIds } from '@/api/interviewelf/algoTask'

export function fetchResumeScoreList(userId, params) {
  return request({
    url: `/interview/admin/users/${userId}/resumes/score/list`,
    method: 'get',
    params: params || {}
  }).then(res => res.data)
}

export function createResumeScoreTask(userId, resumeIds) {
  return createAlgoTask(userId, 'resume_score', {
    options: {
      resumeIds: resumeIds || []
    }
  })
}

export function getResumeScoreTask(userId, taskId) {
  return getAlgoTask(userId, taskId)
}

export function cancelResumeScoreTask(userId, taskId) {
  return cancelAlgoTask(userId, taskId)
}

export function getPendingResumeIdsForResumeScore(userId, taskId) {
  return getPendingResumeIds(userId, taskId)
}

export function triggerResumeScore(userId, resumeIds) {
  return request({
    url: `/interview/admin/users/${userId}/resumes/score`,
    method: 'post',
    data: { resumeIds: resumeIds || [] }
  }).then(res => res.data)
}

export function fetchLatestResumeScore(userId, resumeId) {
  return request({
    url: `/interview/admin/users/${userId}/resumes/${resumeId}/score/latest`,
    method: 'get'
  }).then(res => res.data)
}
