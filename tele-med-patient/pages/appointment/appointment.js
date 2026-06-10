var app = getApp();
var request = require('../../utils/request');

Page({
  data: {
    doctors: [],
    selectedDoctorIndex: 0,
    appointmentDate: '',
    timeSlotIndex: 0,
    timeSlots: [
      { code: 0, name: '上午' },
      { code: 1, name: '下午' }
    ],
    description: '',
    appointments: [],
    tabIndex: 0
  },

  onShow: function () {
    this.loadDoctors();
    this.loadAppointments();
  },

  loadDoctors: function () {
    var that = this;
    request.request({
      url: '/hospital/doctors',
      method: 'GET',
      success: function (data) {
        var list = (data || []).map(function (item) {
          item.displayName = item.name + ' - ' + (item.title || '');
          return item;
        });
        that.setData({
          doctors: list
        });
      }
    });
  },

  loadAppointments: function () {
    var that = this;
    var patientId = app.globalData.patientId;
    if (!patientId) {
      return;
    }
    request.request({
      url: '/appointment/patient/' + patientId,
      method: 'GET',
      success: function (data) {
        var list = (data || []).map(function (item) {
          var statusMap = {
            0: '待确认',
            1: '已确认',
            2: '已完成',
            3: '已取消'
          };
          item.statusText = statusMap[item.status] || '未知';
          item.timeSlotDesc = item.timeSlot === 0 ? '上午' : '下午';
          return item;
        });
        that.setData({
          appointments: list
        });
      }
    });
  },

  onDoctorChange: function (e) {
    this.setData({
      selectedDoctorIndex: e.detail.value
    });
  },

  onDateChange: function (e) {
    this.setData({
      appointmentDate: e.detail.value
    });
  },

  onTimeSlotTap: function (e) {
    var index = e.currentTarget.dataset.index;
    this.setData({
      timeSlotIndex: index
    });
  },

  onTabChange: function (e) {
    this.setData({
      tabIndex: e.detail.index
    });
  },

  onDescriptionInput: function (e) {
    this.setData({
      description: e.detail.value
    });
  },

  submitAppointment: function () {
    var that = this;
    var patientId = app.globalData.patientId;
    if (!patientId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    var doctors = this.data.doctors;
    var selectedDoctorIndex = this.data.selectedDoctorIndex;
    if (!doctors.length) {
      wx.showToast({ title: '请选择医生', icon: 'none' });
      return;
    }
    if (!this.data.appointmentDate) {
      wx.showToast({ title: '请选择日期', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '提交中...' });
    request.request({
      url: '/appointment/create',
      method: 'POST',
      data: {
        patientId: patientId,
        doctorId: doctors[selectedDoctorIndex].id,
        appointmentDate: that.data.appointmentDate,
        timeSlot: that.data.timeSlots[that.data.timeSlotIndex].code,
        description: that.data.description
      },
      success: function () {
        wx.hideLoading();
        wx.showToast({ title: '预约成功', icon: 'success' });
        that.setData({
          appointmentDate: '',
          timeSlotIndex: 0,
          description: '',
          selectedDoctorIndex: 0,
          tabIndex: 1
        });
        that.loadAppointments();
      },
      fail: function () {
        wx.hideLoading();
      }
    });
  },

  cancelAppointment: function (e) {
    var that = this;
    var id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '取消预约',
      content: '确定要取消该预约吗？',
      success: function (res) {
        if (res.confirm) {
          var patientId = app.globalData.patientId;
          wx.showLoading({ title: '取消中...' });
          request.request({
            url: '/appointment/cancel?appointmentId=' + id + '&patientId=' + patientId,
            method: 'POST',
            success: function () {
              wx.hideLoading();
              wx.showToast({ title: '已取消', icon: 'success' });
              that.loadAppointments();
            },
            fail: function () {
              wx.hideLoading();
            }
          });
        }
      }
    });
  }
});
