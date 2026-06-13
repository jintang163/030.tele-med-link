var request = require('../../utils/request');

Page({
  data: {
    consultation: {},
    conclusion: null,
    signatureList: [],
    allSigned: false,
    pdfDownloading: false
  },

  onLoad: function (options) {
    var consultationId = options.consultationId;
    if (consultationId) {
      this.setData({ consultationId: consultationId });
      this.loadConsultationDetail(consultationId);
      this.loadConclusion(consultationId);
      this.loadSignatureStatus(consultationId);
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

  loadSignatureStatus: function (consultationId) {
    var that = this;
    request.request({
      url: '/signature/consultation/' + consultationId,
      method: 'GET',
      success: function (data) {
        var list = data || [];
        var allSigned = list.length > 0 && list.every(function (s) { return s.signStatus === 1; });
        that.setData({
          signatureList: list,
          allSigned: allSigned
        });
      }
    });
  },

  downloadPdf: function () {
    var that = this;
    var consultationId = that.data.consultationId;
    if (!consultationId) return;

    that.setData({ pdfDownloading: true });

    request.request({
      url: '/signature/pdf-url/' + consultationId,
      method: 'GET',
      success: function (data) {
        var pdfUrl = data && data.url;
        if (pdfUrl) {
          wx.downloadFile({
            url: pdfUrl,
            success: function (res) {
              var filePath = res.tempFilePath;
              wx.openDocument({
                filePath: filePath,
                fileType: 'pdf',
                success: function () {
                  wx.showToast({ title: '文件已打开', icon: 'success' });
                },
                fail: function () {
                  wx.showToast({ title: '文件打开失败', icon: 'none' });
                }
              });
            },
            fail: function () {
              wx.showToast({ title: '下载失败', icon: 'none' });
            }
          });
        } else {
          wx.showToast({ title: 'PDF尚未生成', icon: 'none' });
        }
        that.setData({ pdfDownloading: false });
      },
      fail: function () {
        wx.showToast({ title: '获取PDF失败', icon: 'none' });
        that.setData({ pdfDownloading: false });
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
