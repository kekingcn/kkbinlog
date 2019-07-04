<template>
<el-table :data="clients">
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
</template>
<script>
  import {getClientList,deleteClient} from '../api/api'
  import ElButton from "../../node_modules/element-ui/packages/button/src/button.vue";
  export  default {
    components: {ElButton},
    data(){
      return{
      clients:[],
      }
    },
    methods:{
      list(){
        getClientList('').then((res)=>{
          this.clients=res.data;
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
      }
    },
    mounted(){
     this.list()
    }
  }
</script>
<style>

</style>
