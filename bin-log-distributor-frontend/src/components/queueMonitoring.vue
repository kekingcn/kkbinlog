<template>
  <el-tabs type="card" v-model="activeName" @tab-click="handleClick(activeName)">
    <el-tab-pane name="info">
      <span slot="label"> <i class="el-icon-info" style="color:green"></i>正常队列</span>
      <el-collapse accordion>
        <div v-for="item in clientIds" @click.once="getClientQueue(item,'info',1)">
          <el-collapse-item :title="item">
            <el-table :data="queueArray[item]">
              <el-table-column
                type="index"
                width="50">
              </el-table-column>
              <el-table-column prop="queue" label="详细" align="center"></el-table-column>
            </el-table>
            <el-pagination
              @current-change="handleCurrentChange"
              :page-size="10"
              layout="total,  prev, pager, next, jumper"
              :total="queueSize">
            </el-pagination>
          </el-collapse-item>
        </div>
      </el-collapse>
    </el-tab-pane>
    <el-tab-pane name="error">
      <span slot="label"> <i class="el-icon-error" style="color: red"></i>异常队列</span>
      <el-collapse>
        <div v-for="item in errClient" @click.once="getClientQueue(item,'error',1)">
          <el-collapse-item :title="item">
            <el-table :data="abnormalQueue"  ref="multipleTable">
              <el-table-column prop="exception" label="异常大概" align="center"></el-table-column>
              <el-table-column prop="table" label="表" align="center"></el-table-column>
              <el-table-column prop="dataBase" label="库" align="center"></el-table-column>
              <el-table-column prop="eventType" label="事件" align="center"></el-table-column>
              <el-table-column prop="uuid" label="UUID" align="center"></el-table-column>
              <el-table-column label="操作" align="center">
                <template slot-scope="scope">
                  <el-tooltip class="item" effect="dark" content="此异常的锁级别为NONE，因此你可以删除它" placement="top">
                    <el-button @click="enqueueAgainOrDelete(scope.row.uuid,scope.row.dataKey,'delete',item)" v-show="scope.row.dataKey.indexOf('TABLE')==-1">删除</el-button>
                  </el-tooltip>
                  <el-button @click="enqueueAgainOrDelete(scope.row.uuid,scope.row.dataKey,'enqueue',item)">重新入队</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </div>
      </el-collapse>
    </el-tab-pane>
  </el-tabs>
</template>
<script>
  import {getqueuesize, getClientList, getErrorClientList,enqueueAgainOrDelete} from '../api/api'
  import Vue from 'vue'

  export default {
    data() {
      return {
        queueName: '',
        queueSize: '',
        queueArray: [],
        activeName: 'info',
        clientIds: [],
        clientName: '',
        errClient: [],
        queueErrSize:'',
        abnormalQueue:[],//异常队列
        multipleSelection:[],
      }
    },
    methods: {
      handleClick(activeName) {
        if (activeName == "error") {
          getErrorClientList('').then((res) => {
            this.errClient = res.data
          })
        }
      },
      //获取应用对应的队列信息
      getClientQueue(clientName, type, page) {
        this.clientName = clientName
        getqueuesize({clientName: clientName, type: type, page: page}).then((res => {
          if(type=='info'){//正常队列
            this.queueSize = res.data.queueSize
            let temp=new Array()
            res.data.queue.forEach(item=>{
              if(item!==null){
                let object=new Object({
                  queue:[],
                })
                object.queue.push(JSON.stringify(item))
                temp.push(object)
              }
            })
            Vue.set(this.queueArray, clientName, temp)
          }else {//异常队列返回值
            this.abnormalQueue=res.data.queue
          }
        }))
      },
      handleCurrentChange(val) {
        this.getClientQueue(this.clientName, this.activeName, val)
      },
      //重新入队或删除
      enqueueAgainOrDelete(uuid,dataKey,type,item){
        enqueueAgainOrDelete({uuid:uuid,dataKey:dataKey,type:type,errClient:item}).then(res=>{
          if(res.data==true){
            this.$message({
              type: 'success',
              message: '操作成功!'
            })
            this.getClientQueue(item,'error',1)
          }else {
            this.$message.error("操作失败!")
            this.getClientQueue(item,'error',1)
          }
        })
      }

    },
    mounted() {
      getClientList('').then((res) => {
        const  set=new Set()
        res.data.forEach(item=>{
        set.add(item.key)
        })
        set.forEach(item=>{
          this.clientIds.push(item)
        })
      })
    }
  }
</script>
<style>

</style>
