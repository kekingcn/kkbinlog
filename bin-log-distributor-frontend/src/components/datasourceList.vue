<template>
  <div>
    <el-button
      style="float:right; margin-right: 50px;"
      @click="persistVisible = true">添加数据源</el-button>
    <el-table :data="datasourceList">
      <el-table-column prop="namespace" label="命名空间" align="center"></el-table-column>
      <el-table-column label="状态" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.active" type="success">运行中</el-tag>
          <el-tag v-if="!scope.row.active" type="danger">已关闭</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="host" label="数据库host" align="center"></el-table-column>
      <el-table-column prop="port" label="数据库port" align="center"></el-table-column>
      <el-table-column prop="username" label="用户名" align="center"></el-table-column>
      <el-table-column prop="serverId" label="serverId" align="center"></el-table-column>
      <el-table-column prop="dataSourceUrl" label="url" align="center"></el-table-column>
      <el-table-column label="操作" align="center">
        <template slot-scope="scope">
          <el-button
            v-if="!scope.row.active"
            @click="handleStartDatasource(scope.row.namespace)">
            开启
          </el-button>
          <el-button
            v-if="scope.row.active"
            @click="handleStopDatasource(scope.row.namespace)">关闭</el-button>
          <el-button
            v-if="!scope.row.active && scope.row.deletable"
            @click="handleRemoveDatasource(scope.row.namespace)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog width="40%" title="添加数据源" :visible.sync="persistVisible">
      <el-form :rules="rules" ref="ruleForm" class="persist-datasource-form" label-width="150px" :model="persistDatasource">
        <el-form-item label="命名空间" prop="namespace">
          <el-input class="form-input" v-model="persistDatasource.namespace" ></el-input>
        </el-form-item>
        <el-form-item label="数据库host" prop="host">
          <el-input class="form-input" v-model="persistDatasource.host" ></el-input>
        </el-form-item>
        <el-form-item label="数据库port" prop="port">
          <el-input class="form-input" v-model="persistDatasource.port" ></el-input>
        </el-form-item>
        <el-form-item label="数据库username" prop="username">
          <el-input class="form-input" v-model="persistDatasource.username" ></el-input>
        </el-form-item>
        <el-form-item label="数据库password" prop="password">
          <el-input class="form-input" v-model="persistDatasource.password" ></el-input>
        </el-form-item>
        <el-form-item label="serverID" prop="serverId">
          <el-input class="form-input" v-model="persistDatasource.serverId" ></el-input>
        </el-form-item>
        <el-form-item label="连接URL" prop="dataSourceUrl">
          <el-input class="form-input" type="textarea" :autosize="{ minRows: 3, maxRows: 4}"  v-model="persistDatasource.dataSourceUrl" ></el-input>
        </el-form-item>
        <el-form-item label="是否可移除" prop="deletable">
          <el-radio  v-model="persistDatasource.deletable" label="true">是</el-radio>
          <el-radio v-model="persistDatasource.deletable" label="false">否</el-radio>
        </el-form-item>
        <el-form-item label="redis日志状态key" prop="binLogStatusKey">
          <el-input class="form-input" v-model="persistDatasource.binLogStatusKey" ></el-input>
        </el-form-item>
        <el-form-item label="redis客户端状态key" prop="binLogClientSet">
          <el-input class="form-input" v-model="persistDatasource.binLogClientSet" ></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="persistVisible = false">取 消</el-button>
        <el-button type="primary" @click="handlePersistDatasource">确 定</el-button>
      </div>
    </el-dialog>

  </div>
</template>
<script>
  import {getDatasourceList, stopDatasource, startDatasource, persistDatasource, removeDatasource} from '../api/api'
  import ElButton from "../../node_modules/element-ui/packages/button/src/button.vue";
  export  default {
    components: {ElButton},
    data(){
      return{
        datasourceList:[],
        persistVisible: false,
        defaultPersistDatasource: {
          namespace: '',
          host: '',
          port: '',
          username: '',
          password: '',
          serverId: '',
          dataSourceUrl: '',
          deletable: true,
          binLogStatusKey: 'binLogStatus',
          binLogClientSet: 'binLogClientSet'
        },
        persistDatasource: {
          namespace: '',
          host: '',
          port: '',
          username: '',
          password: '',
          serverId: '',
          dataSourceUrl: '',
          deletable: 'true',
          binLogStatusKey: 'binLogStatus',
          binLogClientSet: 'binLogClientSet'
        },
        rules: {
          namespace: [
            { required: true, message: '请输入', trigger: 'blur' },
          ],
          host: [
            { required: true, message: '请输入', trigger: 'blur' },
          ],
          port: [
            { required: true, message: '请输入', trigger: 'blur' },
          ],
          username: [
            { required: true, message: '请输入', trigger: 'blur' },
          ],
          password: [
            { required: true, message: '请输入', trigger: 'blur' },
          ],
          serverId: [
            { required: true, message: '请输入', trigger: 'blur' },
          ],
          deletable: [
            { required: true, message: '请输入', trigger: 'blur' },
          ],
          dataSourceUrl: [
            { required: true, message: '请输入', trigger: 'blur' },
          ],
          binLogStatusKey: [
            { required: true, message: '请输入', trigger: 'blur' },
          ],
          binLogClientSet: [
            { required: true, message: '请输入', trigger: 'blur' },
          ]
        }
      }
    },
    methods:{
      list(){
        getDatasourceList('').then((res)=>{
          this.datasourceList=res.data;
        })
      },
      handleStartDatasource(namespace){
        startDatasource({
          namespace: namespace
        }).then(res=>{
          if(res.data.code=="success"){
            this.$message({
              type: 'success',
              message: res.data.msg
            })
            this.list()
          }else {
            this.$message.error(res.data.msg)
          }
        })
      },
      handleStopDatasource(namespace){
        stopDatasource({
          namespace: namespace
        }).then(res=>{
          if(res.data.code=="success"){
            this.$message({
              type: 'success',
              message: '关闭数据源监听成功'
            })
            this.list()
          }else {
            this.$message.error("关闭数据源监听失败")
          }
        })
      },
      handlePersistDatasource() {

        this.$refs['ruleForm'].validate((valid) => {

          if(!valid) {
            return
          }

          this.persistVisible = false

          persistDatasource(this.persistDatasource).then(res=>{
            if(res.data.code=="success"){
              this.$message({
                type: 'success',
                message: '添加数据源成功'
              })
              this.list()
              this.persistDatasource = this.defaultPersistDatasource
            }else {
              this.$message.error("添加数据源失败")
            }
          })
        })
      },
      handleRemoveDatasource(namespace){
        removeDatasource({
          namespace: namespace
        }).then(res=>{
          if(res.data.code=="success"){
            this.$message({
              type: 'success',
              message: '移除数据源成功'
            })
            this.list()
          }else {
            this.$message.error("数据源监听失败")
          }
        })
      },
    },
    mounted(){
     this.list()
    }
  }
</script>
<style type="scss" scoped>
  .form-input {
    width: 400px;
  }

</style>
