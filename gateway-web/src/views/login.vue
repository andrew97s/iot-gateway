<template>
  <div class="login">
    <el-form class="login-form" :model="loginForm" :rules="loginRules" ref="loginRef">
      <div class="title-box">
        <h3 class="title">{{ defaultSettings.loginTitle }}</h3>
        <div class="gap-line"></div>
        <p class="describe">JBZA-HSG2000</p>

        <template v-if="validDays !== -1">
          <div class="login-tip" v-if="validDays > 0">
            <CircleCheckFilled class="icon-tips info" />
            您的产品授权时间剩余<span class="day-text" :class="{ warning: validDays <= tipDay }">{{ validDays }}</span>
            天
            <a class="btn-active" href="javascript:;" @click="toActive">立即激活</a>
          </div>
          <div class="login-tip" v-if="validDays <= 0">
            <Warning class="icon-tips" />
            您的产品授权文件不存在或已过期！
            <a class="btn-active" href="javascript:;" @click="toActive">立即激活</a>
          </div>
        </template>
      </div>

      <el-form-item prop="username">
        <el-input v-model="loginForm.username" auto-complete="off" placeholder="账号" size="large" type="text">
          <template #prefix><svg-icon class="el-input__icon input-icon" icon-class="user" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          auto-complete="off"
          placeholder="密码"
          :show-password="true"
          size="large"
          type="password"
          @keyup.enter="handleLogin"
        >
          <template #prefix><svg-icon class="el-input__icon input-icon" icon-class="password" /></template>
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="defaultSettings.needVerify">
        <el-input v-model="loginForm.code" auto-complete="off" placeholder="验证码" size="large" style="width: 63%" @keyup.enter="handleLogin">
          <template #prefix><svg-icon class="el-input__icon input-icon" icon-class="validCode" /></template>
        </el-input>
        <div class="login-code">
          <img class="login-code-img" :src="codeUrl" @click="getCode" />
        </div>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" style="margin: 0px 0px 25px 0px">记住密码</el-checkbox>
      <el-form-item style="width: 100%">
        <el-button :loading="loading" size="large" style="width: 100%" type="primary" @click.prevent="handleLogin">
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer" v-if="defaultSettings.showCopyrightLogo">
      <span>Copyright © 2023-{{ new Date().getFullYear() }}</span>
    </div>

    <el-dialog v-model="openActiveModal" append-to-body class="active-modal long-dialog" title="授权激活" width="450px">
      <el-button type="primary" @click="downloadFile()">生成授权信息</el-button>
      <el-divider style="margin: 15px 0"></el-divider>
      <FileUpload
        :file-size="100"
        :file-type="['lic']"
        upload-tips="上传授权文件"
        :upload-url="'/license/importLicense'"
        @onSuccess="uploadSuccess"
      />
      <div class="active-tip">
        <p class="tip-subtitle">操作步骤：</p>
        <div class="tip-content">
          <ul class="tip-list">
            <li class="list-item">1、点击生成授权信息按钮，导出授权信息文件。</li>
            <li class="list-item">
              2、将授权信息文件发送至青鸟智安服务邮箱，邮箱地址：
              <div style="padding-left: 22px">qingniaozhian@jbewt.com。</div>
            </li>
            <li class="list-item">3、点击上传授权文件按钮，上传授权文件到本系统，即可激活。</li>
            <el-divider style="margin: 15px 0"></el-divider>
            <li class="list-item" style="color: #f00; font-weight: bold">说明：</li>
            <li class="list-item" style="color: #f00; font-weight: bold">
              1、上传新的授权文件，授权到期时间直接累加，若功能授权发生改变时，以授权文件最新授权时间为准。
            </li>
            <li class="list-item" style="color: #f00; font-weight: bold">2、邮件内容需包含：项目名称、项目地址、授权单位和授权信息文件。</li>
          </ul>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import defaultSettings from '@/settings'
import { getCodeImg } from '@/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'
import { getLicenseInfo } from '@/api/system/license'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
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

const codeUrl = ref('')
const loading = ref(false)
const redirect = ref(undefined)
const openActiveModal = ref(false)
const tipDay = 15
const validDays = ref(-1)

