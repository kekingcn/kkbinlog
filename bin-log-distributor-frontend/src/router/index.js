import Vue from 'vue'
import  login from '../components/login.vue'
import Router from 'vue-router'
import home from  '@/components/home'
import addClient from '../components/addClient.vue'
import queueMonitoring from '../components/queueMonitoring.vue'
import clientList from '../components/clientList.vue'
import datasourceList from '../components/datasourceList.vue'
import logProgress from '../components/logProgress.vue'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path:'/login',
      component:login,
      name: "",
      hidden: true
    },
    {
      path: '/',
      name: '主页',
      component: home,
      children:[
        {
          path:'/logProgress',
          name:'日志进度',
          component:logProgress
        },
        {
          path:'/addClient',
          name:'新增应用',
          component:addClient
        },
        {
          path:'/queueMonitoring',
          name:'队列监控',
          component:queueMonitoring
        },
        {
          path:'/clientList',
          name:'应用列表',
          component:clientList
        },
        {
          path:'/datasourceList',
          name:'数据源列表',
          component:datasourceList
        },
      ]
    }
  ]
})
