var app = getApp();
var request = require('../../utils/request');

Page({
  data: {
    userInfo: null,
    patientId: null,
    recentConsultations: [],
    hospitals: [],
    selectedHospitalIndex: 0
  },

  onLoad: function () {
    this.checkLogin();
  },

  onShow: function () {
    var userInfo = app.globalData.userInfo;
    var patientId = app.globalData.patientId;
    this.setData({
      userInfo: userInfo,
      patientId: patientId
    });
    if (patientId) {
      this.loadRecentConsultations();
    }
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
