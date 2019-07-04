<template>
  <el-form :model="client" :rules="rules" ref="ruleForm" label-width="80px" style="width: 600px; margin: 40px auto;">
    <el-form-item prop="clientId" label="应用ID">
      <el-input v-model="client.clientId" class="auto"></el-input>
    </el-form-item>
    <el-form-item prop="namespace" label="命名空间">
      <el-select v-model="client.namespace" style="width: 100%" placeholder="请选择">
        <el-option
          v-for="item in namespaceList"
          :key="item"
          :label="item"
          :value="item">
        </el-option>
      </el-select>
    </el-form-item>
    <el-form-item prop="databaseName" label="数据库">
      <el-input v-model="client.databaseName" class="auto"></el-input>
    </el-form-item>
    <el-form-item prop="tableName" label="表名">
      <el-input v-model="client.tableName" class="auto" ></el-input>
    </el-form-item>
    <el-form-item label="表操作" prop="databaseEvent">
      <el-checkbox-group v-model="client.databaseEvent" @change="checkBoxChange">
        <el-checkbox label="WRITE_ROWS">增加操作</el-checkbox>
        <el-checkbox label="UPDATE_ROWS">更新操作</el-checkbox>
        <el-checkbox label="DELETE_ROWS">删除操作</el-checkbox>
      </el-checkbox-group>
    </el-form-item>
    <el-form-item label="队列类型" prop="queueType">
      <el-radio-group v-model="client.queueType">
        <el-radio :label="'redis'">redis</el-radio>
        <el-radio :label="'rabbit'">rabbit</el-radio>
        <el-radio :label="'kafka'">kafka</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="addclient('ruleForm')" class="auto">提交</el-button>
    </el-form-item>
  </el-form>
</template>
<script>
  import {addClient, getNamespaceList} from '../api/api'
  export default {
    data() {
      return {
        client: {
          clientId: '',
          namespace: '',
          databaseName: '',
          tableName: '',
          databaseEvent: ['WRITE_ROWS', 'UPDATE_ROWS', 'DELETE_ROWS'],
          queueType: 'kafka'
        },
        rules: {
          clientId: [{required: true, message: '请输入应用id', trigger: 'blur'}],
          namespace: [{required: true, message: '请选择命名空间', trigger: 'blur'}],
          databaseName: [{required: true, message: '请输入数据库名', trigger: 'blur'}],
          tableName: [{required: true, message: '请输入表名', trigger: 'blur'}],
          databaseEvent: [{required: true, message: '请勾选事件类型', trigger: 'blur'}],
          queueType: [{required: true, message: '请选择队列类型', trigger: 'blur'}]
        },
        namespaceList: []
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
      },
      checkBoxChange(list) {
        this.client.databaseEvent = list;
      }
    },
    mounted() {
      getNamespaceList().then(res => {
        this.namespaceList = res.data
      })
    }
  }
</script>
<style>
  /* .auto {
    width: auto;
    margin-left: -1000px;
  } */
</style>
