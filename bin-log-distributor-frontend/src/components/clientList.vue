<template>
  <el-collapse v-model="activeClientGroup">
    <el-collapse-item v-for="(clientList, groupKey) in clientMap" :key="groupKey" :name="groupKey">
      <template slot="title">
            <div style="text-align: left; font-size: 18px;">
                {{groupKey}}
            </div>
      </template>
      <div style="text-align: right; padding-right: 50px;">
        <el-button type="danger" @click="handleDeleteTopic(groupKey)">删除通道topic</el-button>
      </div>
      <el-table
        :data="clientList">
        <el-table-column prop="clientId" label="应用" align="center"></el-table-column>
        <el-table-column prop="namespace" label="命名空间" align="center"></el-table-column>
        <el-table-column prop="databaseName" label="数据库" align="center"></el-table-column>
        <el-table-column prop="tableName" label="表" align="center"></el-table-column>
        <el-table-column prop="databaseEvent" label="动作" align="center"></el-table-column>
        <el-table-column prop="queueType" label="队列类型" align="center"></el-table-column>
        <el-table-column label="操作" align="center">
          <template slot-scope="scope">
            <el-button @click="deleteClient(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

    </el-collapse-item>
  </el-collapse>
</template>
<script>
  import {deleteClient, getClientMap, deleteTopic} from '../api/api'
  import ElButton from "../../node_modules/element-ui/packages/button/src/button.vue";
  export  default {
    components: {ElButton},
    data(){
      return{
        clientMap: {},
        activeClientGroup: [],
      }
    },
    methods:{
      listClientMap() {
        getClientMap('').then((res)=>{
          this.clientMap=res.data;
          this.activeClientGroup = Object.keys(res.data)
        })
      },
      deleteClient(client){
        deleteClient(client).then(data=>{
          if(data.code=="success"){
            this.$message({
              type: 'success',
              message: '删除成功'
            })
            this.list()
          }else {
            this.$message.error("删除失败：", data.msg)
          }
        })
      },
      handleDeleteTopic(clientInfoKey) {
        this.$confirm('确认删除</br><strong>' + clientInfoKey +'</strong></br>对应通道的topic？', '删除确认', {dangerouslyUseHTMLString: true}).then(_ => {
          deleteTopic({clientInfoKey: clientInfoKey}).then(res => {
            console.log(res)
            if('success' == res.data.code) {
                this.$message({
                  message: res.data.msg,
                  type: 'success'
              })
            } else {
              this.$message({
                message: res.data.msg,
                type: 'error'
              })
            }
            this.listClientMap()
          })
        })
      }
    },
    mounted(){

      // sso
      let token = this.$cookies.get("keking_token");
      if(token) {
        sessionStorage.setItem('user', '{"username":"' + token.username + '","token":"' + token.access_token + '"}');
      }

      this.listClientMap()
    }
  }
</script>
<style>

</style>
