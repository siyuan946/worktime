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

export default {
  components: { LoginPage },
  data() {
    return {
      loggedIn: false,
      department: ''
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
  },
  methods: {
    onLogin(payload = {}) {
      this.loggedIn = true
      localStorage.setItem('loggedIn', 'true')
      this.department = payload.department || localStorage.getItem('department') || ''
    },
    logout() {
      this.loggedIn = false
      this.department = ''
      localStorage.removeItem('loggedIn')
      localStorage.removeItem('username')
      localStorage.removeItem('department')
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
