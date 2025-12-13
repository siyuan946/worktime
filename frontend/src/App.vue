<template>
  <div>
    <login-page v-if="!loggedIn" @logged-in="onLogin"></login-page>
    <div v-else>
      <nav class="navbar navbar-expand navbar-light bg-white">
        <div class="container-fluid">
          <a class="navbar-brand" href="#">单件工时录入</a>
          <ul class="navbar-nav">
            <li class="nav-item"><router-link class="nav-link" to="/upload">Excel上传</router-link></li>
            <li class="nav-item" v-if="showAdvanced"><router-link class="nav-link" to="/records">扫码录入</router-link></li>
            <li class="nav-item" v-if="showAdvanced"><router-link class="nav-link" to="/workers">人员管理</router-link></li>
            <li class="nav-item" v-if="showAdvanced"><router-link class="nav-link" to="/processes">工序代码</router-link></li>
            <li class="nav-item"><router-link class="nav-link" to="/logs">操作记录</router-link></li>
            <li class="nav-item"><a href="#" class="nav-link" @click.prevent="logout">退出登录</a></li>
          </ul>
          <div class="d-flex align-items-center gap-2">
            <label class="mb-0 me-2 text-nowrap" for="codeModeSelect">码型</label>
            <select
              id="codeModeSelect"
              class="form-select form-select-sm"
              style="width: 130px;"
              v-model="codeMode"
              @change="onCodeModeChange"
            >
              <option value="qr">二维码（默认）</option>
              <option value="barcode">条形码</option>
            </select>
          </div>
        </div>
      </nav>
      <div class="container py-4">
        <router-view ref="view" @saved="onSaved" />
      </div>
    </div>
  </div>
</template>

<script>
import LoginPage from './components/LoginPage.vue'

const DEPARTMENT_PRODUCTION = 'production'
const RESTRICTED_PATHS = new Set(['/records', '/workers', '/processes'])

export default {
  components: { LoginPage },
  data() {
    return {
      loggedIn: false,
      department: '',
      navigationGuard: null,
      codeMode: localStorage.getItem('codeMode') || 'qr'
    }
  },
  computed: {
    showAdvanced() {
      return this.department !== DEPARTMENT_PRODUCTION
    }
  },
  created() {
    this.loggedIn = localStorage.getItem('loggedIn') === 'true'
    this.department = localStorage.getItem('department') || ''
    this.registerGuard()
    this.pushCodeMode()
    if (this.department === DEPARTMENT_PRODUCTION && RESTRICTED_PATHS.has(this.$route.path)) {
      this.$router.replace('/upload')
    }
  },
  watch: {
    department(newValue) {
      if (newValue === DEPARTMENT_PRODUCTION && RESTRICTED_PATHS.has(this.$route.path)) {
        this.$router.replace('/upload')
      }
    }
  },
  methods: {
    registerGuard() {
      if (this.navigationGuard) {
        return
      }
      this.navigationGuard = this.$router.beforeEach((to, from, next) => {
        if (this.department === DEPARTMENT_PRODUCTION && RESTRICTED_PATHS.has(to.path)) {
          next('/upload')
        } else {
          next()
        }
      })
    },
    onLogin(payload = {}) {
      this.loggedIn = true
      localStorage.setItem('loggedIn', 'true')
      if (payload.username) {
        localStorage.setItem('username', payload.username)
      }
      this.department = payload.department || localStorage.getItem('department') || ''
      if (this.department === DEPARTMENT_PRODUCTION && RESTRICTED_PATHS.has(this.$route.path)) {
        this.$router.replace('/upload')
      }
    },
    logout() {
      this.loggedIn = false
      this.department = ''
      localStorage.removeItem('loggedIn')
      localStorage.removeItem('username')
      localStorage.removeItem('department')
      this.$router.replace('/upload')
    },
    onCodeModeChange() {
      if (!this.codeMode) {
        this.codeMode = 'qr'
      }
      localStorage.setItem('codeMode', this.codeMode)
      this.pushCodeMode()
    },
    pushCodeMode() {
      this.$root.$emit('code-mode-changed', this.codeMode)
    },
    onSaved() {
      const v = this.$refs.view
      if (v && v.fetch) v.fetch()
    }
  }
}
</script>

<style>
.table input { margin: 2px; }
</style>
