<template>
  <div class="assistant">
    <div class="chat-shell">
      <!-- 历史会话侧边栏 -->
      <aside class="sidebar" :class="{ open: sidebarVisible }">
        <div class="sidebar-head">
          <el-button type="primary" round class="new-chat-btn" @click="startNewChat">
            <el-icon><Plus /></el-icon>新对话
          </el-button>
        </div>
        <div class="conv-list">
          <template v-if="convLoading && conversations.length === 0">
            <el-skeleton v-for="i in 4" :key="i" class="conv-skeleton" animated>
              <template #template>
                <el-skeleton-item variant="text" style="width: 70%; height: 16px" />
                <el-skeleton-item variant="text" style="width: 40%; height: 12px; margin-top: 6px" />
              </template>
            </el-skeleton>
          </template>
          <div v-else-if="conversations.length === 0" class="conv-empty">暂无历史会话</div>
          <div
            v-for="c in conversations"
            :key="c.conversationId"
            class="conv-item"
            :class="{ active: c.conversationId === currentId }"
            @click="selectConversation(c)"
          >
            <el-icon class="conv-icon"><ChatLineRound /></el-icon>
            <div class="conv-main">
              <div class="conv-title">{{ c.title || '新对话' }}</div>
              <div class="conv-time">{{ formatTime(c.updateTime) }}</div>
            </div>
            <el-icon class="conv-del" @click.stop="removeConversation(c)"><Delete /></el-icon>
          </div>
        </div>
      </aside>
      <div v-if="sidebarVisible" class="sidebar-backdrop" @click="sidebarVisible = false"></div>

      <!-- 对话主区 -->
      <div class="chat-panel">
        <div class="chat-bar">
          <div class="chat-bar-left">
            <el-button text class="menu-toggle" @click="sidebarVisible = !sidebarVisible">
              <el-icon size="18"><Menu /></el-icon>
            </el-button>
            <el-button text class="back-btn" @click="$router.back()">
              <el-icon><ArrowLeft /></el-icon>返回
            </el-button>
            <span class="chat-title">
              <el-icon><ChatDotRound /></el-icon>{{ currentTitle }}
            </span>
          </div>
        </div>

        <div ref="scrollRef" class="message-list" @scroll.passive="onListScroll">
          <!-- 空状态引导 -->
          <div v-if="messages.length === 0" class="empty-tip">
            <span class="empty-icon"><el-icon size="30"><ChatDotRound /></el-icon></span>
            <h2>你好，我是自习室预约助手</h2>
            <p>我可以帮你查询空位、预约座位、取消预约，以及解答预约规则。试试下面的问题：</p>
            <div class="quick-questions">
              <el-tag
                v-for="q in quickQuestions"
                :key="q"
                class="quick-tag"
                effect="plain"
                round
                @click="sendQuick(q)"
              >{{ q }}</el-tag>
            </div>
          </div>

          <!-- 消息气泡 -->
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            :class="['message-row', msg.role]"
          >
            <div class="avatar">
              <el-icon v-if="msg.role === 'assistant'"><Service /></el-icon>
              <el-icon v-else><UserFilled /></el-icon>
            </div>
            <div class="msg-body">
              <div :class="['bubble', { error: msg.error, stopped: msg.stopped }]">
                <!-- 等待首字：思考中动画 -->
                <div v-if="msg.loading && !msg.content" class="thinking">
                  <span></span><span></span><span></span>
                </div>
                <!-- AI 回复按 Markdown 渲染，用户消息保持纯文本 -->
                <div v-else-if="msg.role === 'assistant'" class="md-content" v-html="renderMarkdown(msg.content)"></div>
                <div v-else class="bubble-text"><p>{{ msg.content }}</p></div>
                <span v-if="msg.loading && msg.content" class="typing">▋</span>
              </div>
              <div v-if="msg.error" class="msg-error">
                <el-icon><WarningFilled /></el-icon>{{ msg.content }}
                <el-button link type="primary" size="small" @click="retryLast">重新生成</el-button>
              </div>
              <div class="msg-meta">
                <span v-if="msg.time" class="msg-time">{{ formatTime(msg.time) }}</span>
                <span v-if="msg.stopped" class="msg-stopped">已停止生成</span>
                <el-tooltip
                  v-if="msg.role === 'assistant' && !msg.loading && !msg.error && msg.content"
                  content="复制回答"
                >
                  <el-icon class="msg-copy" @click="copyMessage(msg)"><CopyDocument /></el-icon>
                </el-tooltip>
              </div>
            </div>
          </div>
        </div>

        <!-- 回到底部悬浮按钮 -->
        <transition name="fade">
          <div v-show="showJumpBtn" class="jump-bottom" title="回到底部" @click="forceScrollToBottom">
            <el-icon><Bottom /></el-icon>
          </div>
        </transition>

        <div class="chat-footer">
          <div class="input-wrap">
            <el-input
              v-model="input"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 5 }"
              resize="none"
              maxlength="500"
              placeholder="输入你的需求，Enter 发送，Shift + Enter 换行"
              @keydown.enter.exact.prevent="handleSend"
            />
            <span v-show="input.length > 400" class="char-count">{{ input.length }}/500</span>
            <el-button
              class="send-btn"
              type="primary"
              circle
              :disabled="!loading && !input.trim()"
              :title="loading ? '停止生成' : '发送'"
              @click="loading ? stopStream() : handleSend()"
            >
              <el-icon size="18">
                <VideoPause v-if="loading" />
                <Promotion v-else />
              </el-icon>
            </el-button>
          </div>
          <p class="footer-tip">AI 回答仅供参考，预约结果请以「我的预约」页面为准</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { chatStream, listConversations, getConversationMessages, deleteConversation } from '@/api/ai'

