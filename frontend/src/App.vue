<template>
  <div style="max-width: 400px; margin: auto;">
    <h1>Work Time Entry</h1>
    <div>
      <label>Code: <input v-model="form.code"></label>
    </div>
    <div>
      <label>Name: <input v-model="form.name"></label>
    </div>
    <div>
      <label>Start Date: <input type="date" v-model="form.startDate"></label>
    </div>
    <div>
      <label>End Date: <input type="date" v-model="form.endDate"></label>
    </div>
    <div>
      <label>Process: <input v-model="form.process"></label>
    </div>
    <div>
      <label>Hours: <input type="number" step="0.1" v-model="form.hours"></label>
    </div>
    <button @click="submit">Submit</button>
  </div>
</template>

<script>
import axios from 'axios'
export default {
  data() {
    return {
      form: {
        code: '',
        name: '',
        startDate: '',
        endDate: '',
        process: '',
        hours: ''
      }
    }
  },
  methods: {
    async submit() {
      await axios.post('http://localhost:8080/api/worktimes', this.form)
      alert('Saved')
    }
  }
}
</script>

<style>
input {
  margin: 4px;
}
</style>
