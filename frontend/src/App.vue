<script setup lang="ts">
import {RouterView} from 'vue-router'
import {computed, reactive, ref} from "vue";
import { useElementSize } from '@vueuse/core'

const navBar = ref(null);
const size = reactive(
    useElementSize(
        navBar,
        { width: 0, height: 0 },
        { box: 'border-box' },
    ),
)

const navBarHeight = computed(() => {
  return size.height+'px';
})
</script>

<template>

  <div id="nav" ref="navBar">
    <router-link to="/">Willkommen</router-link>
    <router-link to="/create">Create Question</router-link>
  </div>
  <div class="content-container">
    <div class="content-wrapper">
      <RouterView/>
    </div>
  </div>
</template>

<style scoped>
#nav {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 999;
  width: 100%;
  height: fit-content;
  text-align: center;
  background-color: #474D29dd;
}

#nav a {
  display: inline-block;
  padding: 5px;
  margin: 5px;
  border: transparent;
  background-color: #fea645;
  color: black;
  border-radius: 5px;
}


h1 {
  padding-bottom: 100px;
  font-size: 40px;
  font-weight: 500;
}

h2 {
  font-weight: 500;
  font-size: 2.6rem;
  top: -10px;
}

img {
  display: block;
  margin-left: auto;
  margin-right: auto;
  width: 50%;
}

.content-container {
  margin-top: v-bind('navBarHeight');
  width: 100%;
  max-width: 30rem;
}

.content-wrapper {
  background-color: #474D29dd;
  padding: 15px;
  border-radius: 15px;
  color: white;
  height: fit-content;
  width: 100%;
  min-width: 100%;
  max-width: 100%;
}
</style>
