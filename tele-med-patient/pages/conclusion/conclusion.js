var request = require('../../utils/request');

Page({
  data: {
    consultation: {},
    conclusion: null
  },

  onLoad: function (options) {
    var consultationId = options.consultationId;
    if (consultationId) {
      this.loadConsultationDetail(consultationId);
      this.loadConclusion(consultationId);
    }
  },

  loadConsultationDetail: function (consultationId) {
    var that = this;
    request.request({
      url: '/consultation/detail/' + consultationId,
      method: 'GET',
      success: function (data) {
        var statusMap = {
          0: '等待中',
          1: '进行中',
          2: '已完成',
          3: '已取消'
        };
        data.statusText = statusMap[data.status] || '未知';
        that.setData({
          consultation: data
        });
      }
    });
  },

  loadConclusion: function (consultationId) {
    var that = this;
    request.request({
      url: '/conclusion/detail?consultationId=' + consultationId,
      method: 'GET',
      success: function (data) {
        that.setData({
          conclusion: data || null
        });
      }
    });
  },

  openFile: function (e) {
    var url = e.currentTarget.dataset.url;
    if (url) {
      wx.downloadFile({
        url: url,
        success: function (res) {
          var filePath = res.tempFilePath;
          wx.openDocument({
            filePath: filePath,
            fail: function () {
              wx.showToast({ title: '文件打开失败', icon: 'none' });
            }
          });
        },
        fail: function () {
          wx.showToast({ title: '文件下载失败', icon: 'none' });
        }
      });
    }
  }
});
