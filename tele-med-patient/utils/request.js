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
        if (options.fail) {
          options.fail({ message: '登录已过期，请重新登录' });
        }
        return;
      }
      if (res.statusCode >= 200 && res.statusCode < 300) {
        var result = res.data;
        if (result.code === 0 || result.code === 200) {
          if (options.success) {
            options.success(result.data);
          }
        } else {
          wx.showToast({ title: result.message || '请求失败', icon: 'none' });
          if (options.fail) {
            options.fail(result);
          }
        }
      } else {
        wx.showToast({ title: '网络请求失败', icon: 'none' });
        if (options.fail) {
          options.fail(res);
        }
      }
    },
    fail: function (err) {
      wx.showToast({ title: '网络连接失败', icon: 'none' });
      if (options.fail) {
        options.fail(err);
      }
    }
  });
}

module.exports = {
  request: request
};
