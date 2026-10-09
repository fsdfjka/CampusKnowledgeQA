import http from './http'

/** 单轮问答（非流式）：GET /chat?question=... */
export function chat(question) {
  return http.get('/chat', { params: { question } })
}

/**
 * 多轮流式问答：POST /chat/stream（SSE）
 * @param {{conversationId?: number, question: string, history?: Array}} payload
 * @param {(chunk: string) => void} onMessage 每收到一段文本时回调
 * @param {AbortSignal} [signal]
 */
export async function chatStream(payload, onMessage, options = {}) {
  const { conversationId, question, history } = payload || {}
  const { signal, onRecordId } = options || {}
  const token = localStorage.getItem('stu_token') || localStorage.getItem('ck_token')
  const headers = { 'Content-Type': 'application/json', Accept: 'text/event-stream' }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  const resp = await fetch('/api/chat/stream', {
    method: 'POST',
    headers,
    body: JSON.stringify({ conversationId, question, history: history || [] }),
    signal,
  })
  if (resp.status === 401) {
    throw new Error('登录已过期，请重新登录')
  }
  if (!resp.ok || !resp.body) {
    throw new Error(`请求失败：${resp.status}`)
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let boundary
    while ((boundary = buffer.indexOf('\n\n')) >= 0) {
      const rawEvent = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      const data = parseSseEvent(rawEvent)
      if (data === null || data === '') continue
      if (data.startsWith(RECORD_ID_PREFIX)) {
        const id = Number(data.slice(RECORD_ID_PREFIX.length))
        if (!Number.isNaN(id)) onRecordId?.(id)
        continue
      }
      onMessage?.(data)
    }
  }
}

const RECORD_ID_PREFIX = '\u0001RECORD_ID:'

function parseSseEvent(rawEvent) {
  const dataLines = []
  for (const line of rawEvent.split('\n')) {
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).replace(/^ /, ''))
    }
  }
  return dataLines.length ? dataLines.join('\n') : null
}
