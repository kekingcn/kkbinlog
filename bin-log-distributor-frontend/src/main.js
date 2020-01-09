// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import ElementUI from 'element-ui'
import App from './App'
import router from './router'
import 'element-ui/lib/theme-chalk/index.css'
import HttpInterceptors from './HttpInterceptors'
import VueCookies from 'vue-cookies'

Vue.config.productionTip = false
Vue.use(ElementUI);
Vue.use(VueCookies);
VueCookies.config("17h");

router.beforeEach((to, from, next) => {

    if(to.path === '/clientList') {
      next();
      return;
    }

    let username = to.query.username;
    let token = to.query.token;
    if(username && token){
      sessionStorage.setItem('user', '{"username":"'+username+'","token":"'+token+'"}');
      next()
    }
    if (to.path == '/login') {
      sessionStorage.removeItem('user');
    }
    let user =  sessionStorage.getItem('user');
    if (!user && to.path != '/login') {
      next({path: '/login'})
    } else {
      next()
    }
  }
)
new Vue({
  el: '#app',
  router,
  HttpInterceptors,
  components: {App},
  template: '<App/>'
});
