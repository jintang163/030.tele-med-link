var app = getApp();
var request = require('../../utils/request');
var signaling = require('../../utils/websocket');
var mediasoup = require('../../utils/mediasoup');

Page({
  data: {
    consultationId: null,
    consultationNo: '',
    roomId: '',
    localVideoUrl: '',
    remoteVideoUrl: '',
    chatMessages: [],
    inputMessage: '',
    isMuted: false,
    isCameraOff: false,
    status: 0,
    statusText: '等待中',
    showChat: true,
    scrollToId: '',
    currentResolution: '1280x720',
    showRecordingAuth: false,
    recordingConsultationId: null,
    recordingId: null
  },

  signalingClient: null,
  mediasoupRoom: null,

  onLoad: function (options) {
    var consultationId = options.consultationId;
    this.setData({ consultationId: consultationId });
    this.loadConsultationDetail(consultationId);
  },

  loadConsultationDetail: function (consultationId) {
    var that = this;
    request.request({
      url: '/consultation/detail/' + consultationId,
      method: 'GET',
      success: function (data) {
        that.setData({
          consultationNo: data.consultationNo,
          roomId: data.roomId,
          status: data.status
        });
        that.updateStatusText(data.status);
        that.initVideo();
        that.connectSignaling();
      }
    });
  },

  updateStatusText: function (status) {
    var statusMap = {
      0: '等待中',
      1: '进行中',
      2: '已完成',
      3: '已取消'
    };
    this.setData({
      statusText: statusMap[status] || '未知',
      status: status
    });
  },

  initVideo: async function () {
    var that = this;
    var roomId = this.data.roomId;
    if (!roomId) {
      return;
    }

    var patientId = app.globalData.patientId;
    this.mediasoupRoom = new mediasoup.MediasoupRoom(roomId);
    try {
      var result = await this.mediasoupRoom.joinRoom('patient_' + patientId);
      that.setData({
        localVideoUrl: result.localVideoUrl,
        remoteVideoUrl: result.remoteVideoUrl,
        currentResolution: result.currentResolution
      });
    } catch (e) {
      wx.showToast({ title: '初始化视频失败', icon: 'none' });
    }
  },

  connectSignaling: function () {
    var that = this;
    var patientId = app.globalData.patientId;
    if (!patientId) {
      return;
    }

    this.signalingClient = signaling.connectSignaling('patient_' + patientId);

    this.signalingClient.onOpen(function () {
      that.signalingClient.send({
        type: 'join',
        roomId: that.data.roomId,
        userId: 'patient_' + patientId,
        role: 'patient'
      });
    });

    this.signalingClient.onMessage(function (data) {
      that.handleSignalingMessage(data);
    });
  },

  handleSignalingMessage: function (data) {
    var that = this;

    if (data.type === 'offer') {
      that.signalingClient.send({
        type: 'answer',
        sdp: 'placeholder_answer_sdp',
        targetUserId: data.fromUserId,
        roomId: that.data.roomId
      });
    } else if (data.type === 'ice-candidate') {
    } else if (data.type === 'mediasoup-producer-created') {
    } else if (data.type === 'mediasoup-consumer-created') {
    } else if (data.type === 'mediasoup-quality-advice') {
      if (that.mediasoupRoom) {
        if (data.shouldDowngrade) {
          that.mediasoupRoom._downgradeResolution();
        } else if (data.shouldUpgrade) {
          that.mediasoupRoom._upgradeResolution();
        }
        that.setData({
          currentResolution: that.mediasoupRoom.currentResolution
        });
      }
    } else if (data.type === 'chat') {
      var now = new Date();
      var timeStr = now.getHours() + ':' + (now.getMinutes() < 10 ? '0' : '') + now.getMinutes();
      var messages = that.data.chatMessages.concat({
        content: data.content,
        fromSelf: false,
        time: timeStr
      });
      that.setData({
        chatMessages: messages,
        scrollToId: 'msg-' + (messages.length - 1)
      });
    } else if (data.type === 'status') {
      that.updateStatusText(data.status);
    } else if (data.type === 'video-recording-request') {
      var payload = data.payload || data;
      that.setData({
        showRecordingAuth: true,
        recordingConsultationId: payload.consultationId,
        recordingId: payload.recordingId
      });
    } else if (data.type === 'video-recording-status') {
      var payload = data.payload || data;
      if (payload.status === 6) {
        that.setData({
          showRecordingAuth: false,
          recordingConsultationId: null,
          recordingId: null
        });
        wx.showToast({ title: '录制授权已被拒绝', icon: 'none' });
      }
    } else if (data.type === 'video-recording-auth') {
      var payload = data.payload || data;
      if (!payload.authorized) {
        that.setData({
          showRecordingAuth: false,
          recordingConsultationId: null,
          recordingId: null
        });
      }
    }
  },

  sendMessage: function () {
    var message = this.data.inputMessage.trim();
    if (!message) {
      return;
    }

    if (this.signalingClient) {
      this.signalingClient.send({
        type: 'chat',
        content: message,
        roomId: this.data.roomId
      });
    }

    var now = new Date();
    var timeStr = now.getHours() + ':' + (now.getMinutes() < 10 ? '0' : '') + now.getMinutes();
    var messages = this.data.chatMessages.concat({
      content: message,
      fromSelf: true,
      time: timeStr
    });
    this.setData({
      chatMessages: messages,
      inputMessage: '',
      scrollToId: 'msg-' + (messages.length - 1)
    });
  },

  onInputChange: function (e) {
    this.setData({ inputMessage: e.detail.value });
  },

  toggleMute: function () {
    var isMuted;
    if (this.mediasoupRoom) {
      isMuted = this.mediasoupRoom.toggleMute();
    } else {
      isMuted = !this.data.isMuted;
    }
    this.setData({ isMuted: isMuted });
  },

  toggleCamera: function () {
    var isCameraOff;
    if (this.mediasoupRoom) {
      isCameraOff = this.mediasoupRoom.toggleCamera();
    } else {
      isCameraOff = !this.data.isCameraOff;
    }
    this.setData({ isCameraOff: isCameraOff });
  },

  switchCamera: function () {
    var that = this;
    wx.createLivePlayerContext('localPusher', that).switchCamera({
      success: function () {},
      fail: function () {}
    });
  },

  toggleChat: function () {
    this.setData({ showChat: !this.data.showChat });
  },

  respondRecordingAuth: function (e) {
    var that = this;
    var authorized = e.currentTarget.dataset.authorized;
    var consultationId = that.data.recordingConsultationId;
    var recordingId = that.data.recordingId;
    var patientId = app.globalData.patientId;

    that.setData({
      showRecordingAuth: false,
      recordingConsultationId: null,
      recordingId: null
    });

    if (that.signalingClient) {
      that.signalingClient.send({
        type: 'video-recording-auth',
        consultationId: consultationId,
        recordingId: recordingId,
        userRole: 'PATIENT',
        userId: patientId,
        authorized: authorized
      });
    }

    request.request({
      url: '/video/recording/authorize',
      method: 'POST',
      data: {
        consultationId: consultationId,
        userId: patientId,
        userRole: 'PATIENT',
        authorized: authorized
      },
      success: function () {
        if (authorized) {
          wx.showToast({ title: '已同意录制', icon: 'success' });
        } else {
          wx.showToast({ title: '已拒绝录制', icon: 'none' });
        }
      }
    });
  },

  endConsultation: function () {
    var that = this;
    wx.showModal({
      title: '结束会诊',
      content: '确定要结束本次会诊吗？',
      success: function (res) {
        if (res.confirm) {
          request.request({
            url: '/consultation/finish',
            method: 'POST',
            data: {
              consultationId: that.data.consultationId
            },
            success: function () {
              if (that.signalingClient) {
                that.signalingClient.close();
              }
              wx.navigateBack();
            }
          });
        }
      }
    });
  },

  onRemotePlayerStateChange: function (e) {
    var that = this;
    if (e.detail && e.detail.code === 1003 && that.mediasoupRoom) {
      var lossRate = e.detail.info && e.detail.info.lossRate ? e.detail.info.lossRate : 0;
      if (lossRate > 0.05) {
        that.mediasoupRoom.reportLossRate(lossRate);
      }
    }
  },

  onLocalPusherStateChange: function (e) {
  },

  onUnload: function () {
    if (this.signalingClient) {
      this.signalingClient.close();
    }
    if (this.mediasoupRoom) {
      this.mediasoupRoom.leaveRoom();
    }
  }
});
