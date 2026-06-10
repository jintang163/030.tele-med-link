var app = getApp();
var request = require('../../utils/request');
var signaling = require('../../utils/websocket');

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
    scrollToId: ''
  },

  signalingClient: null,

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

  initVideo: function () {
    var that = this;
    var roomId = this.data.roomId;
    if (!roomId) {
      return;
    }

    var pushUrl = 'webrtc://localhost/live/' + roomId + '_patient';
    var pullUrl = 'webrtc://localhost/live/' + roomId + '_doctor';

    that.setData({
      localVideoUrl: pushUrl,
      remoteVideoUrl: pullUrl
    });
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
    this.setData({ isMuted: !this.data.isMuted });
  },

  toggleCamera: function () {
    this.setData({ isCameraOff: !this.data.isCameraOff });
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
  },

  onLocalPusherStateChange: function (e) {
  },

  onUnload: function () {
    if (this.signalingClient) {
      this.signalingClient.close();
    }
  }
});
