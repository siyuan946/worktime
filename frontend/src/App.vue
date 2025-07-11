<script setup>
import { ref } from 'vue';
import axios from 'axios';

const form = ref({
  code: '',
  name: '',
  startDate: '',
  endDate: '',
  process: '',
  hours: ''
});

const submit = async () => {
  await axios.post('http://localhost:8080/api/worktimes', form.value);
  alert('Saved');
};
</script>

<template>
  <div style="max-width: 400px; margin: auto;">
    <h1>Work Time Entry</h1>
    <div>
      <label>Code: <input v-model="form.code" /></label>
    </div>
    <div>
      <label>Name: <input v-model="form.name" /></label>
    </div>
    <div>
      <label>Start Date: <input type="date" v-model="form.startDate" /></label>
    </div>
    <div>
      <label>End Date: <input type="date" v-model="form.endDate" /></label>
    </div>
    <div>
      <label>Process: <input v-model="form.process" /></label>
    </div>
    <div>
      <label>Hours: <input type="number" step="0.1" v-model="form.hours" /></label>
    </div>
    <button @click="submit">Submit</button>
  </div>
</template>

<style>
input {
  margin: 4px;
}
</style>
