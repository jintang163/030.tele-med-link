var app = getApp();

function request(options) {
  var url = app.globalData.baseUrl + options.url;
  var method = options.method || 'GET';
  var data = options.data || {};
  var header = {
    'Content-Type': 'application/json'
  };
  var token = wx.getStorageSync('token');
  if (token) {
    header['Authorization'] = 'Bearer ' + token;
  }

  return new Promise(function (resolve, reject) {
    wx.request({
      url: url,
      method: method,
      data: data,
      header: header,
      success: function (res) {
        if (res.statusCode === 401) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          wx.removeStorageSync('patientId');
          app.globalData.userInfo = null;
          app.globalData.patientId = null;
          wx.redirectTo({ url: '/pages/index/index' });
          reject({ message: '登录已过期，请重新登录' });
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          var result = res.data;
          if (result.code === 0 || result.code === 200) {
            resolve(result.data);
          } else {
            wx.showToast({ title: result.message || '请求失败', icon: 'none' });
            reject(result);
          }
        } else {
          wx.showToast({ title: '网络请求失败', icon: 'none' });
          reject(res);
        }
      },
      fail: function (err) {
        wx.showToast({ title: '网络连接失败', icon: 'none' });
        reject(err);
      }
    });
  });
}

var MediasoupRoom = (function () {
  function MediasoupRoom(roomId) {
    this.roomId = roomId;
    this.userId = null;
    this.localVideoUrl = '';
    this.remoteVideoUrl = '';
    this.nodeId = null;
    this.nodeUrl = '';
    this.turnServer = {
      urls: [],
      username: '',
      credential: ''
    };
    this.currentResolution = '1280x720';
    this.qualityTimer = null;
    this._isMuted = false;
    this._isCameraOff = false;
    this._pusherContext = null;
  }

  MediasoupRoom.prototype.joinRoom = async function (userId) {
    var that = this;
    this.userId = userId;

    var nearestNode = await request({
      url: '/mediasoup/node/nearest',
      method: 'GET'
    });
    this.nodeId = nearestNode.id;
    this.nodeUrl = 'webrtc://' + nearestNode.ip + ':' + nearestNode.port;

    var turnConfig = await request({
      url: '/mediasoup/turn-config',
      method: 'GET'
    });
    this.turnServer = {
      urls: turnConfig.urls || [],
      username: turnConfig.username || '',
      credential: turnConfig.credential || ''
    };

    var turnParam = encodeURIComponent(JSON.stringify(this.turnServer));
    this.localVideoUrl = this.nodeUrl + '/live/' + this.roomId + '_' + userId + '?turn=' + turnParam;
    this.remoteVideoUrl = this.nodeUrl + '/live/' + this.roomId + '_doctor?turn=' + turnParam;

    this.currentResolution = '1280x720';

    this._startQualityMonitor();

    return {
      localVideoUrl: this.localVideoUrl,
      remoteVideoUrl: this.remoteVideoUrl,
      currentResolution: this.currentResolution
    };
  };

  MediasoupRoom.prototype._startQualityMonitor = function () {
    var that = this;
    if (this.qualityTimer) {
      clearInterval(this.qualityTimer);
    }
    this.qualityTimer = setInterval(function () {
      that._reportQuality();
    }, 3000);
  };

  MediasoupRoom.prototype._reportQuality = function () {
    var that = this;
    var lossRate = that._lastLossRate || 0;
    request({
      url: '/mediasoup/quality/report',
      method: 'POST',
      data: {
        roomId: that.roomId,
        userId: that.userId,
        nodeId: that.nodeId,
        lossRate: lossRate,
        resolution: that.currentResolution
      }
    }).then(function (advice) {
      if (advice.shouldDowngrade) {
        that._downgradeResolution();
      } else if (advice.shouldUpgrade) {
        that._upgradeResolution();
      }
    }).catch(function () {});
  };

  MediasoupRoom.prototype._downgradeResolution = function () {
    var resolutions = ['1920x1080', '1280x720', '854x480', '640x360'];
    var currentIndex = resolutions.indexOf(this.currentResolution);
    if (currentIndex < resolutions.length - 1) {
      this.currentResolution = resolutions[currentIndex + 1];
      this._applyVideoQuality();
    }
  };

  MediasoupRoom.prototype._upgradeResolution = function () {
    var resolutions = ['1920x1080', '1280x720', '854x480', '640x360'];
    var currentIndex = resolutions.indexOf(this.currentResolution);
    if (currentIndex > 0) {
      this.currentResolution = resolutions[currentIndex - 1];
      this._applyVideoQuality();
    }
  };

  MediasoupRoom.prototype._applyVideoQuality = function () {
    if (!this._pusherContext) {
      try {
        this._pusherContext = wx.createLivePusherContext('localPusher');
      } catch (e) {}
    }
    if (this._pusherContext && this._pusherContext.setVideoQuality) {
      var qualityMap = {
        '1920x1080': 'high',
        '1280x720': 'medium',
        '854x480': 'low',
        '640x360': 'low'
      };
      this._pusherContext.setVideoQuality(qualityMap[this.currentResolution] || 'medium');
    }
  };

  MediasoupRoom.prototype.reportLossRate = function (lossRate) {
    this._lastLossRate = lossRate;
  };

  MediasoupRoom.prototype.getIceServers = function () {
    var that = this;
    return this.turnServer.urls.map(function (url) {
      return {
        urls: url,
        username: that.turnServer.username,
        credential: that.turnServer.credential
      };
    });
  };

  MediasoupRoom.prototype.toggleMute = function () {
    this._isMuted = !this._isMuted;
    return this._isMuted;
  };

  MediasoupRoom.prototype.toggleCamera = function () {
    this._isCameraOff = !this._isCameraOff;
    return this._isCameraOff;
  };

  MediasoupRoom.prototype.leaveRoom = function () {
    if (this.qualityTimer) {
      clearInterval(this.qualityTimer);
      this.qualityTimer = null;
    }
    this.userId = null;
    this.localVideoUrl = '';
    this.remoteVideoUrl = '';
  };

  return MediasoupRoom;
})();

module.exports = {
  MediasoupRoom: MediasoupRoom
};
