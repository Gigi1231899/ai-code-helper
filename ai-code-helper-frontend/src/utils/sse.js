/**
 * SSE（Server-Sent Events）流式请求工具
 *
 * 为什么不用原生 EventSource：
 * EventSource 不支持自定义请求头，也就没法带上 JWT。
 * 这里改用 fetch + ReadableStream 手动解析 text/event-stream，
 * 既能带 Authorization 头，也能控制断点与错误提示。
 */

/**
 * 解析单个 SSE 事件块
 * 事件块形如：
 *   data:第一段
 *   data:第二段
 * 兼容两种情况：
 *  1) 后端直接输出纯文本：data:你好
 *  2) 后端把 ServerSentEvent 序列化成 JSON：data:{"data":"你好"}
 *
 * @param {string} raw 事件原始文本
 * @returns {string|null} 解析出的内容，空事件返回 null
 */
function parseEvent(raw) {
  const dataLines = []
  for (const line of raw.split(/\r?\n/)) {
    if (line.startsWith('data:')) {
      // 去掉 "data:" 前缀以及规范允许的一个空格
      dataLines.push(line.slice(5).replace(/^ /, ''))
    }
  }
  if (dataLines.length === 0) {
    return null
  }
  const text = dataLines.join('\n')

  // 兼容 JSON 包装的情况
  if (text.startsWith('{')) {
    try {
      const parsed = JSON.parse(text)
      if (parsed && typeof parsed.data === 'string') {
        return parsed.data
      }
    } catch (e) {
      // 不是合法 JSON 就按纯文本处理
    }
  }
  return text
}

/**
 * 发起 SSE 流式请求
 *
 * @param {Object}    options
 * @param {string}    options.url      请求地址（含 /api 前缀）
 * @param {Object}    options.params   query 参数
 * @param {Function}  options.onMessage 每收到一段文本回调
 * @param {Function}  [options.onError] 出错回调
 * @param {Function}  [options.onDone]  流结束回调
 */
export async function streamChat({ url, params, onMessage, onError, onDone }) {
  const query = new URLSearchParams(params).toString()
  const token = localStorage.getItem('token')

  let response
  try {
    response = await fetch(`${url}?${query}`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'text/event-stream',
      },
    })
  } catch (e) {
    onError?.(new Error('无法连接后端服务，请确认后端已启动'))
    onDone?.()
    return
  }

  if (!response.ok || !response.body) {
    onError?.(new Error(`请求失败（HTTP ${response.status}）`))
    onDone?.()
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  // 缓冲区：网络可能把一个事件拆成多次返回，需要拼接后再切分
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })

      // 事件之间以空行分隔；最后一段可能不完整，留在缓冲区等下次数据
      const chunks = buffer.split('\n\n')
      buffer = chunks.pop() ?? ''

      for (const chunk of chunks) {
        const text = parseEvent(chunk)
        if (text) {
          onMessage?.(text)
        }
      }
    }

    // 处理最后残留的一段（部分实现末尾没有空行）
    if (buffer.trim()) {
      const text = parseEvent(buffer)
      if (text) {
        onMessage?.(text)
      }
    }
  } catch (e) {
    onError?.(e)
  } finally {
    onDone?.()
  }
}
