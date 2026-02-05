import { createRouter, createWebHistory } from 'vue-router'
import Decision from "@/views/Decision.vue";
import CreateQuestion from "@/views/CreateQuestion.vue";
import QuestionEditor from "@/views/QuestionEditor.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'decision', component: Decision },
    { path: '/create', name: 'create', component: CreateQuestion },
    // Editor can be opened without an ID; question is selectable inside the view
    { path: '/admin/questions', name: 'question-editor', component: QuestionEditor },
    { path: '/admin/questions/:id', name: 'question-edit', component: QuestionEditor, props: true }
  ]
})

export default router
