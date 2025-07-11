<template>
  <div style="max-width: 600px; margin: auto;">
    <h1>Work Time Entry</h1>
    <section style="margin-bottom:20px;">
      <h2>Manual Entry</h2>
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
    </section>

    <section style="margin-bottom:20px;">
      <h2>Excel Upload</h2>
      <input type="file" @change="onFileChange">
      <button @click="upload" :disabled="!file">Upload</button>
    </section>

    <section>
      <h2>Existing Records</h2>
      <table border="1" cellpadding="4" cellspacing="0" style="width:100%;">
        <thead>
          <tr>
            <th>Code</th>
            <th>Name</th>
            <th>Start</th>
            <th>End</th>
            <th>Steps</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="wt in worktimes" :key="wt.id">
            <td>{{ wt.code }}</td>
            <td>{{ wt.name }}</td>
            <td>{{ wt.startDate }}</td>
            <td>{{ wt.endDate }}</td>
            <td>
              <ul>
                <li v-for="step in wt.steps" :key="step.id">
                  {{ step.process }}: {{ step.hours }}
                </li>
              </ul>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
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
      },
      file: null,
      worktimes: []
    }
  },
  created() {
    this.fetchWorktimes()
  },
  methods: {
    async submit() {
      await axios.post('http://localhost:8080/api/worktimes', this.form)
      alert('Saved')
      this.form = { code: '', name: '', startDate: '', endDate: '', process: '', hours: '' }
      this.fetchWorktimes()
    },
    onFileChange(e) {
      this.file = e.target.files[0]
    },
    async upload() {
      const data = new FormData()
      data.append('file', this.file)
      await axios.post('http://localhost:8080/api/worktimes/upload', data, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      this.file = null
      this.fetchWorktimes()
    },
    async fetchWorktimes() {
      const res = await axios.get('http://localhost:8080/api/worktimes')
      this.worktimes = res.data
    }
  }
}
</script>

<style>
input {
  margin: 4px;
}
</style>
