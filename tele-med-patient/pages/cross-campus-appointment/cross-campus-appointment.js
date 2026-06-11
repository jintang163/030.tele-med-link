var app = getApp();
var request = require('../../utils/request');

Page({
  data: {
    currentStep: 1,
    sourceCampusId: null,
    sourceCampusName: '',
    hospitalId: null,
    targetCampusList: [],
    selectedTargetCampusId: null,
    targetCampusName: '',
    filterDepartment: '',
    targetDoctorList: [],
    assistantDoctorList: [],
    selectedPrimaryDoctorId: null,
    primaryDoctorName: '',
    primaryDoctorTitle: '',
    primaryDoctorDepartment: '',
    selectedAssistantDoctorIds: [],
    assistantNames: '',
    minDate: '',
    appointmentDate: '',
    timeSlotList: [],
    selectedTimeSlot: null,
    selectedTimeSlotName: '',
    description: '',
    submitting: false
  },

  onLoad: function () {
    var today = new Date();
    var minDate = this.formatDateStr(today);
    this.setData({
      sourceCampusId: app.globalData.campusId,
      sourceCampusName: app.globalData.campusName || '当前院区',
      hospitalId: app.globalData.hospitalId,
      minDate: minDate,
      appointmentDate: minDate
    });
    this.loadTargetCampusList();
  },

  formatDateStr: function (d) {
    var year = d.getFullYear();
    var month = String(d.getMonth() + 1).padStart(2, '0');
    var day = String(d.getDate()).padStart(2, '0');
    return year + '-' + month + '-' + day;
  },

  loadTargetCampusList: function () {
    var that = this;
    if (!this.data.hospitalId) return;
    request.request({
      url: '/cross-campus/campuses?hospitalId=' + this.data.hospitalId,
      method: 'GET',
      success: function (data) {
        var list = (data || []).filter(function (item) {
          return item.id !== that.data.sourceCampusId;
        });
        that.setData({ targetCampusList: list });
      }
    });
  },

  selectTargetCampus: function (e) {
    var id = e.currentTarget.dataset.id;
    var name = e.currentTarget.dataset.name;
    this.setData({
      selectedTargetCampusId: id,
      targetCampusName: name
    });
  },

  goToStep2: function () {
    if (!this.data.selectedTargetCampusId) {
      wx.showToast({ title: '请选择目标院区', icon: 'none' });
      return;
    }
    this.setData({ currentStep: 2 });
    this.loadTargetDoctors();
  },

  goBackStep1: function () {
    this.setData({ currentStep: 1 });
  },

  onDeptInput: function (e) {
    this.setData({ filterDepartment: e.detail.value });
  },

  loadTargetDoctors: function () {
    var that = this;
    var campusId = this.data.selectedTargetCampusId;
    if (!campusId) return;
    var dept = this.data.filterDepartment || '';
    wx.showLoading({ title: '加载中...' });
    request.request({
      url: '/cross-campus/campus/' + campusId + '/doctors?department=' + encodeURIComponent(dept),
      method: 'GET',
      success: function (data) {
        wx.hideLoading();
        that.setData({
          targetDoctorList: data || [],
          assistantDoctorList: data || []
        });
      },
      fail: function () {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  selectPrimaryDoctor: function (e) {
    var id = e.currentTarget.dataset.id;
    var doctor = this.data.targetDoctorList.find(function (d) { return d.id === id; });
    var newAssistantIds = this.data.selectedAssistantDoctorIds.filter(function (aid) {
      return aid !== id;
    });
    this.setData({
      selectedPrimaryDoctorId: id,
      primaryDoctorName: doctor ? doctor.name : '',
      primaryDoctorTitle: doctor ? doctor.title : '',
      primaryDoctorDepartment: doctor ? doctor.department : '',
      selectedAssistantDoctorIds: newAssistantIds
    });
    this.updateAssistantNames();
  },

  toggleAssistantDoctor: function (e) {
    var id = e.currentTarget.dataset.id;
    if (id === this.data.selectedPrimaryDoctorId) {
      wx.showToast({ title: '主诊医生不能作为副诊', icon: 'none' });
      return;
    }
    var ids = this.data.selectedAssistantDoctorIds.slice();
    var index = ids.indexOf(id);
    if (index > -1) {
      ids.splice(index, 1);
    } else {
      ids.push(id);
    }
    this.setData({ selectedAssistantDoctorIds: ids });
    this.updateAssistantNames();
  },

  updateAssistantNames: function () {
    var ids = this.data.selectedAssistantDoctorIds;
    var names = [];
    for (var i = 0; i < ids.length; i++) {
      var doc = this.data.assistantDoctorList.find(function (d) { return d.id === ids[i]; });
      if (doc) names.push(doc.name);
    }
    this.setData({ assistantNames: names.join('、') });
  },

  goBackStep2: function () {
    this.setData({ currentStep: 2 });
  },

  goToStep3: function () {
    if (!this.data.selectedPrimaryDoctorId) {
      wx.showToast({ title: '请选择主诊医生', icon: 'none' });
      return;
    }
    this.setData({ currentStep: 3 });
    this.loadTimeSlots();
  },

  onDateChange: function (e) {
    this.setData({ appointmentDate: e.detail.value });
    this.loadTimeSlots();
  },

  loadTimeSlots: function () {
    var that = this;
    var doctorId = this.data.selectedPrimaryDoctorId;
    var date = this.data.appointmentDate;
    if (!doctorId || !date) return;
    request.request({
      url: '/cross-campus/schedule/doctor/' + doctorId + '/time-slots?date=' + date,
      method: 'GET',
      success: function (data) {
        that.setData({
          timeSlotList: data || [],
          selectedTimeSlot: null,
          selectedTimeSlotName: ''
        });
      },
      fail: function () {
        that.setData({
          timeSlotList: [
            { code: 0, name: '上午', startTime: '08:30', endTime: '12:00', available: true },
            { code: 1, name: '下午', startTime: '14:00', endTime: '17:30', available: true }
          ]
        });
      }
    });
  },

  selectTimeSlot: function (e) {
    var code = e.currentTarget.dataset.code;
    var available = e.currentTarget.dataset.available;
    if (!available) {
      wx.showToast({ title: '该时间段已约满', icon: 'none' });
      return;
    }
    var slot = this.data.timeSlotList.find(function (s) { return s.code === code; });
    this.setData({
      selectedTimeSlot: code,
      selectedTimeSlotName: slot ? slot.name : ''
    });
  },

  goBackStep3: function () {
    this.setData({ currentStep: 3 });
  },

  goToStep4: function () {
    if (!this.data.appointmentDate) {
      wx.showToast({ title: '请选择会诊日期', icon: 'none' });
      return;
    }
    if (this.data.selectedTimeSlot === null) {
      wx.showToast({ title: '请选择时间段', icon: 'none' });
      return;
    }
    this.setData({ currentStep: 4 });
  },

  onDescInput: function (e) {
    this.setData({ description: e.detail.value });
  },

  submitAppointment: function () {
    var that = this;
    if (!app.globalData.patientId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    wx.showLoading({ title: '提交中...' });
    request.request({
      url: '/cross-campus/consultation/create',
      method: 'POST',
      data: {
        patientId: app.globalData.patientId,
        sourceCampusId: this.data.sourceCampusId,
        targetCampusId: this.data.selectedTargetCampusId,
        primaryDoctorId: this.data.selectedPrimaryDoctorId,
        assistantDoctorIds: this.data.selectedAssistantDoctorIds,
        appointmentDate: this.data.appointmentDate,
        timeSlot: this.data.selectedTimeSlot,
        description: this.data.description,
        patientSymptoms: this.data.description,
        consultationType: 2
      },
      success: function (data) {
        wx.hideLoading();
        that.setData({ submitting: false });
        wx.showModal({
          title: '申请提交成功',
          content: '已向目标院区医生发送会诊邀请，请耐心等待医生确认。30分钟内未确认将自动取消。',
          showCancel: false,
          confirmText: '查看申请',
          success: function (res) {
            if (res.confirm) {
              wx.redirectTo({
                url: '/pages/cross-campus-list/cross-campus-list'
              });
            }
          }
        });
      },
      fail: function (res) {
        wx.hideLoading();
        that.setData({ submitting: false });
        wx.showToast({
          title: res && res.message ? res.message : '提交失败',
          icon: 'none'
        });
      }
    });
  }
});