const conversations = ref([])
const convLoading = ref(false)
const currentId = ref('')
const messages = ref([])
const input = ref('')
const loading = ref(false)
const scrollRef = ref(null)
const sidebarVisible = ref(false)
const showJumpBtn = ref(false)

let controller = null
let stickToBottom = true   // 用户未主动上滑时，流式输出自动跟随滚动
let userAborted = false    // 区分主动停止与异常中断

const quickQuestions = [
  '现在有哪些自习室还有空位？',
  '帮我预约明天下午的座位',
  '我有哪些预约？',
  '预约后多久不签到会被取消？'
]

// 顶栏标题：优先用当前会话的标题，新对话时显示默认名
const currentTitle = computed(() => {
  const c = conversations.value.find(x => x.conversationId === currentId.value)
  return c?.title || 'AI 预约助手'
})

function newConversationId() {
  return 'web-' + Date.now() + '-' + Math.random().toString(36).slice(2, 7)
}

function formatTime(t) {
  if (!t) return ''
  const d = dayjs(t)
  return d.isSame(dayjs(), 'day') ? d.format('HH:mm') : d.format('MM-DD HH:mm')
}

/* ---- 轻量 Markdown 渲染（先转义再替换，避免 XSS） ---- */
function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;')
}

function inlineMd(s) {
  return s
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
}

function renderMarkdown(content) {
  const text = (content || '').trim()
  if (!text) return ''
  const blocks = []
  // 先提取围栏代码块，占位后再处理其余块级语法
  let src = escapeHtml(text).replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) => {
    blocks.push(`<pre class="md-pre" data-lang="${lang}"><code>${code.replace(/\n$/, '')}</code></pre>`)
    return `\u0000${blocks.length - 1}\u0000`
  })

  const lines = src.split('\n')
  const html = []
  let listType = null   // 'ul' | 'ol'
  const closeList = () => { if (listType) { html.push(`</${listType}>`); listType = null } }

  for (let line of lines) {
    const placeholder = line.trim().match(/^\u0000(\d+)\u0000$/)
    if (placeholder) { closeList(); html.push(blocks[+placeholder[1]]); continue }

    const heading = line.match(/^(#{1,4})\s+(.*)$/)
    const bullet = line.match(/^\s*[-*]\s+(.*)$/)
    const ordered = line.match(/^\s*\d+[.、)]\s+(.*)$/)

    if (heading) {
      closeList()
      const lv = Math.min(heading[1].length + 2, 6)   // # -> h3，避免气泡内标题过大
      html.push(`<h${lv}>${inlineMd(heading[2])}</h${lv}>`)
    } else if (bullet) {
      if (listType !== 'ul') { closeList(); html.push('<ul>'); listType = 'ul' }
      html.push(`<li>${inlineMd(bullet[1])}</li>`)
    } else if (ordered) {
      if (listType !== 'ol') { closeList(); html.push('<ol>'); listType = 'ol' }
      html.push(`<li>${inlineMd(ordered[1])}</li>`)
    } else if (/^\s*(-{3,}|\*{3,})\s*$/.test(line)) {
      closeList(); html.push('<hr/>')
    } else if (!line.trim()) {
      closeList()
    } else {
      closeList(); html.push(`<p>${inlineMd(line)}</p>`)
    }
  }
  closeList()
  return html.join('')
}

