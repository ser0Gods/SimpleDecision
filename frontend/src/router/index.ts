import { createRouter, createWebHistory } from 'vue-router'
import Decision from "@/views/Decision.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'decision', component: Decision }
  ]
})

export default router
