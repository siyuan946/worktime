<template>
  <div class="worksteps">
    <div v-for="step in worksteps" :key="step.id" class="workstep">
      <h3>{{ step.name }}</h3>
      <input type="date" v-model="step.completionDate" />
      <input type="number" v-model.number="step.quantity" placeholder="完成数量" />
      <button @click="save(step)">保存</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const worksteps = ref([
  { id: 1, name: '工序1', completionDate: '', quantity: null },
  { id: 2, name: '工序2', completionDate: '', quantity: null }
])

function save(step) {
  fetch(`/api/worksteps/${step.id}/records`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ date: step.completionDate, quantity: step.quantity })
  })
    .then(res => res.json())
    .then(data => {
      if (data.message && data.message.includes('超产')) {
        window.alert(data.message)
      }
      window.alert('保存成功')
    })
    .catch(() => {
      window.alert('保存失败')
    })
}
</script>

<style scoped>
.worksteps {
  padding: 16px;
}
.workstep {
  margin-bottom: 12px;
}
</style>
