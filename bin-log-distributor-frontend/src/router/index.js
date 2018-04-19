import Vue from 'vue'
import Router from 'vue-router'
import home from  '@/components/home'
import addClient from '../components/addClient.vue'
import queueMonitoring from '../components/queueMonitoring.vue'
import clientList from '../components/clientList.vue'
import logProgress from '../components/logProgress.vue'

Vue.use(Router)

export default new Router({
  routes: [
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
      ]
    }
  ]
})
