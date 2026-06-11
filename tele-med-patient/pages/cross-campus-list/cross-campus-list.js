var app = getApp();
var request = require('../../utils/request');

var statusMap = {
  0: '待确认',
  1: '进行中',
  2: '已完成',
  3: '已取消'
};

Page({
  data: {
    loading: false,
    activeTab: 'all',
    consultationList: [],
    emptyTipText: '暂无申请记录',
    showDetail: false,
    detail: null
  },

  onShow: function () {
    this.loadList();
  },

  onPullDownRefresh: function () {
    this.loadList();
    wx.stopPullDownRefresh();
  },

  switchTab: function (e) {
    var tab = e.currentTarget.dataset.tab;
    this.setData({ activeTab: tab });
    this.loadList();
  },

  loadList: function () {
    var that = this;
    var patientId = app.globalData.patientId;
    if (!patientId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    this.setData({ loading: true });

    var status = null;
    if (this.data.activeTab === 'pending') status = 0;
    else if (this.data.activeTab === 'confirmed') status = 1;
    else if (this.data.activeTab === 'done') status = 2;

    request.request({
      url: '/consultation/patient/' + patientId,
      method: 'GET',
      success: function (data) {
        var list = (data || []).filter(function (item) {
          return item.crossCampus === true;
        }).map(function (item) {
          if (!item.statusText) {
            item.statusText = statusMap[item.status] || '未知';
          }
          return item;
        });

        if (status !== null) {
          list = list.filter(function (item) { return item.status === status; });
        }

        list.sort(function (a, b) {
          return new Date(b.createTime) - new Date(a.createTime);
        });

        var tipText = '暂无申请记录';
        if (that.data.activeTab === 'pending') tipText = '暂无待确认申请';
        else if (that.data.activeTab === 'confirmed') tipText = '暂无进行中的申请';
        else if (that.data.activeTab === 'done') tipText = '暂无已完成的申请';

        that.setData({
          consultationList: list,
          loading: false,
          emptyTipText: tipText
        });
      },
      fail: function () {
        that.setData({ loading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  isExpired: function (time) {
    if (!time) return false;
    return new Date(time.replace(/-/g, '/')).getTime() < Date.now();
  },

  viewDetail: function (e) {
    var that = this;
    var id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.showLoading({ title: '加载中...' });
    request.request({
      url: '/cross-campus/consultation/' + id,
      method: 'GET',
      success: function (data) {
        wx.hideLoading();
        if (data && !data.statusText) {
          data.statusText = statusMap[data.status] || '未知';
        }
        that.setData({
          detail: data,
          showDetail: true
        });
      },
      fail: function () {
        wx.hideLoading();
        wx.showToast({ title: '加载详情失败', icon: 'none' });
      }
    });
  },

  hideDetail: function () {
    this.setData({ showDetail: false, detail: null });
  },

  stopPropagation: function () {},

  cancelConsultation: function (e) {
    var that = this;
    var id = e.currentTarget.dataset.id;
    var patientId = app.globalData.patientId;
    if (!patientId || !id) return;

    wx.showModal({
      title: '取消申请',
      content: '确定要取消该跨院区会诊申请吗？',
      success: function (res) {
        if (!res.confirm) return;
        wx.showLoading({ title: '取消中...' });
        request.request({
          url: '/cross-campus/consultation/cancel?consultationId=' + id + '&patientId=' + patientId,
          method: 'POST',
          success: function () {
            wx.hideLoading();
            wx.showToast({ title: '已取消', icon: 'success' });
            that.setData({ showDetail: false });
            that.loadList();
          },
          fail: function () {
            wx.hideLoading();
            wx.showToast({ title: '操作失败', icon: 'none' });
          }
        });
      }
    });
  },

  enterRoom: function (e) {
    var id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({
      url: '/pages/consultation/consultation?consultationId=' + id
    });
  },

  viewConclusion: function (e) {
    var id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.navigateTo({
      url: '/pages/conclusion/conclusion?consultationId=' + id
    });
  },

  openConclusionFile: function (e) {
    var url = e.currentTarget.dataset.url;
    if (!url) return;
    wx.showToast({ title: '打开：' + url, icon: 'none' });
  }
});
