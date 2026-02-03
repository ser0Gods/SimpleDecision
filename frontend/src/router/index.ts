import { createRouter, createWebHistory } from 'vue-router'
import Decision from "@/views/Decision.vue";
import CreateQuestion from "@/views/CreateQuestion.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'decision', component: Decision },
    { path: '/create', name: 'create', component: CreateQuestion }
  ]
})

export default router
