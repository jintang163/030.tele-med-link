var app = getApp();
var request = require('../../utils/request');

Page({
  data: {
    recordings: [],
    loading: true,
    currentPlayback: null,
    mp4Url: '',
    playbackToken: '',
    showPlayer: false
  },

  onLoad: function () {
    this.fetchRecordings();
  },

  fetchRecordings: function () {
    var that = this;
    that.setData({ loading: true });
    var patientId = app.globalData.patientId;
    if (!patientId) {
      that.setData({ loading: false });
      return;
    }
    request.request({
      url: '/video/recording/patient/' + patientId,
      method: 'GET',
      success: function (data) {
        var recordings = (data || []).map(function (r) {
          var statusMap = {
            0: '等待授权',
            1: '录制中',
            2: '上传中',
            3: '处理中',
            4: '已完成',
            5: '失败',
            6: '已取消',
            7: '已过期'
          };
          r.statusText = statusMap[r.status] || '未知';
          if (r.startTime) {
            r.startTimeText = new Date(r.startTime).toLocaleString();
          }
          if (r.totalDuration) {
            var mins = Math.floor(r.totalDuration / 60);
            var secs = r.totalDuration % 60;
            r.durationText = mins + '分' + secs + '秒';
          }
          return r;
        });
        that.setData({
          recordings: recordings,
          loading: false
        });
      },
      fail: function () {
        that.setData({ loading: false });
        wx.showToast({ title: '获取录制列表失败', icon: 'none' });
      }
    });
  },

  playRecording: function (e) {
    var that = this;
    var recordingId = e.currentTarget.dataset.id;
    var patientId = app.globalData.patientId;

    request.request({
      url: '/video/playback/auth',
      method: 'POST',
      data: {
        recordingId: recordingId,
        userId: patientId,
        userRole: 'PATIENT',
        expireMinutes: 60
      },
      success: function (data) {
        that.setData({
          currentPlayback: recordingId,
          mp4Url: data.mp4Url || data.hlsPlaylistUrl,
          playbackToken: data.authToken,
          showPlayer: true
        });

        request.request({
          url: '/video/playback/increment',
          method: 'POST',
          header: {
            'X-Playback-Token': data.authToken
          }
        });
      },
      fail: function () {
        wx.showToast({ title: '获取播放授权失败', icon: 'none' });
      }
    });
  },

  closePlayer: function () {
    this.setData({
      showPlayer: false,
      currentPlayback: null,
      mp4Url: '',
      playbackToken: ''
    });
  },

  onPullDownRefresh: function () {
    this.fetchRecordings();
    wx.stopPullDownRefresh();
  }
});
