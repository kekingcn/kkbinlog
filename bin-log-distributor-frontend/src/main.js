// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import ElementUI from 'element-ui'
import App from './App'
import router from './router'
import 'element-ui/lib/theme-chalk/index.css'
import HttpInterceptors from './HttpInterceptors'

Vue.config.productionTip = false
Vue.use(ElementUI)

new Vue({
  el: '#app',
  router,
  HttpInterceptors,
  components: {App},
  template: '<App/>'
});
