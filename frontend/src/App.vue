<template>
  <div>
    <login-page v-if="!loggedIn" @logged-in="onLogin"></login-page>
    <div v-else>
      <nav class="navbar navbar-expand navbar-light bg-white">
        <div class="container-fluid">
          <a class="navbar-brand" href="#">工时录入</a>
          <ul class="navbar-nav">
            <li class="nav-item"><router-link class="nav-link" to="/upload">Excel上传</router-link></li>
            <li class="nav-item"><router-link class="nav-link" to="/records">扫码录入</router-link></li>
            <li class="nav-item"><router-link class="nav-link" to="/workers">人员管理</router-link></li>
            <li class="nav-item"><router-link class="nav-link" to="/processes">工序代码</router-link></li>
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

export default {
  components: { LoginPage },
  data() {
    return { loggedIn: false }
  },
  methods: {
    onLogin() { this.loggedIn = true },
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
