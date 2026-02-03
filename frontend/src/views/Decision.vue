<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import api from '@/lib/api'

type AnswerDTO = { id: number, text: string }
type QuestionDTO = { id: number, text: string, answers: AnswerDTO[] }
type ProcessDTO = { id: number, text: string }

const loading = ref(false)
const current = ref<QuestionDTO | null>(null)
const history = reactive<AnswerDTO[]>([])
const processes = reactive<ProcessDTO[]>([])
const selectingProcess = ref(false)

async function loadCurrent() {
  loading.value = true
  try {
    const res = await api.get('api/graph/current')
    current.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  const res = await api.get('api/graph/history')
  history.splice(0, history.length, ...res.data)
}

async function choose(answerId: number) {
  loading.value = true
  try {
    const res = await api.post(`api/graph/answer/${answerId}`)
    current.value = res.data // could be null when finished
    await loadHistory()
  } finally {
    loading.value = false
  }
}

async function reset() {
  await api.post('api/graph/reset')
  await loadHistory()
  await loadCurrent()
  if (!current.value) {
    await loadProcessesAndMaybeStart()
  }
}

async function loadProcessesAndMaybeStart() {
  const res = await api.get('api/graph/processes')
  processes.splice(0, processes.length, ...res.data)
  if (processes.length === 1) {
    // auto start the only process
    await startProcess(processes[0].id)
  } else if (processes.length > 1) {
    selectingProcess.value = true
    current.value = null
  }
}

async function startProcess(processId: number) {
  selectingProcess.value = false
  loading.value = true
  try {
    const res = await api.post(`api/graph/process/${processId}/start`)
    current.value = res.data
    await loadHistory()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadHistory()
  // load current question; if null, let processes decide
  await loadCurrent()
  if (!current.value) {
    await loadProcessesAndMaybeStart()
  }
})
</script>

<template>
  <main>
    <h1>Decision Graph</h1>
    <div v-if="loading">Loading...</div>
    <!-- Process selection view (process = root question) -->
    <div v-if="selectingProcess">
      <h2>Select a process</h2>
      <div class="answers">
        <button v-for="p in processes" :key="p.id" class="answer" @click="startProcess(p.id)">{{ p.text }}</button>
      </div>
    </div>
    <div v-if="current">
      <h2>{{ current.text }}</h2>
      <div class="answers">
        <button v-for="a in current.answers" :key="a.id" class="answer" @click="choose(a.id)">{{ a.text }}</button>
      </div>
    </div>
    <div v-else>
      <p v-if="!selectingProcess">No further questions. You reached the end of the process.</p>
    </div>

    <div class="history">
      <h3>Your Answers (session)</h3>
      <ul>
        <li v-for="(a, idx) in history" :key="idx">{{ a.text }}</li>
      </ul>
      <button class="reset" @click="reset">Reset</button>
    </div>
  </main>
  
</template>

<style scoped>
h1 { font-size: 28px; margin-bottom: 10px; }
h2 { font-size: 22px; }
.answers { display: flex; flex-direction: column; gap: 8px; margin: 12px 0; }
.answer { padding: 8px 12px; border-radius: 6px; border: none; cursor: pointer; background: #fea645; }
.answer:hover { background: #ffb56b; }
.history { margin-top: 20px; }
.reset { margin-top: 10px; padding: 6px 10px; }
</style>
