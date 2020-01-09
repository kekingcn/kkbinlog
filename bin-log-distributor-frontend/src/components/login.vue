<template>
  <el-form :model="ruleForm2" :rules="rules2" ref="ruleForm2" label-position="left" label-width="0px"
           class="demo-ruleForm login-container">
    <h3 class="title">binLogClient-ams</h3>
    <el-form-item prop="account">
      <el-input type="text" v-model="ruleForm2.account" auto-complete="off" placeholder="输入任意字符登录"
                @blur="" :autofocus="focusStatus"></el-input>
    </el-form-item>
    <el-form-item prop="checkPass">
      <el-input type="password" v-model="ruleForm2.checkPass" auto-complete="off" placeholder="密码"></el-input>
    </el-form-item>
    <el-form-item style="width:100%;">
      <el-button type="primary" style="width:100%;" @click.native.prevent="handleSubmit2" :loading="logining">登录
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script>
  import {requestLogin} from '../api/api';
  import cookies from 'vue-cookies';
  export default {
    data() {
      return {
        userName: '',
        logining: false,
        ruleForm2: {
          account: '',
          checkPass: ''
        },
        rules2: {
          account: [
            {required: true, message: '请输入账号', trigger: 'blur'},
          ],
          checkPass: [
            {required: true, message: '请输入密码', trigger: 'blur'},
          ]
        },
        checked: true,
        focusStatus: true,
      };
    },
    methods: {
      handleSubmit2() {
          this.userName=this.ruleForm2.account;
          this.submit();
      },
      submit(){
        this.$refs.ruleForm2.validate((valid) => {
          if (valid) {
            cookies.set("keking_token", "需要对接自己的认证系统");
            sessionStorage.setItem('user', '{"username":"' + this.userName + '","token":"a"}');
            this.$router.push({path: '/datasourceList'});
            this.$message({
              message: '登录成功',
              type: 'success'
            })
          } else {
            return false;
          }
        });
      },
    }
  }
</script>

<style scoped>
  .login-container {
    /*box-shadow: 0 0px 8px 0 rgba(0, 0, 0, 0.06), 0 1px 0px 0 rgba(0, 0, 0, 0.02);*/
    -webkit-border-radius: 5px;
    border-radius: 5px;
    -moz-border-radius: 5px;
    background-clip: padding-box;
    margin: 180px auto;
    width: 350px;
    padding: 35px 35px 15px 35px;
    background: #fff;
    border: 1px solid #eaeaea;
    box-shadow: 0 0 25px #cac6c6;
  }
  .title {
    margin: 0px auto 40px auto;
    text-align: center;
    color: #505458;
  }
  .remember {
    margin: 0px 0px 35px 0px;
  }

</style>
