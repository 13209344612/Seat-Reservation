import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

// 同步对话：一次性返回完整回答
export function chat(data) {
  return request({
    url: '/ai/chat',
    method: 'post',
    data
  })
}

// 历史会话列表（当前用户，按最后活跃时间倒序）
export function listConversations() {
  return request({
    url: '/ai/conversations',
    method: 'get'
  })
}

// 回溯某会话的全部消息（按时间正序）
export function getConversationMessages(conversationId) {
  return request({
    url: `/ai/conversations/${conversationId}/messages`,
    method: 'get'
  })
}

// 删除某会话及其消息
export function deleteConversation(conversationId) {
  return request({
    url: `/ai/conversations/${conversationId}`,
    method: 'delete'
  })
}

/**
 * 流式对话（SSE）
 *
 * 使用原生 fetch 读取响应流，以便携带 Authorization 头
 * （原生 EventSource 不支持自定义请求头，无法复用 JWT 认证）。
 *
 * @param {{message: string, conversationId?: string}} payload 请求体
 * @param {{onMessage?: Function, onDone?: Function, onError?: Function}} handlers 回调
 * @returns {AbortController} 调用方可用返回的 controller.abort() 主动中断
 */
export function chatStream(payload, { onMessage, onDone, onError } = {}) {
  const userStore = useUserStore()
  const controller = new AbortController()

  fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${userStore.token}`
    },
    body: JSON.stringify(payload),
    signal: controller.signal
  }).then(async (response) => {
    if (!response.ok) {
      onError?.(new Error('请求失败（HTTP ' + response.status + '）'))
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let finished = false

    while (!finished) {
      const { value, done } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')

      // SSE 事件之间以空行（\n\n）分隔，逐个完整事件解析
      let sepIndex
      while ((sepIndex = buffer.indexOf('\n\n')) >= 0) {
        const rawEvent = buffer.slice(0, sepIndex)
        buffer = buffer.slice(sepIndex + 2)

        let eventType = 'message'
        const dataLines = []
        for (const line of rawEvent.split('\n')) {
          if (line.startsWith('event:')) {
            eventType = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            // 去掉 "data:" 前缀及其后可能的一个空格
            dataLines.push(line.slice(5).replace(/^ /, ''))
          }
        }
        const data = dataLines.join('\n')

        if (eventType === 'done' || data === '[DONE]') {
          finished = true
          onDone?.()
        } else if (eventType === 'error') {
          onError?.(new Error(data || 'AI 服务异常'))
        } else if (data) {
          onMessage?.(data)
        }
      }
    }
    if (!finished) onDone?.()
  }).catch((err) => {
    if (err.name !== 'AbortError') onError?.(err)
  })

  return controller
}
