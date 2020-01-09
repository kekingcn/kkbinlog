<template>
  <div>
    <el-button
      style="float:right; margin-right: 70px;"
      @click="persistVisible = true">添加数据源</el-button>
    <div style="float:right; margin-right: 50px; ">
      <span>刷新间隔</span>
      <el-select
        style="width: 80px; margin-left: 20px;"
        v-model="refreshInterval"
        placeholder="请选择">
        <el-option
          v-for="item in refreshOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value">
        </el-option>
      </el-select>
    </div>

    <el-table :data="datasourceList">
      <el-table-column prop="namespace" label="命名空间" align="center"/>
      <el-table-column label="状态" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.active" type="success">运行中</el-tag>
          <el-tag v-if="!scope.row.active" type="danger">已关闭</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="dataSourceType" label="数据源类型" align="center"/>
      <el-table-column prop="host" label="数据库host" align="center"/>
      <el-table-column prop="port" label="数据库port" align="center"/>
      <el-table-column prop="username" label="用户名" align="center"/>
      <el-table-column prop="serverId" label="serverId" align="center"/>
      <el-table-column prop="dataSourceUrl" label="url" align="center"/>
      <el-table-column prop="version" label="版本号" align="center"/>
      <el-table-column label="操作" align="center">
        <template slot-scope="scope">
          <el-button
            v-if="!scope.row.active"
            @click="clickStart(scope.row.namespace)">
            开启
          </el-button>
          <el-button
            v-if="scope.row.active"
            @click="handleStopDatasource(scope.row.namespace)">关闭
          </el-button>
          <el-button
            v-if="!scope.row.active && scope.row.deletable"
            @click="handleRemoveDatasource(scope.row.namespace)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="padding: 40px 0;">
    </div>

    <el-table :data="serviceStatusList">
      <el-table-column prop="ip" label="IP" align="center"/>
      <el-table-column prop="activeNamespaces" label="有效的命名空间" align="center"/>
      <el-table-column prop="totalEventCount" label="Event总数量" align="center"/>
      <el-table-column prop="latelyEventCount" label="Event间隔数量" align="center"/>
      <el-table-column prop="totalPublishCount" label="Publish总数量" align="center"/>
      <el-table-column prop="latelyPublishCount" label="Publish间隔数量" align="center"/>
      <el-table-column prop="updateTime" label="更新时间" align="center"/>
    </el-table>

    <el-dialog width="40%" title="添加数据源" :visible.sync="persistVisible">
      <el-form :rules="rules" ref="ruleForm" class="persist-datasource-form" label-width="150px"
               :model="persistDatasource">
        <el-form-item label="数据源类型" prop="dataSourceType">
          <el-select style="width: 70%" v-model="persistDatasource.dataSourceType" placeholder="请选择">
            <el-option v-for="item in dataSourceTyepOptions"
                       :key="item.value"
                       :label="item.label"
                       :value="item.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="命名空间" prop="namespace">
          <el-input class="form-input" v-model="persistDatasource.namespace"/>
        </el-form-item>
        <el-form-item label="数据库host" prop="host" v-if="persistDatasource.dataSourceType === 'MySQL'">
          <el-input class="form-input" v-model="persistDatasource.host"/>
        </el-form-item>
        <el-form-item label="数据库port" prop="port" v-if="persistDatasource.dataSourceType === 'MySQL'">
          <el-input class="form-input" v-model="persistDatasource.port"/>
        </el-form-item>
        <el-form-item label="数据库username" prop="username" v-if="persistDatasource.dataSourceType === 'MySQL'">
          <el-input class="form-input" v-model="persistDatasource.username"/>
        </el-form-item>
        <el-form-item label="数据库password" prop="password" v-if="persistDatasource.dataSourceType === 'MySQL'">
          <el-input class="form-input" v-model="persistDatasource.password"/>
        </el-form-item>
        <el-form-item label="serverID" prop="serverId" v-if="persistDatasource.dataSourceType === 'MySQL'">
          <el-input class="form-input" v-model="persistDatasource.serverId"/>
        </el-form-item>
        <el-form-item label="连接URL" prop="dataSourceUrl">
          <el-input class="form-input" type="textarea" :autosize="{ minRows: 3, maxRows: 4}"
                    v-model="persistDatasource.dataSourceUrl"/>
        </el-form-item>
        <el-form-item label="是否可移除" prop="deletable">
          <el-radio v-model="persistDatasource.deletable" label="true">是</el-radio>
          <el-radio v-model="persistDatasource.deletable" label="false">否</el-radio>
        </el-form-item>
        <el-form-item label="redis日志状态key" prop="binLogStatusKey">
          <el-input class="form-input" v-model="persistDatasource.binLogStatusKey"/>
        </el-form-item>
        <el-form-item label="redis客户端状态key" prop="binLogClientSet">
          <el-input class="form-input" v-model="persistDatasource.binLogClientSet"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="persistVisible = false">取 消</el-button>
        <el-button type="primary" @click="handlePersistDatasource">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog width="30%" title="开启数据源" :visible.sync="startVisible">
      <el-form ref="ruleForm" class="persist-datasource-form" label-width="150px" :model="persistDatasource">
        <el-form-item label="指定IP" prop="namespace">
          <el-select
            style=";"
            v-model="startDataSourceParams.delegatedIp"
            placeholder="请选择">
            <el-option
              v-for="item in serviceStatusList"
              :key="item.ip"
              :label="item.ip"
              :value="item.ip">
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="startVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleStartDatasource">确 定</el-button>
      </div>
    </el-dialog>

  </div>
