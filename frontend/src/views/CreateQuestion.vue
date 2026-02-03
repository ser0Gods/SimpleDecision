<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import api from '@/lib/api'

type QuestionLite = { id: number, text: string }
type NewAnswer = { text: string, nextQuestionId: number | null }

const loading = ref(false)
const saving = ref(false)
const questions = reactive<QuestionLite[]>([])

const form = reactive({
  text: '',
  root: false,
  answers: [] as NewAnswer[]
})

function addAnswer() {
  form.answers.push({ text: '', nextQuestionId: null })
}

function removeAnswer(idx: number) {
  form.answers.splice(idx, 1)
}

async function loadQuestions() {
  loading.value = true
  try {
    const res = await api.get('api/admin/questions')
    questions.splice(0, questions.length, ...res.data)
  } finally {
    loading.value = false
  }
}

const message = ref<string | null>(null)
const error = ref<string | null>(null)

async function submit() {
  message.value = null
  error.value = null
  if (!form.text.trim()) {
    error.value = 'Question text is required.'
    return
  }
  // optional: basic validation for answers
  for (const a of form.answers) {
    if (!a.text.trim()) {
      error.value = 'All answers must have text.'
      return
    }
  }
  saving.value = true
  try {
    const createQ = await api.post('api/admin/questions', { text: form.text.trim(), root: form.root })
    const newQuestionId = createQ.data.id as number
    // create answers sequentially
    for (const a of form.answers) {
      await api.post('api/admin/answers', {
        text: a.text.trim(),
        questionId: newQuestionId,
        nextQuestionId: a.nextQuestionId || null,
      })
    }
    message.value = 'Question created successfully.'
    // reset form but keep loaded questions (now include the created one for linking from other new entries)
    form.text = ''
    form.root = false
    form.answers = []
    await loadQuestions()
  } catch (e: any) {
    error.value = e?.response?.data?.message || 'Failed to create question.'
  } finally {
    saving.value = false
  }
}

onMounted(loadQuestions)
</script>

<template>
  <main>
    <h1>Create Question</h1>
    <div v-if="loading">Loading existing questions...</div>

    <div v-if="message" class="msg success">{{ message }}</div>
    <div v-if="error" class="msg error">{{ error }}</div>

    <form @submit.prevent="submit" class="form">
      <label class="row">
        <span>Question text</span>
        <input v-model="form.text" type="text" placeholder="Enter question text" />
      </label>
      <label class="row chk">
        <input v-model="form.root" type="checkbox" />
        <span>Root question</span>
      </label>

      <div class="answers">
        <div class="answers-header">
          <h3>Answers</h3>
          <button type="button" @click="addAnswer">Add answer</button>
        </div>
        <div v-if="form.answers.length === 0" class="hint">No answers yet. Click "Add answer".</div>
        <div v-for="(a, idx) in form.answers" :key="idx" class="answer-item">
          <div class="row">
            <span>Text</span>
            <input v-model="a.text" type="text" placeholder="Answer text" />
          </div>
          <div class="row">
            <span>Next question (optional)</span>
            <select v-model.number="a.nextQuestionId">
              <option :value="null">— none / end —</option>
              <option v-for="q in questions" :key="q.id" :value="q.id">{{ q.text }}</option>
            </select>
          </div>
          <div class="row right">
            <button type="button" class="danger" @click="removeAnswer(idx)">Remove</button>
          </div>
        </div>
      </div>

      <div class="actions">
        <button type="submit" :disabled="saving">{{ saving ? 'Saving...' : 'Create' }}</button>
      </div>
    </form>
  </main>
</template>

<style scoped>
h1 { font-size: 28px; margin-bottom: 10px; }
.form { display: flex; flex-direction: column; gap: 12px; }
.row { display: flex; align-items: center; gap: 10px; }
.row > span { min-width: 140px; }
input[type="text"], select { flex: 1; padding: 6px 8px; border-radius: 6px; border: 1px solid #ccc; }
.chk { gap: 8px; }
.answers { background: #3f4426; padding: 10px; border-radius: 8px; }
.answers-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.hint { opacity: 0.8; font-style: italic; }
.answer-item { background: #2f331e; padding: 10px; border-radius: 6px; margin-bottom: 8px; display: flex; flex-direction: column; gap: 8px; }
.actions { display: flex; justify-content: flex-end; }
button { padding: 6px 10px; border-radius: 6px; border: none; cursor: pointer; background: #fea645; }
button:hover { background: #ffb56b; }
button.danger { background: #e57373; }
button.danger:hover { background: #ef9a9a; }
.msg { padding: 8px 10px; border-radius: 6px; }
.msg.success { background: #2e7d32; color: white; }
.msg.error { background: #c62828; color: white; }
</style>
