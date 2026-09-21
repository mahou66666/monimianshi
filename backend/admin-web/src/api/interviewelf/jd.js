import request from '@/utils/request'

export function fetchJdList(params) {
  return request({
    url: '/interview/admin/jd/list',
    method: 'get',
    params: params || {}
  }).then(res => res.data)
}

export function fetchJdGuideList(params) {
  return request({
    url: '/interview/admin/jd/guide/list',
    method: 'get',
    params: params || {}
  }).then(res => res.data)
}

export function fetchJdGuideLatest(jdId) {
  return request({
    url: `/interview/admin/jd/guide/${jdId}/latest`,
    method: 'get'
  }).then(res => res.data)
}

export function clearJdGuide(jdId) {
  return request({
    url: `/interview/admin/jd/guide/${jdId}/clear`,
    method: 'post'
  }).then(res => res.data)
}

export function importJdTextBatch(text, companyId) {
  return request({
    url: '/interview/admin/jd/import/text-batch',
    method: 'post',
    data: {
      text: text || '',
      companyId: companyId || null
    }
  }).then(res => res.data)
}

export function extractJdBatch(jdIds) {
  return request({
    url: '/interview/admin/jd/extract/batch',
    method: 'post',
    data: {
      jdIds: jdIds || []
    }
  }).then(res => res.data)
}
