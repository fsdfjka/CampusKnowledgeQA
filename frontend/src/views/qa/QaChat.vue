<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { chatStream } from '@/api/chat'
import { createConversation } from '@/api/conversation'

const messages = ref([
  {
    role: 'assistant',
    content: '你好，我是校园知识库问答助手，请问有什么可以帮你？',
    sources: [],
  },
])
const input = ref('')
const loading = ref(false)
const listRef = ref()
const conversationId = ref(null)

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function send() {
  const question = input.value.trim()
  if (!question) {
    ElMessage.warning('请输入问题')
    return
  }
  messages.value.push({ role: 'user', content: question, sources: [] })
  const index = messages.value.push({ role: 'assistant', content: '', sources: [] }) - 1
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    // 方案B：首次提问先建会话，拿到 conversationId
    if (!conversationId.value) {
      const res = await createConversation(question)
      conversationId.value = res?.data?.id ?? null
    }
    await chatStream({ conversationId: conversationId.value, question }, (chunk) => {
      messages.value[index].content += chunk
      scrollToBottom()
    })
    if (!messages.value[index].content) {
      messages.value[index].content = '（未返回内容）'
    }
  } catch (e) {
    messages.value[index].content = messages.value[index].content || '回答失败，请稍后重试'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<template>
  <el-card class="page-card chat-card" shadow="never">
    <div class="page-toolbar">
      <span class="title">智能问答</span>
      <el-tag type="info" effect="plain">基于知识库 RAG 检索 · 流式输出</el-tag>
    </div>

    <div ref="listRef" class="chat-list">
      <div v-for="(msg, index) in messages" :key="index" class="message"
        :class="msg.role === 'user' ? 'message-user' : 'message-assistant'">
        <el-avatar :size="34" :icon="msg.role === 'user' ? 'UserFilled' : 'Service'"
          :style="{ background: msg.role === 'user' ? '#1890ff' : '#52c41a' }" />
        <div class="bubble">
          <div class="bubble-content">
            {{ msg.content || (loading && index === messages.length - 1 ? '正在生成...' : '') }}
          </div>
          <div v-if="msg.sources?.length" class="bubble-sources text-muted">
            引用来源：<el-tag v-for="s in msg.sources" :key="s" size="small" effect="plain">{{ s }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input v-model="input" type="textarea" :rows="2" resize="none"
        placeholder="请输入你的问题，例如：新生报到需要带哪些材料？" @keyup.enter.exact.prevent="send" />
      <el-button type="primary" :icon="'Promotion'" :loading="loading" @click="send">发送</el-button>
    </div>
  </el-card>
</template>

<style scoped>
.chat-card {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 96px);
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
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
  max-width: 70%;
}

.bubble-content {
  padding: 10px 14px;
  border-radius: 8px;
  background: #f4f4f5;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-user .bubble-content {
  background: #1890ff;
  color: #fff;
}

.bubble-sources {
  margin-top: 6px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.chat-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
</style>
