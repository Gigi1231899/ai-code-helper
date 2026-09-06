import hljs from 'highlight.js/lib/common'
import MarkdownIt from 'markdown-it'

/**
 * Markdown 渲染（AI 回答里常有代码块，直接显示纯文本体验很差）
 * 只引入 highlight.js 的常用语言包，避免打包体积过大
 */
const md = new MarkdownIt({
  html: false, // 禁止输出原始 HTML，防止 XSS
  linkify: true,
  breaks: true,
  highlight(code, language) {
    const escaped = md.utils.escapeHtml(code)
    if (language && hljs.getLanguage(language)) {
      try {
        const highlighted = hljs.highlight(code, { language, ignoreIllegals: true }).value
        return `<pre class="hljs"><code>${highlighted}</code></pre>`
      } catch (e) {
        // 高亮失败时退回转义后的纯文本
      }
    }
    return `<pre class="hljs"><code>${escaped}</code></pre>`
  },
})

/**
 * 把 Markdown 文本渲染成 HTML
 *
 * @param {string} text Markdown 原文
 * @returns {string} HTML 字符串
 */
export function renderMarkdown(text) {
  return md.render(text || '')
}
