<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { chatStream } from '@/api/chat'
import { createConversation, listMessages } from '@/api/conversation'
import { hotQuestions, submitFeedback as submitQaFeedback } from '@/api/qa'
import { sensitiveWords } from '@/mock/student'

const route = useRoute()

const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是校园知识库问答助手，请输入你的校园相关问题～',
    sources: [],
    feedback: '',
    recordId: null,
  },
])
const input = ref('')
const loading = ref(false)
const listRef = ref()
const feedbackVisible = ref(false)
const feedbackForm = ref({ messageIndex: -1, reason: '' })
const conversationId = ref(null)

const FALLBACK_SUGGESTIONS = [
  '新生报到需要带哪些材料？',
  '转专业需要满足什么条件？',
  '国家奖学金的申请条件是什么？',
  '宿舍晚上几点熄灯？',
  '校园卡丢了怎么补办？',
  '每学期选课什么时候开始？',
]

const pool = ref([...FALLBACK_SUGGESTIONS])

const askedQuestions = computed(
  () => new Set(messages.value.filter((m) => m.role === 'user').map((m) => m.content.trim())),
)

const suggestions = computed(() => pool.value.filter((q) => !askedQuestions.value.has(q)).slice(0, 4))

function shuffle(list) {
  const arr = [...list]
  for (let i = arr.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

async function loadSuggestions() {
  try {
    const res = await hotQuestions({ limit: 8, days: 30 })
    const hot = (res?.data || []).map((item) => item.question).filter(Boolean)
    const merged = Array.from(new Set([...hot, ...FALLBACK_SUGGESTIONS]))
    pool.value = shuffle(merged)
  } catch (e) {
    pool.value = shuffle(FALLBACK_SUGGESTIONS)
  }
}

function refreshSuggestions() {
  pool.value = shuffle(pool.value)
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function containsSensitive(text) {
  return sensitiveWords.find((word) => text.includes(word))
}

async function send(text) {
  const question = (text ?? input.value).trim()
  if (!question) {
    ElMessage.warning('请输入问题')
    return
  }

  const badWord = containsSensitive(question)
  if (badWord) {
    messages.value.push({ role: 'user', content: question, sources: [], feedback: '' })
    messages.value.push({
      role: 'assistant',
      content: `抱歉，你的问题包含违规内容「${badWord}」，已被系统拦截，请重新提问。`,
      sources: [],
      feedback: '',
      recordId: null,
    })
    input.value = ''
    scrollToBottom()
    return
  }

  // 多轮：改用服务端会话（方案B）
  messages.value.push({ role: 'user', content: question, sources: [], feedback: '' })
  const index = messages.value.push({ role: 'assistant', content: '', sources: [], feedback: '', recordId: null }) - 1
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    if (!conversationId.value) {
      const res = await createConversation(question)
      conversationId.value = res?.data?.id ?? null
    }
    await chatStream(
      { conversationId: conversationId.value, question },
      (chunk) => {
        messages.value[index].content += chunk
        scrollToBottom()
      },
      {
        onRecordId: (id) => {
          messages.value[index].recordId = id
        },
      },
    )
    if (!messages.value[index].content) {
      messages.value[index].content = '（未返回内容）'
    }
    await loadRealSources(index)
  } catch (e) {
    messages.value[index].content = messages.value[index].content || '回答失败，请稍后重试'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function applyFeedback(msg, feedback) {
  msg.feedback = !feedback || feedback === 'NONE' ? '' : feedback
}

async function rate(index, type) {
  const msg = messages.value[index]
  if (!msg.recordId) {
    ElMessage.warning('该回答尚未保存，暂时无法反馈')
    return
  }
  // 再次点击同一按钮 = 取消反馈
  const next = msg.feedback === type ? 'NONE' : type
  try {
    const res = await submitQaFeedback({ recordId: msg.recordId, type: next })
    applyFeedback(msg, res?.data?.feedback)
    if (next === 'NONE') {
      ElMessage.info('已取消反馈')
    } else if (next === 'LIKE') {
      ElMessage.success('感谢你的认可！')
    }
  } catch (e) {
    // 错误提示由 http 拦截器统一处理
  }
}

/** 从服务端会话消息里取本轮回答的真实引用来源 */
async function loadRealSources(index) {
  if (!conversationId.value) return
  try {
    const res = await listMessages(conversationId.value)
    const list = res?.data || []
    const last = list[list.length - 1]
    if (last?.role === 'assistant' && last.sources) {
      const parsed = JSON.parse(last.sources)
      if (Array.isArray(parsed) && parsed.length) {
        messages.value[index].sources = parsed
      }
    }
  } catch (e) {
    // 忽略
  }
}

function openFeedback(index) {
  const msg = messages.value[index]
  // 已点踩，再次点击 = 取消反馈
  if (msg.feedback === 'DISLIKE') {
    rate(index, 'DISLIKE')
    return
  }
  feedbackForm.value = { messageIndex: index, reason: '' }
  feedbackVisible.value = true
}

async function submitFeedback() {
  const msg = messages.value[feedbackForm.value.messageIndex]
  if (!msg?.recordId) {
    ElMessage.warning('该回答尚未保存，暂时无法反馈')
    return
  }
  const reason = feedbackForm.value.reason.trim()
  try {
    const res = await submitQaFeedback({ recordId: msg.recordId, type: 'DISLIKE', reason: reason || undefined })
    applyFeedback(msg, res?.data?.feedback)
    feedbackVisible.value = false
    ElMessage.success('反馈已提交，感谢你的帮助')
  } catch (e) {
    // 错误提示由 http 拦截器统一处理
  }
}

onMounted(() => {
  loadSuggestions()
  const q = route.query.q
  if (q) send(String(q))
})
</script>

<template>
  <el-card class="chat-card" shadow="never">
    <div class="chat-header">
      <span class="title"><el-icon><ChatDotRound /></el-icon> 智能问答</span>
      <el-tag type="success" effect="plain">基于知识库 RAG 检索</el-tag>
    </div>

    <div ref="listRef" class="chat-list">
      <div v-for="(msg, index) in messages" :key="index" class="message"
        :class="msg.role === 'user' ? 'message-user' : 'message-assistant'">
        <el-avatar :size="36" :icon="msg.role === 'user' ? 'UserFilled' : 'Service'"
          :style="{ background: msg.role === 'user' ? '#1890ff' : '#52c41a' }" />
        <div class="bubble">
          <div class="bubble-content">
            {{ msg.content || (loading && index === messages.length - 1 ? '正在检索知识库...' : '') }}
          </div>

          <div v-if="msg.role === 'assistant' && msg.sources?.length" class="sources">
            <span class="text-muted">引用来源：</span>
            <el-tag v-for="s in msg.sources" :key="s" size="small" effect="plain" type="info">
              <el-icon><Document /></el-icon> {{ s }}
            </el-tag>
          </div>

          <div v-if="msg.role === 'assistant' && msg.recordId" class="actions">
            <button class="feedback-btn like" :class="{ active: msg.feedback === 'LIKE' }"
              @click="rate(index, 'LIKE')">
              <el-icon><CaretTop /></el-icon>
              <span>点赞</span>
            </button>
            <button class="feedback-btn dislike" :class="{ active: msg.feedback === 'DISLIKE' }"
              @click="openFeedback(index)">
              <el-icon><CaretBottom /></el-icon>
              <span>点踩</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input v-model="input" type="textarea" :rows="2" resize="none"
        placeholder="请输入你的问题，例如：新生报到需要带哪些材料？" @keyup.enter.exact.prevent="send()" />
      <el-button type="primary" :icon="'Promotion'" :loading="loading" @click="send()">发送</el-button>
    </div>

    <div class="suggestions">
      <span class="text-muted">试试：</span>
      <el-tag v-for="q in suggestions" :key="q" class="suggestion" effect="plain" @click="send(q)">
        {{ q }}
      </el-tag>
      <el-button link type="primary" size="small" :icon="'Refresh'" @click="refreshSuggestions">
        换一换
      </el-button>
    </div>

    <el-dialog v-model="feedbackVisible" title="提交反馈" width="460px">
      <el-input v-model="feedbackForm.reason" type="textarea" :rows="4"
        placeholder="请描述回答存在的问题，例如：信息过期、答案不准确等" />
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.chat-card {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 140px);
  border-radius: 8px;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.chat-header .title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 4px;
}

.message {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.message-user {
  flex-direction: row-reverse;
}

.bubble {
  max-width: 74%;
}

.bubble-content {
  padding: 10px 14px;
  border-radius: 8px;
  background: #f4f4f5;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-user .bubble-content {
  background: #1890ff;
  color: #fff;
}

.sources {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.actions {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.feedback-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 12px;
  border: 1px solid transparent;
  border-radius: 14px;
  background: transparent;
  color: #9499a0;
  font-size: 13px;
  line-height: 1.4;
  cursor: pointer;
  transition: color 0.2s, background 0.2s, border-color 0.2s;
}

.feedback-btn:hover:not(:disabled) {
  background: #f2f3f5;
  color: #61666d;
}

.feedback-btn:disabled {
  cursor: default;
}

.feedback-btn:disabled:not(.active) {
  opacity: 0.4;
}

.feedback-btn.like.active {
  color: #52c41a;
  border-color: #b7eb8f;
  background: #f6ffed;
  font-weight: 600;
}

.feedback-btn.dislike.active {
  color: #ff4d4f;
  border-color: #ffccc7;
  background: #fff1f0;
  font-weight: 600;
}

.chat-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding-top: 10px;
}

.suggestion {
  cursor: pointer;
}
</style>
