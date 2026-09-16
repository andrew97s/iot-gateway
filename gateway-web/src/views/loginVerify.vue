<template>
  <div class="login">
    <el-form class="login-form" :model="loginForm" :rules="loginRules" ref="loginRef">
      <h3 class="title">{{ defaultSettings.loginTitle }}</h3>
      <el-form-item class="form-item-box" prop="username">
        <el-input v-model="loginForm.username" auto-complete="off" placeholder="账号" size="large" type="text">
          <template #prefix><svg-icon class="el-input__icon input-icon" icon-class="user" /></template>
        </el-input>
      </el-form-item>
      <el-form-item class="form-item-box" prop="password">
        <el-input v-model="loginForm.password" auto-complete="off" placeholder="密码" size="large" type="password" @keyup.enter="handleLogin">
          <template #prefix><svg-icon class="el-input__icon input-icon" icon-class="password" /></template>
        </el-input>
      </el-form-item>
      <!-- <el-form-item prop="code" v-if="captchaEnabled">
        <el-input v-model="loginForm.code" size="large" auto-complete="off" placeholder="验证码" style="width: 63%" @keyup.enter="handleLogin">
          <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img" />
        </div>
      </el-form-item> -->
      <Verify
        :captcha-type="'blockPuzzle'"
        :img-size="{ width: '330px', height: '155px' }"
        :mode="'pop'"
        ref="verify"
        @success="capctchaCheckSuccess"
      ></Verify>
      <el-checkbox v-model="loginForm.rememberMe" class="remember-me" style="margin: 0px 0px 25px 0px">记住密码</el-checkbox>
      <el-form-item class="form-item-box login-btn-box">
        <el-button class="login-btn" :loading="loading" style="width: 100%" type="primary" @click.prevent="handleLogin">
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>Copyright © 2023-{{ new Date().getFullYear() }} </span>
    </div>
  </div>
</template>

<script setup>
import defaultSettings from '@/settings'
import Verify from '@/components/system/Verifition/Verify.vue'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
})

const loginRules = {
  username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
  password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }],
  code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
}

const loading = ref(false)

const redirect = ref(undefined)

function handleLogin() {
  proxy.$refs.loginRef.validate((valid) => {
    if (valid) {
      proxy.$refs.verify.show()
    }
  })
}

function capctchaCheckSuccess(params) {
  loginForm.code = params.captchaVerification
  loading.value = true
  if (loginForm.rememberMe) {
    Cookies.set('username', loginForm.username, { expires: 30 })
    Cookies.set('password', encrypt(loginForm.password), { expires: 30 })
    Cookies.set('rememberMe', loginForm.rememberMe, { expires: 30 })
  } else {
    Cookies.remove('username')
    Cookies.remove('password')
    Cookies.remove('rememberMe')
  }
  // 调用action的登录方法
  userStore
    .login(loginForm)
    .then(() => {
      router.push({ path: redirect.value || '/' })
    })
    .catch(() => {
      loading.value = false
    })
}

function getCookie() {
  const username = Cookies.get('username')
  const password = Cookies.get('password')
  const rememberMe = Cookies.get('rememberMe')
  loginForm.username = username === undefined ? loginForm.username : username
  loginForm.password = password === undefined ? loginForm.password : decrypt(password)
  loginForm.rememberMe = rememberMe === undefined ? false : Boolean(rememberMe)
}

getCookie()
</script>

<style lang="scss" scoped>
$formItemHeight: 56px;
$itemBorderRadius: 10px;
.login {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url('../assets/images/bg-login.png');
  background-position: bottom;
  background-repeat: no-repeat;
  background-size: cover;
  .platform-logo {
    position: absolute;
    left: 96px;
    top: 42px;
  }
  .copyright-logo {
    width: 175px;
    position: absolute;
    left: 50%;
    bottom: 5px;
    transform: translateX(-50%);
  }

  .remember-me {
    margin-left: 115px;
    margin-top: 15px;
  }
}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  font-size: 22px;
  font-weight: bold;
}
.login-form {
  width: 600px;
  height: 400px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: $itemBorderRadius;
  padding: 30px 0 0;
  // box-shadow: inset 0 0 16px rgba(51, 89, 255, 0.85);
  box-shadow: 0px 8px 44px 2px rgba(0, 0, 0, 0.29);
  .login-title {
    display: block;
    margin: 0 auto 30px;
    text-align: center;
    color: #323232;
    font-size: 36px;
    font-family: 'customFont';
    font-weight: 400;
  }
  .form-item-box {
    width: 379px;
    margin: 34px auto 0;
    &:first-of-type {
      margin-top: 0;
    }
    &.login-btn-box {
      margin-top: 10px;
    }
  }
  :deep(.el-input) {
    height: $formItemHeight;
    background: transparent;
    .el-input__wrapper {
      // padding: 0;
      box-shadow: none;
      background: rgba(0, 0, 0, 0.3);
      border-radius: $itemBorderRadius;
      input {
        height: 100%;
        padding-left: 50px;
        color: #fcf8f8;
        font-size: 20px;
        border: 0;
        &:-webkit-autofill,
        &:-webkit-autofill:hover,
        &:-webkit-autofill:focus {
          box-shadow: 0 0 0 1000px rgba(58, 72, 107, 0) inset !important;
          /* -webkit-text-fill-color: #fff; */
          transition-delay: 99999s;
          transition:
            color 99999s ease-out,
            background-color 99999s ease-out;
        }
      }
      .el-input__password {
        font-size: 20px;
        color: #e6e6e6;
      }
    }

    .icon {
      position: absolute;
      left: 15px;
      top: 50%;
      transform: translateY(-50%);
      width: 24px;
      height: 24px;
      &.icon-user {
        background: url(@/assets/images/icon-user.png) no-repeat center/cover;
      }
      &.icon-password {
        background: url(@/assets/images/icon-password.png) no-repeat center/cover;
      }
      &.icon-code {
        background: url(@/assets/images/icon-code.png) no-repeat center/cover;
      }
      &.eyes {
        cursor: pointer;
        right: 10px;
        left: auto;
        width: auto;
        height: auto;
        font-size: 20px;
      }
    }
  }
  .login-btn {
    width: 460px;
    height: $formItemHeight;
    background: #437cff;
    border-radius: $itemBorderRadius;
    margin-top: 0px;
    border: 0;
    font-size: 22px;
    color: #fefeff;
  }
}
.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: $formItemHeight;
  float: right;
  .login-code-img {
    width: 100%;
    height: 100%;
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 40px;
  padding-left: 12px;
}
</style>
