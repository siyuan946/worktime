<template>
  <div class="container py-5" style="max-width:400px;">
    <h2 class="mb-3 text-center">登录</h2>
    <div class="mb-2">
      <input class="form-control" v-model="username" placeholder="用户名" />
    </div>
    <div class="mb-2">
      <input type="password" class="form-control" v-model="password" placeholder="密码" />
    </div>
    <button class="btn btn-primary w-100" @click="login">登录</button>
    <div class="text-danger mt-2" v-if="error">{{ error }}</div>
  </div>
</template>

<script>
import axios from 'axios'
export default {
  data() {
    return { username: '', password: '', error: '' }
  },
  methods: {
    async login() {
      try {
        await axios.post('http://localhost:8080/api/auth/login', {
          username: this.username,
          password: this.password
        })
        this.$emit('logged-in')
      } catch (e) {
        this.error = '登录失败'
      }
    }
  }
}
</script>
