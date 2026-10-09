import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useStudentStore = defineStore('student', () => {
  const token = ref(localStorage.getItem('stu_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('stu_user') || 'null'))

  function login(data) {
    token.value = data.token
    userInfo.value = { username: data.username, nickname: data.nickname || data.username }
    localStorage.setItem('stu_token', data.token)
    localStorage.setItem('stu_user', JSON.stringify(userInfo.value))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('stu_token')
    localStorage.removeItem('stu_user')
  }

  return { token, userInfo, login, logout }
})