/* ---- 滚动控制 ---- */
function scrollToBottom(force = false) {
  if (!force && !stickToBottom) return
  nextTick(() => {
    if (scrollRef.value) {
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight
    }
  })
}

function forceScrollToBottom() {
  stickToBottom = true
  showJumpBtn.value = false
  scrollToBottom(true)
}

// 距底部 60px 内视为“跟随”；上滑阅读时暂停自动滚动并显示回到底部按钮
function onListScroll() {
  const el = scrollRef.value
  if (!el) return
  const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60
  stickToBottom = atBottom
  showJumpBtn.value = !atBottom && messages.value.length > 0
}

function abortStream() {
  if (controller) {
    controller.abort()
    controller = null
  }
}

// 停止生成：保留已输出内容，标记为已停止
function stopStream() {
  userAborted = true
  abortStream()
  const last = messages.value[messages.value.length - 1]
  if (last?.role === 'assistant' && last.loading) {
    last.loading = false
    last.stopped = true
    if (!last.content) last.content = '已停止生成'
  }
  loading.value = false
}

// 新建对话：仅切换到一个空的本地会话，落库发生在首条消息发送后
function startNewChat() {
  abortStream()
  currentId.value = newConversationId()
  messages.value = []
  loading.value = false
  sidebarVisible.value = false
  showJumpBtn.value = false
  stickToBottom = true
}

async function loadConversations() {
  convLoading.value = true
  try {
    const res = await listConversations()
    conversations.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    convLoading.value = false
  }
}

async function selectConversation(c) {
  if (c.conversationId === currentId.value) {
    sidebarVisible.value = false
    return
  }
  abortStream()
  loading.value = false
  currentId.value = c.conversationId
  sidebarVisible.value = false
  try {
    const res = await getConversationMessages(c.conversationId)
    messages.value = (res.data || []).map(m => ({
      role: m.role,
      content: m.content,
      time: m.createTime || m.time
    }))
    forceScrollToBottom()
  } catch (e) {
    console.error(e)
    messages.value = []
  }
}

async function removeConversation(c) {
  try {
    await ElMessageBox.confirm('确认删除该会话？删除后不可恢复。', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return  // 用户取消
  }
  try {
    await deleteConversation(c.conversationId)
    ElMessage.success('已删除')
    if (c.conversationId === currentId.value) {
      startNewChat()
    }
    await loadConversations()
  } catch (e) {
    console.error(e)
  }
}

function sendQuick(q) {
  input.value = q
  handleSend()
}

async function copyMessage(msg) {
  try {
    await navigator.clipboard.writeText(msg.content)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('当前环境不支持自动复制，请手动选择文本')
  }
}

// 重新生成：移除末尾的错误回复，重发最后一条用户消息
function retryLast() {
  if (loading.value) return
  const lastUserIdx = messages.value.map(m => m.role).lastIndexOf('user')
  if (lastUserIdx < 0) return
  const text = messages.value[lastUserIdx].content
  messages.value.splice(lastUserIdx)   // 去掉用户消息之后的错误回复
  input.value = text
  handleSend()
}

function handleSend() {
  const text = input.value.trim()
  if (!text || loading.value) return

  if (!currentId.value) currentId.value = newConversationId()

  messages.value.push({ role: 'user', content: text, time: new Date() })
  messages.value.push({ role: 'assistant', content: '', loading: true })
  const assistantMsg = messages.value[messages.value.length - 1]

  input.value = ''
  loading.value = true
  userAborted = false
  forceScrollToBottom()

  controller = chatStream(
    { message: text, conversationId: currentId.value },
    {
      onMessage: (chunk) => {
        assistantMsg.content += chunk
        scrollToBottom()
      },
      onDone: () => {
        assistantMsg.loading = false
        assistantMsg.time = new Date()
        loading.value = false
        scrollToBottom()
        loadConversations()  // 刷新侧边栏（新会话标题、最近排序）
      },
      onError: (err) => {
        assistantMsg.loading = false
        if (userAborted) return   // 主动停止不提示错误
        assistantMsg.error = true
        assistantMsg.content = '抱歉，AI 助手暂时不可用：' + (err.message || '未知错误')
        loading.value = false
        ElMessage.error('对话失败：' + (err.message || '未知错误'))
      }
    }
  )
}

onMounted(() => {
  startNewChat()
  loadConversations()
})

onBeforeUnmount(() => {
  abortStream()
})
</script>

<style scoped>
.assistant {
  height: calc(100vh - 60px);
  display: flex;
  justify-content: center;
  background: var(--app-page-bg);
}

.chat-shell {
  position: relative;
  display: flex;
  width: 100%;
  max-width: 1100px;
  height: 100%;
  margin: 0 auto;
  background: #fff;
  border-left: 1px solid var(--app-border);
  border-right: 1px solid var(--app-border);
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--app-border);
  background: #fbfbfd;
}

