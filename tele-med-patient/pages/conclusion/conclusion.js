var request = require('../../utils/request');
var app = getApp();

Page({
  data: {
    consultation: {},
    conclusion: null,
    signatureList: [],
    allSigned: false,
    pdfDownloading: false,
    pdfUrl: '',
    needFaceVerify: false,
    patientId: null,
    diagnosisSuggestion: null
  },

  onLoad: function (options) {
    var consultationId = options.consultationId;
    var patientId = options.patientId || app.globalData.patientId;
    var skipVerify = options.skipVerify === '1';

    if (consultationId) {
      this.setData({
        consultationId: consultationId,
        patientId: patientId,
        skipVerify: skipVerify
      });

      if (skipVerify) {
        this.loadAllData(consultationId);
      } else {
        this.checkFaceVerifyAndLoad(consultationId);
      }
    }
  },

  onShow: function () {
    var faceToken = wx.getStorageSync('faceToken');
    if (faceToken && this.data.consultationId && !this.data.skipVerify) {
      this.loadAllData(this.data.consultationId);
      this.loadPdfUrl();
    }
  },

  checkFaceVerifyAndLoad: function (consultationId) {
    var that = this;
    var faceToken = wx.getStorageSync('faceToken');
    if (!faceToken) {
      wx.redirectTo({
        url: '/pages/face-verify/face-verify?from=conclusion&patientId=' + (that.data.patientId || '') +
             '&consultationId=' + consultationId
      });
      return;
    }
    this.loadAllData(consultationId);
  },

  loadAllData: function (consultationId) {
    this.loadConsultationDetail(consultationId);
    this.loadConclusion(consultationId);
    this.loadSignatureStatus(consultationId);
    this.loadDiagnosisSuggestion(consultationId);
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
        if (allSigned) {
          that.loadPdfUrl();
        }
      }
    });
  },

  loadDiagnosisSuggestion: function (consultationId) {
    var that = this;
    request.request({
      url: '/diagnosis-assist/suggestion/consultation/' + consultationId,
      method: 'GET',
      success: function (data) {
        if (data && data.primaryDisease) {
          var suggestion = data;
          if (suggestion.relatedSymptoms && typeof suggestion.relatedSymptoms === 'string') {
            suggestion.relatedSymptomsList = suggestion.relatedSymptoms.split(/[,，、;；\s]+/).filter(function (s) { return s; });
          } else {
            suggestion.relatedSymptomsList = [];
          }
          if (suggestion.recommendedTests && typeof suggestion.recommendedTests === 'string') {
            suggestion.recommendedTestsList = suggestion.recommendedTests.split(/[,，、;；\s]+/).filter(function (s) { return s; });
          } else {
            suggestion.recommendedTestsList = [];
          }
          suggestion.primaryConfidenceText = suggestion.primaryConfidence
            ? Math.round(suggestion.primaryConfidence) + '%'
            : '-';
          that.setData({ diagnosisSuggestion: suggestion });
        }
      },
      fail: function () {
        // ignore - diagnosis suggestion is optional
      }
    });
  },

  loadPdfUrl: function () {
    var that = this;
    var consultationId = that.data.consultationId;
    var patientId = that.data.patientId;
    var faceToken = wx.getStorageSync('faceToken');

    request.request({
      url: '/signature/patient-pdf-url/' + consultationId + '?patientId=' + (patientId || '') + (faceToken ? '&faceToken=' + faceToken : ''),
      method: 'GET',
      success: function (data) {
        if (data && data.url) {
          that.setData({
            pdfUrl: data.url,
            needFaceVerify: false
          });
        } else {
          that.setData({
            needFaceVerify: data && data.needFaceVerify ? true : false
          });
        }
      },
      fail: function (res) {
        if (res && res.code === 401) {
          wx.removeStorageSync('faceToken');
          that.setData({ needFaceVerify: true });
        }
      }
    });
  },

  downloadPdf: function () {
    var that = this;
    var consultationId = that.data.consultationId;
    if (!consultationId) return;

    var faceToken = wx.getStorageSync('faceToken');
    if (!faceToken) {
      wx.navigateTo({
        url: '/pages/face-verify/face-verify?from=conclusion&patientId=' + (that.data.patientId || '')
      });
      return;
    }

    that.setData({ pdfDownloading: true });

    request.request({
      url: '/signature/patient-pdf-url/' + consultationId + '?patientId=' + (that.data.patientId || '') + '&faceToken=' + faceToken,
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
        } else if (data && data.needFaceVerify) {
          wx.removeStorageSync('faceToken');
          wx.navigateTo({
            url: '/pages/face-verify/face-verify?from=conclusion&patientId=' + (that.data.patientId || '')
          });
        } else {
          wx.showToast({ title: 'PDF尚未生成', icon: 'none' });
        }
        that.setData({ pdfDownloading: false });
      },
      fail: function (res) {
        if (res && (res.code === 401 || res.errMsg && res.errMsg.indexOf('401') > -1)) {
          wx.removeStorageSync('faceToken');
          wx.navigateTo({
            url: '/pages/face-verify/face-verify?from=conclusion&patientId=' + (that.data.patientId || '')
          });
        } else {
          wx.showToast({ title: '获取PDF失败', icon: 'none' });
        }
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
