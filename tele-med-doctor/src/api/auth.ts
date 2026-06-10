import request from '@/utils/request'
import type { User, Doctor } from '@/types'

export interface LoginResponse {
  token: string
  userId: number
  username: string
  realName: string
  role: string
  hospitalId?: number
  campusId?: number
  hospitalName?: string
  campusName?: string
  department?: string
  title?: string
  doctorId?: number
}

export function doctorLogin(username: string, password: string) {
  return request.post<never, { data: LoginResponse }>('/auth/doctor-login', {
    username,
    password
  })
}

export function adminLogin(username: string, password: string) {
  return request.post<never, { data: LoginResponse }>('/auth/admin-login', {
    username,
    password
  })
}

export function getUserInfo() {
  return request.get<never, { data: User }>('/user/info')
}

export function getDoctorInfo(userId: number) {
  return request.get<never, { data: Doctor }>(`/user/doctor/${userId}`)
}
