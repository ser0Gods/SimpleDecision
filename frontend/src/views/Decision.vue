<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import api from '@/lib/api'

type AnswerDTO = { id: number, text: string }
type QuestionDTO = { id: number, text: string, answers: AnswerDTO[] }
type ProcessDTO = { id: number, name: string }

const loading = ref(false)
const current = reactive<QuestionDTO[]>([])
const history = reactive<AnswerDTO[]>([])
const processes = reactive<ProcessDTO[]>([])
const selectingProcess = ref(false)

async function loadCurrent() {
  loading.value = true
  try {
    const res = await api.get('api/graph/current')
    const data = res.data as QuestionDTO[] | null
    current.splice(0, current.length)
    if (Array.isArray(data)) {
      current.push(...data)
    } else if (data === null) {
      // no process selected yet (and multiple exist)
      current.splice(0, current.length)
    }
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
    const data = res.data as QuestionDTO[]
    current.splice(0, current.length)
    if (Array.isArray(data)) {
      current.push(...data)
    }
    await loadHistory()
  } finally {
    loading.value = false
  }
}

async function reset() {
  await api.post('api/graph/reset')
  await loadHistory()
  await loadCurrent()
  if (current.length === 0) {
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
    current.splice(0, current.length)
  }
}

async function startProcess(processId: number) {
  selectingProcess.value = false
  loading.value = true
  try {
    const res = await api.post(`api/graph/process/${processId}/start`)
    const data = res.data as QuestionDTO[]
    current.splice(0, current.length)
    if (Array.isArray(data)) current.push(...data)
    await loadHistory()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadHistory()
  // load current question; if null, let processes decide
  await loadCurrent()
  if (current.length === 0) {
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
        <button v-for="p in processes" :key="p.id" class="answer" @click="startProcess(p.id)">{{ p.name }}</button>
      </div>
    </div>
    <div v-if="current.length > 0">
      <div v-for="q in current" :key="q.id" class="question">
        <h2>{{ q.text }}</h2>
        <div class="answers">
          <button v-for="a in q.answers" :key="a.id" class="answer" @click="choose(a.id)">{{ a.text }}</button>
        </div>
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
.question { border: 1px solid #ddd; padding: 10px; margin: 10px 0; border-radius: 6px; }
.answers { display: flex; flex-direction: column; gap: 8px; margin: 12px 0; }
.answer { padding: 8px 12px; border-radius: 6px; border: none; cursor: pointer; background: #fea645; }
.answer:hover { background: #ffb56b; }
.history { margin-top: 20px; }
.reset { margin-top: 10px; padding: 6px 10px; }
</style>
