<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/lib/api'

type QuestionLite = { id: number, text: string }
type AdminAnswer = { id: number, text: string, questionId: number, nextQuestionId: number | null }
type AdminQuestionDetail = { id: number, text: string, answers: AdminAnswer[] }

const route = useRoute()
const router = useRouter()

// Selected question ID can be null until user picks from dropdown
const selectedId = ref<number | null>(route.params.id ? Number(route.params.id) : null)
watch(() => route.params.id as string | undefined, (v) => {
  selectedId.value = v ? Number(v) : null
})

const loading = ref(false)
const saving = ref(false)
const questions = reactive<QuestionLite[]>([])
const detail = reactive<AdminQuestionDetail>({ id: 0, text: '', answers: [] })

const message = ref<string | null>(null)
const error = ref<string | null>(null)

async function loadQuestions() {
  const res = await api.get('api/admin/questions')
  questions.splice(0, questions.length, ...res.data)
}

function clearDetail() {
  detail.id = 0
  detail.text = ''
  detail.answers.splice(0, detail.answers.length)
}

async function loadDetail() {
  if (!selectedId.value || selectedId.value <= 0 || Number.isNaN(selectedId.value)) {
    clearDetail()
    return
  }
  loading.value = true
  message.value = null
  error.value = null
  try {
    const res = await api.get(`api/admin/questions/${selectedId.value}/details`)
    const d = res.data as AdminQuestionDetail
    detail.id = d.id
    detail.text = d.text
    detail.answers.splice(0, detail.answers.length, ...d.answers)
  } catch (e: any) {
    error.value = e?.response?.data?.message || 'Failed to load question details.'
  } finally {
    loading.value = false
  }
}

async function saveQuestion() {
  if (!detail.id) return
  saving.value = true
  message.value = null
  error.value = null
  try {
    await api.put(`api/admin/questions/${detail.id}`, { text: detail.text, root: false })
    message.value = 'Question text saved.'
    await loadQuestions()
  } catch (e: any) {
    error.value = e?.response?.data?.message || 'Failed to save question.'
  } finally {
    saving.value = false
  }
}

function addAnswer() {
  if (!detail.id) return
  detail.answers.push({ id: 0, text: '', questionId: detail.id, nextQuestionId: null })
}

async function saveAnswer(ans: AdminAnswer) {
  if (!detail.id) return
  if (!ans.text.trim()) {
    error.value = 'Answer text is required.'
    return
  }
  saving.value = true
  message.value = null
  error.value = null
  try {
    if (ans.id && ans.id > 0) {
      await api.put(`api/admin/answers/${ans.id}`, {
        text: ans.text.trim(),
        questionId: detail.id,
        nextQuestionId: ans.nextQuestionId ?? null,
      })
      message.value = 'Answer updated.'
    } else {
      const res = await api.post('api/admin/answers', {
        text: ans.text.trim(),
        questionId: detail.id,
        nextQuestionId: ans.nextQuestionId ?? null,
      })
      ans.id = res.data.id
      message.value = 'Answer created.'
    }
  } catch (e: any) {
    error.value = e?.response?.data?.message || 'Failed to save answer.'
  } finally {
    saving.value = false
  }
}

async function deleteAnswer(ans: AdminAnswer, idx: number) {
  if (!detail.id) return
  if (!ans.id) {
    detail.answers.splice(idx, 1)
    return
  }
  saving.value = true
  message.value = null
  error.value = null
  try {
    await api.delete(`api/admin/answers/${ans.id}`)
    detail.answers.splice(idx, 1)
    message.value = 'Answer deleted.'
  } catch (e: any) {
    error.value = e?.response?.data?.message || 'Failed to delete answer.'
  } finally {
    saving.value = false
  }
}

// Load detail whenever the selection changes
watch(selectedId, async () => {
  await loadDetail()
})

onMounted(async () => {
  await loadQuestions()
  await loadDetail()
})
</script>

<template>
  <main>
    <h1>Edit Question</h1>
    <div v-if="loading">Loading...</div>
    <div v-if="message" class="msg success">{{ message }}</div>
    <div v-if="error" class="msg error">{{ error }}</div>

    <div v-if="!loading">
      <div class="form">
        <label class="row">
          <span>Select question</span>
          <select v-model.number="selectedId">
            <option :value="null">— select a question —</option>
            <option v-for="q in questions" :key="q.id" :value="q.id">{{ q.text }}</option>
          </select>
        </label>
      </div>

      <div v-if="detail.id">
        <div class="form">
          <label class="row">
            <span>Question text</span>
            <input v-model="detail.text" type="text" />
          </label>
          <div class="row right">
            <button @click="saveQuestion" :disabled="saving">{{ saving ? 'Saving...' : 'Save question' }}</button>
          </div>
        </div>

        <div class="answers">
          <div class="answers-header">
            <h3>Answers</h3>
            <button type="button" @click="addAnswer">Add answer</button>
          </div>
          <div v-if="detail.answers.length === 0" class="hint">No answers yet. Click "Add answer".</div>
          <div v-for="(a, idx) in detail.answers" :key="a.id || idx" class="answer-item">
            <div class="row">
              <span>Text</span>
              <input v-model="a.text" type="text" placeholder="Answer text" />
            </div>
            <div class="row">
              <span>Follow-up question</span>
              <select v-model.number="a.nextQuestionId">
                <option :value="null">— none / end —</option>
                <option v-for="q in questions" :key="q.id" :value="q.id">{{ q.text }}</option>
              </select>
            </div>
            <div class="row right gap">
              <button type="button" @click="saveAnswer(a)" :disabled="saving">{{ a.id ? 'Save' : 'Create' }}</button>
              <button type="button" class="danger" @click="deleteAnswer(a, idx)" :disabled="saving">Remove</button>
            </div>
          </div>
        </div>

        <div class="row">
          <button type="button" class="secondary" @click="router.push({ name: 'create' })">Create new question</button>
        </div>
      </div>
      <div v-else class="hint">Select a question above to begin editing.</div>
    </div>
  </main>
  
</template>

<style scoped>
h1 { font-size: 28px; margin-bottom: 10px; }
.form { display: flex; flex-direction: column; gap: 12px; margin-bottom: 12px; }
.row { display: flex; align-items: center; gap: 10px; }
.row > span { min-width: 160px; }
.row.right { justify-content: flex-end; }
.row.right.gap { gap: 8px; }
input[type="text"], select { flex: 1; padding: 6px 8px; border-radius: 6px; border: 1px solid #ccc; }
.answers { background: #3f4426; padding: 10px; border-radius: 8px; }
.answers-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.hint { opacity: 0.8; font-style: italic; }
.answer-item { background: #2f331e; padding: 10px; border-radius: 6px; margin-bottom: 8px; display: flex; flex-direction: column; gap: 8px; }
button { padding: 6px 10px; border-radius: 6px; border: none; cursor: pointer; background: #fea645; }
button:hover { background: #ffb56b; }
button.danger { background: #e57373; }
button.danger:hover { background: #ef9a9a; }
button.secondary { background: #607d8b; }
button.secondary:hover { background: #78909c; }
.msg { padding: 8px 10px; border-radius: 6px; }
.msg.success { background: #2e7d32; color: white; }
.msg.error { background: #c62828; color: white; }
</style>
