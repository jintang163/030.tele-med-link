import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import { doctorLogin, type LoginResponse } from '@/api/auth'
import router from '@/router'
import type { User, Doctor } from '@/types'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const doctorInfo = ref<Doctor | null>(null)
  const token = ref<string>(localStorage.getItem('token') || '')
  const hospitalId = ref<number | null>(null)
  const campusId = ref<number | null>(null)

  async function login(username: string, password: string) {
    const res = await doctorLogin(username, password)
    const loginData: LoginResponse = res.data as unknown as LoginResponse

    token.value = loginData.token
    localStorage.setItem('token', loginData.token)

    const userData: User = {
      id: loginData.userId,
      username: loginData.username,
      realName: loginData.realName,
      role: loginData.role,
      hospitalId: loginData.hospitalId,
      department: loginData.department,
      status: 1,
      name: loginData.realName
    }
    user.value = userData
    user.value.name = userData.realName || userData.username
    localStorage.setItem('user', JSON.stringify(userData))

    if (loginData.doctorId) {
      const doctor: Doctor = {
        id: loginData.doctorId,
        userId: loginData.userId,
        title: loginData.title,
        department: loginData.department,
        hospitalId: loginData.hospitalId,
        campusId: loginData.campusId,
        name: loginData.realName,
        hospitalName: loginData.hospitalName,
        campusName: loginData.campusName,
        status: 1
      }
      doctorInfo.value = doctor
      localStorage.setItem('doctorInfo', JSON.stringify(doctor))
    }

    if (loginData.hospitalId) {
      hospitalId.value = loginData.hospitalId
      localStorage.setItem('hospitalId', String(loginData.hospitalId))
    }
    if (loginData.campusId) {
      campusId.value = loginData.campusId
      localStorage.setItem('campusId', String(loginData.campusId))
    }
  }

  function logout() {
    user.value = null
    doctorInfo.value = null
    token.value = ''
    hospitalId.value = null
    campusId.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('doctorInfo')
    localStorage.removeItem('hospitalId')
    localStorage.removeItem('campusId')
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
    const savedHospitalId = localStorage.getItem('hospitalId')
    if (savedHospitalId) {
      hospitalId.value = Number(savedHospitalId)
    }
    const savedCampusId = localStorage.getItem('campusId')
    if (savedCampusId) {
      campusId.value = Number(savedCampusId)
    }
  }

  init()

  return { user, doctorInfo, token, hospitalId, campusId, login, logout }
})
