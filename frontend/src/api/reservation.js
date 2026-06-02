import request from '@/utils/request'

// 创建预约
export function createReservation(data) {
  return request({
    url: '/reservations',
    method: 'post',
    data
  })
}

// 获取预约列表
export function getReservationList(params) {
  return request({
    url: '/reservations',
    method: 'get',
    params
  })
}

// 获取预约详情
export function getReservationDetail(id) {
  return request({
    url: `/reservations/${id}`,
    method: 'get'
  })
}

// 取消预约
export function cancelReservation(id) {
  return request({
    url: `/reservations/${id}`,
    method: 'delete'
  })
}

// 签到
export function signReservation(id) {
  return request({
    url: `/reservations/${id}/sign`,
    method: 'post'
  })
}
