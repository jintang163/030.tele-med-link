var app = getApp();
var request = require('../../utils/request');

Page({
  data: {
    userInfo: null,
    patientId: null,
    recentConsultations: [],
    hospitals: [],
    campuses: [],
    hospitalId: null,
    campusId: null,
    hospitalName: '',
    campusName: '',
    selectedHospitalIndex: 0,
    selectedCampusIndex: 0,
    showCampusPicker: false
  },

  onLoad: function () {
    this.loadHospitals();
    this.checkLogin();
  },

  onShow: function () {
    var app = getApp();
    var userInfo = app.globalData.userInfo;
    var patientId = app.globalData.patientId;
    var hospitalId = app.globalData.hospitalId;
    var campusId = app.globalData.campusId;
    var hospitalName = app.globalData.hospitalName;
    var campusName = app.globalData.campusName;
    this.setData({
      userInfo: userInfo,
      patientId: patientId,
      hospitalId: hospitalId,
      campusId: campusId,
      hospitalName: hospitalName,
      campusName: campusName
    });
    if (patientId) {
      this.loadRecentConsultations();
    }
  },

  loadHospitals: function () {
    var that = this;
    request.request({
      url: '/hospital/list',
      method: 'GET',
      success: function (data) {
        var hospitals = data || [];
        that.setData({
          hospitals: hospitals
        });
        if (hospitals.length > 0 && !that.data.hospitalId) {
          var firstHospital = hospitals[0];
          that.setData({
            hospitalId: firstHospital.id,
            hospitalName: firstHospital.name,
            selectedHospitalIndex: 0
          });
          that.loadCampuses(firstHospital.id);
        }
      }
    });
  },

  loadCampuses: function (hospitalId) {
    var that = this;
    request.request({
      url: '/hospital/' + hospitalId + '/campuses',
      method: 'GET',
      success: function (data) {
        var campuses = data || [];
        that.setData({
          campuses: campuses
        });
        if (campuses.length > 0 && !that.data.campusId) {
          var firstCampus = campuses[0];
          that.setData({
            campusId: firstCampus.id,
            campusName: firstCampus.name,
            selectedCampusIndex: 0
          });
          that.saveCampusSelection();
        }
      }
    });
  },

  showCampusPicker: function () {
    this.setData({
      showCampusPicker: true
    });
  },

  hideCampusPicker: function () {
    this.setData({
      showCampusPicker: false
    });
  },

  onHospitalChange: function (e) {
    var index = e.detail.value;
    var hospital = this.data.hospitals[index];
    this.setData({
      selectedHospitalIndex: index,
      hospitalId: hospital.id,
      hospitalName: hospital.name,
      campuses: [],
      campusId: null,
      campusName: ''
    });
    this.loadCampuses(hospital.id);
  },

  onCampusChange: function (e) {
    var index = e.detail.value;
    var campus = this.data.campuses[index];
    this.setData({
      selectedCampusIndex: index,
      campusId: campus.id,
      campusName: campus.name
    });
  },

  confirmCampusSelection: function () {
    this.saveCampusSelection();
    this.setData({
      showCampusPicker: false
    });
    wx.showToast({ title: '院区已切换', icon: 'success' });
  },

  saveCampusSelection: function () {
    var app = getApp();
    app.globalData.hospitalId = this.data.hospitalId;
    app.globalData.campusId = this.data.campusId;
    app.globalData.hospitalName = this.data.hospitalName;
    app.globalData.campusName = this.data.campusName;
    wx.setStorageSync('hospitalId', this.data.hospitalId);
    wx.setStorageSync('campusId', this.data.campusId);
    wx.setStorageSync('hospitalName', this.data.hospitalName);
    wx.setStorageSync('campusName', this.data.campusName);
  },

  checkLogin: function () {
    var that = this;
    var patientId = app.globalData.patientId;
    if (!patientId) {
      this.doLogin();
    }
  },

  doLogin: function () {
    var that = this;
    wx.login({
      success: function (res) {
        if (res.code) {
          request.request({
            url: '/wechat/login',
            method: 'POST',
            data: { code: res.code },
            success: function (data) {
              if (data && data.patientId) {
                app.globalData.patientId = data.patientId;
                app.globalData.userInfo = data.userInfo;
                wx.setStorageSync('patientId', data.patientId);
                wx.setStorageSync('userInfo', data.userInfo);
                that.setData({
                  patientId: data.patientId,
                  userInfo: data.userInfo
                });
                that.loadRecentConsultations();
              } else {
                wx.showToast({ title: '登录失败', icon: 'none' });
              }
            },
            fail: function () {
              wx.showToast({ title: '登录失败，请稍后重试', icon: 'none' });
            }
          });
        }
      }
    });
  },

  loadRecentConsultations: function () {
    var that = this;
    var patientId = app.globalData.patientId;
    if (!patientId) {
      return;
    }
    request.request({
      url: '/consultation/patient/' + patientId,
      method: 'GET',
      success: function (data) {
        var list = (data || []).slice(0, 5).map(function (item) {
          var statusMap = {
            0: '等待中',
            1: '进行中',
            2: '已完成',
            3: '已取消'
          };
          item.statusText = statusMap[item.status] || '未知';
          return item;
        });
        that.setData({
          recentConsultations: list
        });
      }
    });
  },

  startImmediateConsultation: function () {
    var that = this;
    var patientId = app.globalData.patientId;
    if (!patientId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '发起会诊...' });
    request.request({
      url: '/consultation/create',
      method: 'POST',
      data: {
        patientId: patientId,
        doctorId: null,
        type: 0
      },
      success: function (data) {
        wx.hideLoading();
        if (data && data.id) {
          wx.navigateTo({
            url: '/pages/consultation/consultation?consultationId=' + data.id
          });
        } else {
          wx.showToast({ title: '发起失败', icon: 'none' });
        }
      },
      fail: function () {
        wx.hideLoading();
        wx.showToast({ title: '发起失败', icon: 'none' });
      }
    });
  },

  navigateToAppointment: function () {
    wx.navigateTo({
      url: '/pages/appointment/appointment'
    });
  },

  viewConclusion: function (e) {
    var id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/conclusion/conclusion?consultationId=' + id
    });
  }
});
