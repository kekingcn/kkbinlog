<template>
  <el-form :model="client" :rules="rules" ref="ruleForm"  style="margin-top: 20px">
    <el-form-item prop="clientId" label="应用ID">
      <el-input v-model="client.clientId" class="auto"></el-input>
    </el-form-item>
    <el-form-item prop="databaseName" label="数据库">
      <el-input v-model="client.databaseName" class="auto"></el-input>
    </el-form-item>
    <el-form-item prop="tableName" label="表名">
      <el-input v-model="client.tableName" class="auto" style="margin-left: -985px"></el-input>
    </el-form-item>
    <el-form-item prop="databaseEvent" label="表操作">
      <el-select v-model="client.databaseEvent" class="auto" style="margin-left: -1000px;width: 210px">
        <el-option  v-for="item in events"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value">
        </el-option>
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="addclient('ruleForm')" class="auto">提交</el-button>
    </el-form-item>
  </el-form>
</template>
<script>
  import {addClient} from '../api/api'
  export default {
    data() {
      return {
        client: {
          clientId: '',
          databaseName: '',
          tableName: '',
          databaseEvent: ''
        },
        events:[
          {
            value: 'WRITE_ROWS',
            label: '增加操作'
          },
          {
            value: 'UPDATE_ROWS',
            label: '更新操作'
          },
          {
            value: 'DELETE_ROWS',
            label: '删除操作'
          },
        ],
        rules: {
          clientId: [{required: true, message: '请输入应用id', trigger: 'blur'}],
          databaseName: [{required: true, message: '请输入数据库名', trigger: 'blur'}],
          tableName: [{required: true, message: '请输入表名', trigger: 'blur'}],
          databaseEvent: [{required: true, message: '请输入时间', trigger: 'blur'}]
        }
      }
    },
    methods: {
      addclient(ruleForm) {
        this.$refs[ruleForm].validate((valid) => {
          if (valid) {
            addClient(this.client).then((res)=> {
              if (res.data.code == 'success') {
                this.$message({
                  type: 'success',
                  message: '添加成功'
                })
              }
              else {
                this.$message.error("添加失败：", res.data.msg)
              }
            })
          }
        })
      }
    },
    mounted() {

    }
  }
</script>
<style>
  .auto {
    width: auto;
    margin-left: -1000px;
  }
</style>
