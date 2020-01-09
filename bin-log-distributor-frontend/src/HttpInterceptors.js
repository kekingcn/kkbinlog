import axios from 'axios'
import Vue from 'vue'

// 超时时间
axios.defaults.timeout = 60000;
axios.interceptors.request.use(
    config => {

      var tokenData = JSON.parse(sessionStorage.getItem('user'));
      if (tokenData != null) {
        config.headers["token"] = tokenData.token ;
        config.headers["username"] = tokenData.username;
      } else {
      }
        return config
    },
    error => {
        new Vue().$message({message: '加载超时',type: 'error'});
        return Promise.reject(error)
    }
);
axios.interceptors.response.use(
    data => {
        return data
    },
    error => {
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    sessionStorage.removeItem('user');
                    new Vue().$message({message: '没有权限',type: 'warning',onClose:function () {
                        util.logout()
                    }});
                    break;
                case 403:
                   sessionStorage.removeItem('user');
                    new Vue().$message({message: '没有权限',type: 'warning',onClose:function () {
                        util.logout()
                    }});
                    break;
                case 500:
                    new Vue().$message({message: '出现异常',type: 'error'});
                    break;
            }
        } else {
       //     new Vue().$message({message: '请求超时，请检查网络',type: 'warning'});
        }
        return Promise.reject(error)
    }
);

export default axios
