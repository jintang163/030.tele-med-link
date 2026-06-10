App({
  globalData: {
    baseUrl: 'http://localhost:8080/api',
    userInfo: null,
    patientId: null,
    hospitalId: null,
    campusId: null,
    hospitalName: '',
    campusName: ''
  },

  onLaunch: function () {
    try {
      var userInfo = wx.getStorageSync('userInfo');
      var patientId = wx.getStorageSync('patientId');
      var hospitalId = wx.getStorageSync('hospitalId');
      var campusId = wx.getStorageSync('campusId');
      var hospitalName = wx.getStorageSync('hospitalName');
      var campusName = wx.getStorageSync('campusName');
      if (userInfo) {
        this.globalData.userInfo = userInfo;
      }
      if (patientId) {
        this.globalData.patientId = patientId;
      }
      if (hospitalId) {
        this.globalData.hospitalId = hospitalId;
      }
      if (campusId) {
        this.globalData.campusId = campusId;
      }
      if (hospitalName) {
        this.globalData.hospitalName = hospitalName;
      }
      if (campusName) {
        this.globalData.campusName = campusName;
      }
    } catch (e) {
    }
  }
});