.sidebar-head {
  padding: 14px 12px;
  border-bottom: 1px solid var(--app-border);
}

.new-chat-btn {
  width: 100%;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-empty {
  padding: 24px 12px;
  text-align: center;
  font-size: 13px;
  color: #9aa0a8;
}

.conv-skeleton {
  padding: 10px 12px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background .15s;
}

.conv-item:hover {
  background: #eef1f6;
}

.conv-item.active {
  background: var(--app-primary-soft);
}

.conv-icon {
  color: #9aa0a8;
  flex-shrink: 0;
}

.conv-item.active .conv-icon {
  color: var(--el-color-primary);
}

.conv-main {
  flex: 1;
  min-width: 0;
}

.conv-title {
  font-size: 14px;
  color: var(--app-text-body);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-item.active .conv-title {
  color: var(--el-color-primary);
  font-weight: 500;
}

.conv-time {
  font-size: 12px;
  color: #9aa0a8;
  margin-top: 2px;
}

.conv-del {
  opacity: 0;
  color: #b0b4bb;
  flex-shrink: 0;
  transition: opacity .15s, color .15s;
}

.conv-item:hover .conv-del {
  opacity: 1;
}

.conv-del:hover {
  color: var(--el-color-danger);
}

.sidebar-backdrop {
  display: none;
}

/* 对话主区 */
.chat-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.chat-bar {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid var(--app-border);
}

.chat-bar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-toggle {
  display: none;
}

.back-btn {
  color: var(--app-text-muted);
}

.chat-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-title);
}

.chat-title .el-icon {
  color: var(--el-color-primary);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  /* 左右 16px 与 chat-bar 对齐：AI 头像与「返回」按钮同一列 */
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
}

/* 消息不足一屏时整体贴底，更接近聊天习惯 */
.message-list > .message-row:first-child {
  margin-top: auto;
}

.empty-tip {
  text-align: center;
  padding: 48px 20px;
  color: var(--app-text-muted);
}

.empty-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: var(--app-primary-soft);
  color: var(--el-color-primary);
}

.empty-tip h2 {
  margin: 18px 0 8px;
  font-size: 19px;
  color: var(--app-text-title);
}

