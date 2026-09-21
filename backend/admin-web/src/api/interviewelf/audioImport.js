import request from '@/utils/request'

/**
 * 批量上传音频文件
 * @param {number|string} userId 用户ID
 * @param {File[]} files 音频文件列表
 */
export function importAudioBatch(userId, files) {
  const formData = new FormData()
  ;(files || []).forEach((f) => {
    if (f) formData.append('files', f)
  })

  return request({
    url: `/interview/admin/users/${userId}/audios/import-batch`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }).then(res => res.data)
}

/**
 * 获取用户已上传音频列表
 * @param {number|string} userId 用户ID
 */
export function fetchImportedAudios(userId) {
  return request({
    url: `/interview/admin/users/${userId}/audios/imported`,
    method: 'get'
  }).then(res => res.data)
}
