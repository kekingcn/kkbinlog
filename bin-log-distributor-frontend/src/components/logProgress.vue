<template>
<div>
  <el-table :data="logStatus">
    <el-table-column prop="namespace" label="命名空间" align="center"></el-table-column>
    <el-table-column prop="binlogFilename" label="当前日志文件" align="center"></el-table-column>
    <el-table-column prop="binlogPosition" label="当前日志位置" align="center"></el-table-column>
    <el-table-column prop="datetime" label="更新时间" align="center"></el-table-column>
    <el-table-column prop="timestamp" label="更新时间戳" align="center"></el-table-column>
  </el-table>
</div>
</template>

<script>

  import {getLogStatus} from '../api/api'
  export default {
    data(){
      return{
        logStatus: []
      }
    },
    methods:{

    },
    mounted(){
      getLogStatus('').then(res=>{
        this.logStatus = res.data
      })
    }
  }

</script>
<style>

</style>
