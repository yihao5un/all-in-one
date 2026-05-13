<template>
  <div class="ai-chat-container">
    <el-card class="chat-card" shadow="never">
      <template #header>
        <div class="chat-header">
          <div class="title-with-status">
            <el-badge is-dot type="success">
              <el-icon :size="20"><ChatDotRound /></el-icon>
            </el-badge>
            <span class="title">HR AI Assistant</span>
          </div>
          <el-button link @click="clearChat">Clear Chat</el-button>
        </div>
      </template>

      <div class="message-list" ref="messageListRef">
        <div 
          v-for="(msg, index) in messages" 
          :key="index" 
          :class="['message-item', msg.role]"
        >
          <div class="avatar" v-if="msg.role === 'ai'">
            <el-icon><Monitor /></el-icon>
          </div>
          <div class="content">
            <div class="text">{{ msg.text }}</div>
            <div class="time">{{ msg.time }}</div>
          </div>
          <div class="avatar" v-if="msg.role === 'user'">
            <el-icon><User /></el-icon>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputMessage"
          placeholder="Ask me about orders, employee status, or HR policies..."
          :rows="2"
          type="textarea"
          @keyup.enter.exact="sendMessage"
        />
        <div class="actions">
          <el-button type="primary" :loading="sending" @click="sendMessage">
            Send Message
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ChatDotRound, Monitor, User } from '@element-plus/icons-vue'
import axios from 'axios'

interface Message {
  role: 'user' | 'ai'
  text: string
  time: string
}

const inputMessage = ref('')
const sending = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const messages = ref<Message[]>([
  { 
    role: 'ai', 
    text: 'Hello! I am your HR AI Assistant. How can I help you manage your orders or answer HR questions today?', 
    time: new Date().toLocaleTimeString() 
  }
])

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || sending.value) return

  const userMsg = inputMessage.value.trim()
  messages.value.push({
    role: 'user',
    text: userMsg,
    time: new Date().toLocaleTimeString()
  })
  inputMessage.value = ''
  sending.value = true
  await scrollToBottom()

  try {
    // Calling the AI API (proxied via /ai-api in vite.config.ts)
    const response = await axios.post('/ai-api/chat', {
      message: userMsg,
      user_id: 'admin'
    })
    
    messages.value.push({
      role: 'ai',
      text: response.data.response,
      time: new Date().toLocaleTimeString()
    })
  } catch (error) {
    messages.value.push({
      role: 'ai',
      text: 'Sorry, I am having trouble connecting to the AI service. Please ensure the AI backend is running.',
      time: new Date().toLocaleTimeString()
    })
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

const clearChat = () => {
  messages.value = [messages.value[0]]
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.ai-chat-container {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.chat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-with-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title { font-weight: bold; font-size: 1.1rem; }

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f1f5f9;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 80%;
}

.message-item.user {
  margin-left: auto;
  justify-content: flex-end;
}

.avatar {
  width: 36px;
  height: 36px;
  background-color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.message-item.user .avatar {
  background-color: #4f46e5;
  color: white;
}

.content {
  display: flex;
  flex-direction: column;
}

.message-item.user .content {
  align-items: flex-end;
}

.text {
  padding: 12px 16px;
  border-radius: 12px;
  background-color: white;
  color: #1e293b;
  line-height: 1.5;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.message-item.ai .text {
  border-top-left-radius: 0;
}

.message-item.user .text {
  background-color: #4f46e5;
  color: white;
  border-top-right-radius: 0;
}

.time {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-top: 4px;
}

.chat-input {
  padding: 20px;
  border-top: 1px solid #e2e8f0;
  background-color: white;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
