function connectSignaling(userId) {
  var socketTask = wx.connectSocket({
    url: 'ws://localhost:8080/ws/signaling?userId=' + userId,
    success: function () {
    },
    fail: function (err) {
      console.error('WebSocket connect failed:', err);
    }
  });

  var handlers = {
    onMessageCallback: null,
    onOpenCallback: null
  };

  wx.onSocketOpen(function (res) {
    if (handlers.onOpenCallback) {
      handlers.onOpenCallback(res);
    }
  });

  wx.onSocketMessage(function (res) {
    if (handlers.onMessageCallback) {
      var data = res.data;
      try {
        data = JSON.parse(res.data);
      } catch (e) {
      }
      handlers.onMessageCallback(data);
    }
  });

  return {
    send: function (data) {
      var msg = typeof data === 'string' ? data : JSON.stringify(data);
      socketTask.send({
        data: msg,
        fail: function (err) {
          console.error('WebSocket send failed:', err);
        }
      });
    },
    close: function () {
      socketTask.close({
        code: 1000,
        reason: 'normal close'
      });
    },
    onMessage: function (callback) {
      handlers.onMessageCallback = callback;
    },
    onOpen: function (callback) {
      handlers.onOpenCallback = callback;
    }
  };
}

module.exports = {
  connectSignaling: connectSignaling
};
