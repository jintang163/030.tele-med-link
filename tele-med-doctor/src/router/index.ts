import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/consultation/:id',
      name: 'consultation',
      component: () => import('@/views/ConsultationView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/cross-campus/schedule',
      name: 'crossCampusSchedule',
      component: () => import('@/views/CrossCampusScheduleView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/cross-campus/list',
      name: 'crossCampusList',
      component: () => import('@/views/CrossCampusListView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/dicom/viewer',
      name: 'dicomTokenViewer',
      component: () => import('@/views/DicomTokenViewer.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/schedule/manage',
      name: 'scheduleManage',
      component: () => import('@/views/DoctorScheduleManage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/appointment/book',
      name: 'appointmentBook',
      component: () => import('@/views/PatientAppointmentView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/appointment/reschedule',
      name: 'appointmentReschedule',
      component: () => import('@/views/AppointmentRescheduleView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/appointment/reschedule/:appointmentId',
      name: 'appointmentRescheduleWithId',
      component: () => import('@/views/AppointmentRescheduleView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/notifications',
      name: 'notificationCenter',
      component: () => import('@/views/NotificationCenter.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth) {
    const userStore = useUserStore()
    if (!userStore.token) {
      next({ name: 'login' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
