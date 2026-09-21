import request from '@/utils/request'

export function importPdfBatch(userId, files) {
  const formData = new FormData()
  ;(files || []).forEach((f) => {
    if (f) formData.append('files', f)
  })

  return request({
    url: `/interview/admin/users/${userId}/resumes/import-pdf-batch`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).then(res => res.data)
}

export function precheckImportPdfMd5Batch(userId, md5List) {
  return request({
    url: `/interview/admin/users/${userId}/resumes/import-pdf/precheck/md5/batch`,
    method: 'post',
    data: {
      md5List: md5List || []
    }
  }).then(res => res.data)
}

export function parseUploadedBatch(userId, resumeFileIds) {
  return request({
    url: `/interview/admin/users/${userId}/resumes/parse/uploaded/batch`,
    method: 'post',
    timeout: 180000,
    data: {
      resumeFileIds: resumeFileIds || []
    }
  }).then(res => res.data)
}

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

export function getPendingResumeFileIds(userId, taskId) {
  return request({
    url: `/interview/admin/users/${userId}/algorithms/tasks/${taskId}/pending-resume-file-ids`,
    method: 'get'
  }).then(res => res.data)
}

export function cancelAlgoTask(userId, taskId) {
  return request({
    url: `/interview/admin/users/${userId}/algorithms/tasks/${taskId}/cancel`,
    method: 'post'
  }).then(res => res.data)
}

export function parseResumeBatch(userId, files) {
  const formData = new FormData()
  ;(files || []).forEach((f) => {
    if (f) formData.append('files', f)
  })

  return request({
    url: `/interview/admin/users/${userId}/resumes/parse/batch`,
    method: 'post',
    data: formData,
    timeout: 180000,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).then(res => res.data)
}

export function fetchImportedResumes(userId, params) {
  return request({
    url: `/interview/admin/users/${userId}/resumes/imported`,
    method: 'get',
    params: params || {}
  }).then(res => res.data)
}

export function fetchResumeContent(userId, resumeId) {
  return request({
    url: `/interview/admin/users/${userId}/resumes/${resumeId}/content`,
    method: 'get'
  }).then(res => res.data)
}

export function fetchResumeContentBatch(userId, resumeIds) {
  return request({
    url: `/interview/admin/users/${userId}/resumes/content/batch`,
    method: 'post',
    data: {
      resumeIds: resumeIds || []
    }
  }).then(res => res.data)
}

export function updateResume(userId, resumeId, data) {
  return request({
    url: `/interview/admin/users/${userId}/resumes/${resumeId}`,
    method: 'put',
    data: data || {}
  }).then(res => res.data)
}

export function deleteResume(userId, resumeId) {
  return request({
    url: `/interview/admin/users/${userId}/resumes/${resumeId}`,
    method: 'delete'
  }).then(res => res.data)
}