.empty-tip p {
  max-width: 460px;
  margin: 0 auto 20px;
  line-height: 1.7;
  font-size: 14px;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.quick-tag {
  cursor: pointer;
  padding: 0 14px;
  height: 32px;
  line-height: 30px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 18px;
  gap: 10px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.msg-body {
  max-width: 74%;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.message-row.user .msg-body {
  align-items: flex-end;
}

.avatar {
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
  background: var(--app-primary-soft);
}

.message-row.user .avatar {
  background: var(--el-color-primary);
  color: #fff;
}

.bubble {
  max-width: 100%;
  width: fit-content;
  padding: 11px 15px;
  border-radius: 14px;
  background: #f4f5f7;
  color: var(--app-text-body);
  line-height: 1.65;
  word-break: break-word;
}

/* 用户消息：蓝色圆角气泡靠右；AI 回复：基础款灰色气泡靠左 */
.message-row.user .bubble {
  background: var(--el-color-primary);
  color: #fff;
}

.bubble.error {
  display: none;   /* 错误文案展示在下方 msg-error 区域 */
}

.bubble-text p {
  margin: 0;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

/* Markdown 内容样式 */
.md-content :deep(p) {
  margin: 0 0 8px;
  line-height: 1.75;
}

.md-content :deep(p:last-child),
.md-content :deep(ul:last-child),
.md-content :deep(ol:last-child),
.md-content :deep(h3:last-child),
.md-content :deep(h4:last-child),
.md-content :deep(h5:last-child),
.md-content :deep(h6:last-child),
.md-content :deep(pre:last-child) {
  margin-bottom: 0;
}

.md-content :deep(h3),
.md-content :deep(h4),
.md-content :deep(h5),
.md-content :deep(h6) {
  margin: 12px 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text-title);
  line-height: 1.5;
}

.md-content :deep(ul),
.md-content :deep(ol) {
  margin: 0 0 8px;
  padding-left: 20px;
}

.md-content :deep(li) {
  margin: 3px 0;
  line-height: 1.7;
}

.md-content :deep(code) {
  padding: 2px 5px;
  border-radius: 5px;
  background: rgba(17, 24, 39, .07);
  font-family: 'JetBrains Mono', Consolas, Monaco, monospace;
  font-size: 13px;
}

.md-content :deep(.md-pre) {
  margin: 8px 0;
  padding: 12px 14px;
  border-radius: 10px;
  background: #1f2937;
  overflow-x: auto;
}

.md-content :deep(.md-pre code) {
  padding: 0;
  background: transparent;
  color: #e5e7eb;
  font-size: 13px;
  line-height: 1.6;
}

.md-content :deep(a) {
  color: var(--el-color-primary);
  text-decoration: none;
}

.md-content :deep(a:hover) {
  text-decoration: underline;
}

.md-content :deep(hr) {
  margin: 10px 0;
  border: none;
  border-top: 1px solid rgba(17, 24, 39, .1);
}

/* 思考中：三点跳动动画 */
.thinking {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 22px;
}

.thinking span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #b0b4bb;
  animation: bounce 1.2s ease-in-out infinite;
}

.thinking span:nth-child(2) {
  animation-delay: .15s;
}

.thinking span:nth-child(3) {
  animation-delay: .3s;
}

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: .5; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* 消息底部元信息：时间 / 停止标记 / 复制 */
.msg-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  padding: 0 4px;
  font-size: 12px;
  color: #9aa0a8;
  min-height: 18px;
}

.msg-copy {
  cursor: pointer;
  opacity: 0;
  transition: opacity .15s, color .15s;
}

.message-row:hover .msg-copy {
  opacity: 1;
}

.msg-copy:hover {
  color: var(--el-color-primary);
}

.msg-stopped {
  color: var(--el-color-warning);
}

.msg-error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  padding: 8px 12px;
  border-radius: 10px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: var(--el-color-danger);
  font-size: 13px;
  line-height: 1.5;
}

/* 回到底部悬浮按钮 */
.jump-bottom {
  position: absolute;
  right: 28px;
  bottom: 130px;
  z-index: 10;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid var(--app-border);
  box-shadow: var(--app-shadow-hover);
  color: var(--app-text-muted);
  cursor: pointer;
  transition: color .15s;
}

.jump-bottom:hover {
  color: var(--el-color-primary);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity .2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.typing {
  animation: blink 1s steps(1) infinite;
  margin-left: 2px;
}

@keyframes blink {
  50% { opacity: 0; }
}

.chat-footer {
  flex-shrink: 0;
  padding: 12px 16px 10px;
  background: #fff;
  border-top: 1px solid var(--app-border);
}

.input-wrap {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 8px 8px 8px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
  background: #fbfbfd;
  transition: border-color .15s, box-shadow .15s;
}

.input-wrap:focus-within {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, .1);
  background: #fff;
}

.input-wrap :deep(.el-textarea__inner) {
  box-shadow: none !important;
  background: transparent;
  padding: 6px 0;
  font-size: 14px;
  line-height: 1.6;
}

.char-count {
  flex-shrink: 0;
  font-size: 12px;
  color: #9aa0a8;
  padding-bottom: 8px;
}

.send-btn {
  flex-shrink: 0;
}

.footer-tip {
  margin: 8px 2px 0;
  text-align: center;
  font-size: 12px;
  color: #9aa0a8;
}

@media (max-width: 768px) {
  .menu-toggle {
    display: inline-flex;
  }
  .sidebar {
    position: absolute;
    top: 0;
    left: 0;
    height: 100%;
    z-index: 20;
    transform: translateX(-100%);
    transition: transform .2s ease;
  }
  .sidebar.open {
    transform: none;
    box-shadow: 2px 0 12px rgba(0, 0, 0, .1);
  }
  .sidebar-backdrop {
    display: block;
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, .2);
    z-index: 15;
  }
}
</style>
