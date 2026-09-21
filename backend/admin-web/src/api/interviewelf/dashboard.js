import request from '@/utils/request'

export function fetchDashboardOverview() {
  return request({
    url: '/interview/admin/dashboard/overview',
    method: 'get'
  }).then(res => res.data)
}
