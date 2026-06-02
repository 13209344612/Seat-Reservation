import request from '@/utils/request'

// 获取自习室列表
export function getRoomList(params) {
  return request({
    url: '/rooms',
    method: 'get',
    params
  })
}

// 获取自习室详情
export function getRoomDetail(id) {
  return request({
    url: `/rooms/${id}`,
    method: 'get'
  })
}

// 创建自习室（管理员）
export function createRoom(data) {
  return request({
    url: '/rooms',
    method: 'post',
    data
  })
}

// 更新自习室（管理员）
export function updateRoom(id, data) {
  return request({
    url: `/rooms/${id}`,
    method: 'put',
    data
  })
}

// 删除自习室（管理员）
export function deleteRoom(id) {
  return request({
    url: `/rooms/${id}`,
    method: 'delete'
  })
}
