import request from '@/utils/request'

export function fetchGeneralQuestionList(category, params) {
  return request({
    url: `/interview/admin/questions/${category}/list`,
    method: 'get',
    params: params || {}
  }).then(res => res.data)
}

export function updateGeneralQuestion(category, id, data) {
  return request({
    url: `/interview/admin/questions/${category}/${id}`,
    method: 'put',
    data: data || {}
  }).then(res => res.data)
}

export function deleteGeneralQuestion(category, id) {
  return request({
    url: `/interview/admin/questions/${category}/${id}`,
    method: 'delete'
  }).then(res => res.data)
}

export function generateGeneralQuestions(category, data) {
  return request({
    url: `/interview/admin/questions/${category}/generate`,
    method: 'post',
    data: data || {}
  }).then(res => res.data)
}
