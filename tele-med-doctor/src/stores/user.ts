import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import router from '@/router'
import type { User, Doctor } from '@/types'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const doctorInfo = ref<Doctor | null>(null)
  const token = ref<string>(localStorage.getItem('token') || '')

  async function login(username: string, password: string) {
    const res = await request.post<never, { data: User }>('/user/login', { username, password })
    const userData = res.data
    user.value = userData
    user.value.name = userData.realName || userData.username
    token.value = String(userData.id)
    localStorage.setItem('token', token.value)
    localStorage.setItem('user', JSON.stringify(userData))

    if (userData.role === 'DOCTOR') {
      try {
        const doctorRes = await request.get<never, { data: Doctor }>(`/user/doctor/${userData.id}`)
        doctorInfo.value = doctorRes.data
        localStorage.setItem('doctorInfo', JSON.stringify(doctorInfo.value))
      } catch (e) {
        console.error('Failed to load doctor info', e)
      }
    }
  }

  function logout() {
    user.value = null
    doctorInfo.value = null
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('doctorInfo')
    router.push('/')
  }

  function init() {
    const savedUser = localStorage.getItem('user')
    if (savedUser) {
      user.value = JSON.parse(savedUser)
    }
    const savedDoctor = localStorage.getItem('doctorInfo')
    if (savedDoctor) {
      doctorInfo.value = JSON.parse(savedDoctor)
    }
  }

  init()

  return { user, doctorInfo, token, login, logout }
})
