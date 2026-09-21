import request from '@/utils/request'
import { createAlgoTask, getAlgoTask, cancelAlgoTask, getPendingResumeIds } from '@/api/interviewelf/algoTask'

export function fetchResumeQuestionBankList(userId, sectionKey, params) {
  return request({
    url: `/interview/admin/users/${userId}/resume-question-banks/${sectionKey}/list`,
    method: 'get',
    params: params || {}
  }).then(res => res.data)
}

export function fetchResumeQuestionBankDetail(userId, sectionKey, resumeId) {
  return request({
    url: `/interview/admin/users/${userId}/resume-question-banks/${sectionKey}/${resumeId}`,
    method: 'get'
  }).then(res => res.data)
}

export function generateResumeQuestionBank(userId, sectionKey, resumeIds, count) {
  return request({
    url: `/interview/admin/users/${userId}/resume-question-banks/${sectionKey}/generate`,
    method: 'post',
    data: {
      resumeIds: resumeIds || [],
      count: count
    }
  }).then(res => res.data)
}

export function createResumeQuestionBankTask(userId, sectionKey, resumeIds, count) {
  return createAlgoTask(userId, 'resume_question_bank', {
    options: {
      sectionKey: sectionKey,
      resumeIds: resumeIds || [],
      count: count
    }
  })
}

export function getResumeQuestionBankTask(userId, taskId) {
  return getAlgoTask(userId, taskId)
}

export function cancelResumeQuestionBankTask(userId, taskId) {
  return cancelAlgoTask(userId, taskId)
}

export function getPendingResumeIdsForQuestionBank(userId, taskId) {
  return getPendingResumeIds(userId, taskId)
}