watch(
  route,
  (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
  },
  { immediate: true }
)

getLicenseInfo().then((res) => {
  validDays.value = res.data?.validDays || 0
})

function handleLogin() {
  proxy.$refs.loginRef.validate(async (valid) => {
    if (valid) {
      loading.value = true
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set('username', loginForm.value.username, { expires: 30 })
        Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set('rememberMe', loginForm.value.rememberMe, { expires: 30 })
      } else {
        // 否则移除
        Cookies.remove('username')
        Cookies.remove('password')
        Cookies.remove('rememberMe')
      }
      try {
        // 调用action的登录方法
        if (defaultSettings.needVerify) {
          await userStore.login(loginForm.value)
        } else {
          await userStore.loginSimple(loginForm.value)
        }
        loading.value = false
        // 重新获取验证码
        if (defaultSettings.needVerify) {
          getCode()
        }
        router.push({ path: redirect.value || defaultSettings.homePath })
      } catch (error) {
        loading.value = false
        // 重新获取验证码
        if (defaultSettings.needVerify) {
          getCode()
        }
      }
    }
  })
}

function getCode() {
  getCodeImg().then((res) => {
    if (defaultSettings.needVerify) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get('username')
  const password = Cookies.get('password')
  const rememberMe = Cookies.get('rememberMe')
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

function downloadFile() {
  proxy.download('/license/exportServerInfos', {}, `box_${new Date().toLocaleDateString()}.inf`)
}

function uploadSuccess() {
  proxy.$modal.msgSuccess('激活成功，页面即将刷新...')
  openActiveModal.value = false
  setTimeout(() => {
    window.location.reload()
  }, 2000)
}

function toActive() {
  openActiveModal.value = true
}

getCode()
getCookie()
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.module.scss';
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url('../assets/images/bg-login.png');
  background-size: cover;
}
.title-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin: 0px auto 20px auto;

  .title {
    font-size: 22px;
    font-weight: bold;
    color: #fff;
    letter-spacing: 12px;
    margin-right: -12px;
  }
  .gap-line {
    height: 2px;
    width: 124px;
    background: #fff;
    margin: 4px 0;
  }
  .describe {
    font-weight: 550;
    font-style: italic;
    font-size: 16px;
    color: #fff;
  }
}

.login-form {
  border-radius: 6px;
  background: #141414;
  width: 400px;
  padding: 25px 25px 5px 25px;
  .el-input {
    height: 40px;
    input {
      height: 40px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 0px;
  }
}
.login-code {
  width: 30%;
  height: 40px;
  margin-left: 5px;
  float: right;
  img {
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
}

.login-tip {
  width: 100%;
  height: 38px;
  line-height: 38px;
  margin: 12px 0 0 0;
  padding: 0 10px;
  text-align: left;
  font-size: 14px;
  color: #fff;
  background-color: #00000017;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  display: flex;
  align-items: center;
  .icon-tips {
    width: 20px;
    color: #f00b0b;
    margin-right: 5px;
    &.info {
      color: #237cf8d0;
    }
  }
  .day-text {
    color: #237df8;
    font-weight: bold;
    font-size: 16px;
    margin: 0 4px;
    &.warning {
      color: #f00b0b;
    }
  }
  .btn-active {
    position: relative;
    color: #237df8;
    cursor: pointer;
    margin-left: 10px;
    &::after {
      content: '';
      position: absolute;
      left: 0;
      bottom: 8px;
      width: 100%;
      height: 1px;
      background: #237df8;
    }
  }
}
.login-code {
  width: 30%;
  height: 40px;
  margin-left: 5px;
  float: right;
  img {
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
}

.active-tip {
  margin: 25px 0 0;
  width: 100%;
  padding: 10px 20px;
  font-size: 14px;
  color: #333;
  border-radius: 4px;
  // background-color: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(124, 113, 113, 0.6);
  .tip-subtitle {
    line-height: 1.4;
    margin: 5px 0;
  }
  .tip-list {
    .list-item {
      line-height: 2;
    }
  }
}
</style>
