var request = require('../../utils/request');
var app = getApp();

Page({
  data: {
    patientId: null,
    patientName: '',
    idCardNo: '',
    faceImage: '',
    faceImageBase64: '',
    verifying: false,
    verifyType: 1,
    typeOptions: [
      { value: 1, label: '身份证人脸比对' },
      { value: 2, label: '活体检测' },
      { value: 3, label: '活体+身份证比对' }
    ],
    remainingAttempts: 3,
    locked: false,
    lockTime: '',
    failReason: '',
    fromPage: ''
  },

  onLoad: function (options) {
    var patientId = options.patientId || app.globalData.patientId;
    var fromPage = options.from || '';
    var consultationId = options.consultationId || '';
    this.setData({
      patientId: patientId,
      fromPage: fromPage,
      consultationId: consultationId
    });
    this.loadVerifyStatus();
    this.loadPatientInfo();
  },

  loadPatientInfo: function () {
    var that = this;
    var patientId = that.data.patientId;
    if (!patientId) return;
    request.request({
      url: '/patient/detail/' + patientId,
      method: 'GET',
      success: function (data) {
        if (data) {
          that.setData({
            patientName: data.name || '',
            idCardNo: data.idCard || ''
          });
        }
      },
      fail: function () {
      }
    });
  },

  loadVerifyStatus: function () {
    var that = this;
    var patientId = that.data.patientId;
    if (!patientId) return;
    request.request({
      url: '/face-verify/status/' + patientId,
      method: 'GET',
      success: function (data) {
        if (data) {
          that.setData({
            remainingAttempts: data.remainingAttempts != null ? data.remainingAttempts : 3,
            locked: data.locked || false,
            lockTime: data.lockTime || ''
          });
        }
      }
    });
  },

  onTypeChange: function (e) {
    var index = e.detail.value;
    this.setData({
      verifyType: this.data.typeOptions[index].value
    });
  },

  chooseImage: function () {
    var that = this;
    if (that.data.locked) {
      wx.showToast({ title: '账户已锁定，请联系管理员', icon: 'none' });
      return;
    }
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera', 'album'],
      camera: 'front',
      success: function (res) {
        var tempFile = res.tempFiles[0];
        var filePath = tempFile.tempFilePath;
        that.setData({ faceImage: filePath });
        wx.getFileSystemManager().readFile({
          filePath: filePath,
          encoding: 'base64',
          success: function (res) {
            that.setData({ faceImageBase64: res.data });
          },
          fail: function () {
            wx.showToast({ title: '读取图片失败', icon: 'none' });
          }
        });
      }
    });
  },

  onNameInput: function (e) {
    this.setData({ patientName: e.detail.value });
  },

  onIdCardInput: function (e) {
    this.setData({ idCardNo: e.detail.value });
  },

  startLiveVerify: function () {
    var that = this;
    if (that.data.locked) {
      wx.showToast({ title: '账户已锁定，请联系管理员', icon: 'none' });
      return;
    }
    if (!that.data.patientName || !that.data.idCardNo) {
      wx.showToast({ title: '请填写姓名和身份证号', icon: 'none' });
      return;
    }
    if (wx.startFacialRecognitionVerify) {
      wx.startFacialRecognitionVerify({
        name: that.data.patientName,
        idCardNumber: that.data.idCardNo,
        checkAliveType: 2,
        success: function (res) {
          var verifyResult = res.verifyResult;
          that.setData({ faceImageBase64: verifyResult || '' });
          that.submitVerify(3);
        },
        fail: function (res) {
          wx.showToast({ title: res.errMsg || '活体检测失败', icon: 'none' });
        }
      });
    } else {
      wx.showToast({ title: '当前微信版本不支持活体检测', icon: 'none' });
    }
  },

  submitVerify: function (type) {
    var that = this;
    if (that.data.verifying) return;
    if (that.data.locked) {
      wx.showToast({ title: '账户已锁定', icon: 'none' });
      return;
    }
    if (!that.data.patientName || !that.data.idCardNo) {
      wx.showToast({ title: '请填写姓名和身份证号', icon: 'none' });
      return;
    }
    if (!that.data.faceImageBase64) {
      wx.showToast({ title: '请先拍摄/上传人脸照片', icon: 'none' });
      return;
    }

    var verifyType = type || that.data.verifyType;

    that.setData({ verifying: true, failReason: '' });

    request.request({
      url: '/face-verify/verify',
      method: 'POST',
      data: {
        patientId: that.data.patientId,
        verifyType: verifyType,
        idCardName: that.data.patientName,
        idCardNo: that.data.idCardNo,
        faceImageBase64: that.data.faceImageBase64,
        verifySource: 'patient-mini'
      },
      success: function (data) {
        if (data && data.passed) {
          wx.setStorageSync('faceToken', data.faceToken);
          wx.setStorageSync('faceTokenExpire', data.tokenExpireTime);

          wx.showToast({ title: '核验通过', icon: 'success' });

          setTimeout(function () {
            if (that.data.fromPage === 'conclusion' && that.data.consultationId) {
              wx.redirectTo({
                url: '/pages/conclusion/conclusion?consultationId=' + that.data.consultationId +
                     '&patientId=' + (that.data.patientId || '') + '&skipVerify=1'
              });
            } else {
              wx.navigateBack();
            }
          }, 1000);
        } else {
          var msg = data && data.failureReason ? data.failureReason : '核验失败';
          that.setData({
            failReason: msg,
            remainingAttempts: data && data.remainingAttempts != null ? data.remainingAttempts : (that.data.remainingAttempts - 1),
            locked: data && data.locked
          });
          wx.showToast({ title: msg, icon: 'none' });

          if (data && data.locked) {
            that.setData({ lockTime: data.lockTime || '' });
          }
        }
      },
      fail: function (err) {
        var msg = err && err.message ? err.message : '核验失败';
        that.setData({ failReason: msg });
        wx.showToast({ title: msg, icon: 'none' });
      },
      complete: function () {
        that.setData({ verifying: false });
      }
    });
  },

  onVerifyClick: function () {
    var type = this.data.verifyType;
    if (type === 2 || type === 3) {
      this.startLiveVerify();
    } else {
      this.submitVerify(1);
    }
  },

  contactAdmin: function () {
    wx.showToast({ title: '请联系医院管理员解锁', icon: 'none' });
  }
});
