<template>
  <el-tabs type="card" v-model="activeName" @tab-click="handleClick(activeName)">
    <el-tab-pane name="redis">
      <span slot="label"> <i class="el-icon-info" style="color:green"></i>Redis队列</span>
      <el-collapse accordion>
        <div v-for="item in redisClientIds" @click.once="getClientQueue(item,'redis',1)">
          <el-collapse-item :title="item">
            <el-table :data="redisQueueArray[item]">
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
              :total="redisQueueSize">
            </el-pagination>
          </el-collapse-item>
        </div>
      </el-collapse>
    </el-tab-pane>
    <el-tab-pane name="rabbit">
      <span slot="label"> <i class="el-icon-info" style="color:green"></i>Rabbit队列</span>
      <el-collapse accordion>
        <div v-for="item in rabbitClientIds" @click.once="getClientQueue(item,'rabbit',1)">
          <el-collapse-item :title="item">
            <el-table :data="rabbitQueueArray[item]">
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
              :total="rabbitQueueSize">
            </el-pagination>
          </el-collapse-item>
        </div>
      </el-collapse>
    </el-tab-pane>
    <el-tab-pane name="kafka">
      <span slot="label"> <i class="el-icon-info" style="color:green"></i>Kafka队列</span>
      <el-collapse accordion>
        <div v-for="item in kafkaClientIds" @click.once="getClientQueue(item,'kafka',1)">
          <el-collapse-item :title="item">
            <el-table :data="kafkaQueueArray[item]">
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
              :total="kafkaQueueSize">
            </el-pagination>
          </el-collapse-item>
        </div>
      </el-collapse>
    </el-tab-pane>
    <el-tab-pane name="error">
      <span slot="label"> <i class="el-icon-error" style="color: red"></i>异常队列</span>
      <el-collapse>
        <div v-for="item in errClient" :key="item" @click.once="getClientQueue(item,'error',1)">
          <el-collapse-item :title="item">
            <el-table :data="abnormalQueue"  ref="multipleTable">
              <el-table-column type="index" width="50"></el-table-column>
              <el-table-column prop="table" label="表" align="center"></el-table-column>
              <el-table-column prop="projNo" label="项目编号" align="center"></el-table-column>
              <el-table-column prop="cycle" label="期次" align="center"></el-table-column>
              <el-table-column prop="repayType" label="偿还方式" align="center"></el-table-column>
              <el-table-column prop="eventType" label="事件" align="center"></el-table-column>
              <el-table-column label="操作" align="center">
                <template slot-scope="scope">
                  <el-button @click="deleteFromQueue(scope.row.uuid,item)">删除</el-button>
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
  import {getqueuesize, getRedisClientList, getRabbitClientList, getKafkaClientList, getErrorClientList,deleteFromQueue} from '../api/api'
  import Vue from 'vue'

  export default {
    data() {
      return {
        queueName: '',
        redisQueueSize: 0,
        redisQueueArray: [],
        rabbitQueueSize: 0,
        rabbitQueueArray: [],
        kafkaQueueSize: 0,
        kafkaQueueArray: [],
        activeName: 'redis',
        redisClientIds: [],
        rabbitClientIds: [],
        kafkaClientIds:[],
        clientName: '',
        errClient: [],
        queueErrSize:'',
        abnormalQueue:[],//异常队列
        multipleSelection:[],
        sysUserName:'',
      }
    },
    methods: {
      handleClick(activeName) {
        if (activeName === "error") {
          getErrorClientList('').then((res) => {
            this.errClient = res.data
          })
        }
      },
      //获取应用对应的队列信息
      getClientQueue(clientName, type, page) {
        this.clientName = clientName;
        getqueuesize({clientName: clientName, type: type, page: page}).then((res => {
          if(type==='redis'){//正常队列
            this.redisQueueSize = res.data.queueSize;
            let temp = [];
            res.data.queue.forEach(item => {
              if (item !== null) {
                let object = new Object({
                  queue: [],
                });
                object.queue.push(JSON.stringify(item));
                temp.push(object)
              }
            });
            Vue.set(this.redisQueueArray, clientName, temp)
          } else if (type==='rabbit') {
            this.rabbitQueueSize = res.data.queueSize;
            let temp=[];
            res.data.queue.forEach(item => {
              if (item !== null) {
                let object = new Object({
                  queue: [],
                });
                object.queue.push(JSON.stringify(item));
                temp.push(object)
              }
            });
            Vue.set(this.rabbitQueueArray, clientName, temp)
          }else if (type==='kafka') {
            this.kafkaQueueSize = res.data.queueSize;
            let temp=[];
            res.data.queue.forEach(item => {
              if (item !== null) {
                let object = new Object({
                  queue: [],
                });
                object.queue.push(JSON.stringify(item));
                temp.push(object)
              }
            });
            Vue.set(this.kafkaQueueArray, clientName, temp)
          } else {//异常队列返回值
            const {queueSize,queue} = res.data;
            this.handleErrQueue(queue)
          }
        }))
      },
      handleCurrentChange(val) {
        this.getClientQueue(this.clientName, this.activeName, val)
      },
      //从对列中删除
      deleteFromQueue(uuid,item){
        deleteFromQueue({uuid:uuid,errClient:item}).then(res=>{
          if(res.data===true){
            this.$message({
              type: 'success',
              message: '操作成功!'
            });
            this.getClientQueue(item,'error',1)
          }else {
            this.$message.error("操作失败!");
            this.getClientQueue(item,'error',1)
          }
        })
      },
      handleErrQueue(queue){
        let error = [];
        queue.forEach(item => {
          const {dataKey, exception, eventBaseDTO} = item;
          if('rows' in eventBaseDTO) {
            const {database, eventType, rows, table, uuid} = eventBaseDTO;
            rows.forEach(row => {
              if ('afterRowMap' in row) {
                const {beforeRowMap, afterRowMap} = row;
                let object = {};
                object.eventType = eventType;
                object.table = table === "ams_proj_plan" ? "plan(偿还表)" : table === "ams_proj_rcvl" ? "rcvl(应收表)" : table;
                object.uuid = uuid;
                object.projNo = afterRowMap.proj_no;
                object.cycle = afterRowMap.cycle_num;
                object.repayType = afterRowMap.repay_type;
                error.push(object);
              }
            });
          }else if('rowMaps' in eventBaseDTO){
            const {database, eventType, rowMaps, table, uuid} = eventBaseDTO;
            rowMaps.forEach(row => {
              let object = {};
              object.eventType = eventType;
              object.table = table === "ams_proj_plan" ? "plan(偿还表)" : table === "ams_proj_rcvl" ? "rcvl(应收表)" : table;
              object.uuid = uuid;
              object.projNo = row.proj_no;
              object.cycle = row.cycle_num;
              object.repayType = row.repay_type;
              error.push(object);
            });
          }
        });
        this.abnormalQueue = error;
      }
    },
    mounted() {
      getRedisClientList('').then((res) => {
        const set = new Set();
        res.data.forEach(item=>{
        set.add(item.key)
        });
        set.forEach(item=>{
          this.redisClientIds.push(item)
        })
      });
      getRabbitClientList('').then((res) => {
        const set = new Set();
        res.data.forEach(item => {
          set.add(item.key)
        });
        set.forEach(item => {
          this.rabbitClientIds.push(item)
        })
      });
      getKafkaClientList('').then((res) => {
        const set = new Set();
        res.data.forEach(item => {
          set.add(item.key)
        });
        set.forEach(item => {
          this.kafkaClientIds.push(item)
        })
      });
      let user = sessionStorage.getItem('user');
      if (user) {
        user = JSON.parse(user);
        this.sysUserName = user.username || '';
      }
    }
  }
</script>
<style>

</style>
