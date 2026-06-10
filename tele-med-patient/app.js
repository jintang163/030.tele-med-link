App({
  globalData: {
    baseUrl: 'http://localhost:8080/api',
    userInfo: null,
    patientId: null
  },

  onLaunch: function () {
    try {
      var userInfo = wx.getStorageSync('userInfo');
      var patientId = wx.getStorageSync('patientId');
      if (userInfo) {
        this.globalData.userInfo = userInfo;
      }
      if (patientId) {
        this.globalData.patientId = patientId;
      }
    } catch (e) {
    }
  }
});
