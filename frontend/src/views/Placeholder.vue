<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <el-card class="page-card" shadow="never">
    <el-empty :description="`「${route.meta.title}」功能开发中`" />
  </el-card>
</template>
