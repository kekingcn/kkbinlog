<template>
<div>
  <p>当前日志文件:{{fileName}}</p>
  <p>当前日志位置:{{position}}</p>

</div>
</template>

<script>

  import {getLogStatus} from '../api/api'
  export default {
    data(){
      return{
        fileName : '',
        position : '',
      }
    },
    methods:{

    },
    mounted(){
      getLogStatus('').then(res=>{
        this.fileName=res.data.binlogFilename
        this.position=res.data.binlogPosition
      })

    }
  }

</script>
<style>

</style>