</template>
<script>
  import {getDatasourceList, stopDatasource, startDatasource, persistDatasource, removeDatasource, getServiceStatus} from '../api/api'
  import ElButton from "../../node_modules/element-ui/packages/button/src/button.vue";
  export  default {
    components: {ElButton},
    data(){
      return{
        datasourceList:[],
        serviceStatusList: [],
        persistVisible: false,
        startVisible: false,
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
        dataSourceTyepOptions: [{
          value: 'MySQL',
          label: 'MySQL'
        }, {
          value: 'MongoDB',
          label: 'MongoDB'
        }],
        persistDatasource: {
          dataSourceType: 'MySQL',
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
        startDataSourceParams: {
          namespace: '',
          delegatedIp: '',
        },
        refreshOptions: [
          {value: 1, label: '1s'},
          {value: 3, label: '3s'},
          {value: 5, label: '5s'},
          {value: 10, label: '10s'},
          {value: 20, label: '20s'},
          {value: 30, label: '30s'}
        ],
        rules: {
          dataSourceType: [
            {required: true, message: '请输入', trigger: 'blur'},
          ],
          namespace: [
            {required: true, message: '请输入', trigger: 'blur'},
          ],
          host: [
            {required: true, message: '请输入', trigger: 'blur'},
          ],
          port: [
            {required: true, message: '请输入', trigger: 'blur'},
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
        },
        refreshInterval: 5,
        refreshTimer: 0,
      }
    },
    watch: {
      refreshInterval: function() {
        this.refreshConfig()
      }
    },
    methods:{
      list(){
        getDatasourceList('').then((res) => {
          this.datasourceList = res.data;
        });
        getServiceStatus().then(res => {
          this.serviceStatusList = res.data
        })
      },
      clickStart(namespace) {
        this.startVisible = true;
        this.startDataSourceParams.namespace = namespace;
      },
      handleStartDatasource(){
        this.startVisible = false;
        startDatasource(this.startDataSourceParams).then(res=>{
          if (res.data.code === "success") {
            this.$message({
              type: 'success',
              message: res.data.msg
            })
          } else {
            this.$message.error(res.data.msg)
          }
        })
      },
      handleStopDatasource(namespace){
        stopDatasource({
          namespace: namespace
        }).then(res=>{
          if (res.data.code === "success") {
            this.$message({
              type: 'success',
              message: '关闭数据源监听成功'
            })
          } else {
            this.$message.error("关闭数据源监听失败")
          }
        })
      },
      handlePersistDatasource() {

        this.$refs['ruleForm'].validate((valid) => {

          if(!valid) {
            return
          }

          this.persistVisible = false;

          persistDatasource(this.persistDatasource).then(res=>{
            if(res.data.code==="success"){
              this.$message({
                type: 'success',
                message: '添加数据源成功'
              });
              this.list();
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
          if(res.data.code==="success"){
            this.$message({
              type: 'success',
              message: '移除数据源成功'
            });
            this.list()
          }else {
            this.$message.error("数据源监听失败")
          }
        })
      },
      refresh() {
        clearInterval(this.refreshTimer);
        let refreshIntervalMs = this.refreshInterval*1000;
        this.refreshTimer = setInterval(() => {
          this.list();
        }, (refreshIntervalMs))
      }
    },
    mounted(){
      this.list();
      this.refresh()
    },
    destroyed() {
      clearInterval(this.refreshTimer)
    }
  }
</script>
<style type="scss" scoped>
  .form-input {
    width: 400px;
  }

</style>
