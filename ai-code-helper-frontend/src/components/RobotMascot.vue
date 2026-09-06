<template>
  <!-- 吉祥物小机器人：纯 SVG 绘制，线条轻盈、配色马卡龙 -->
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 200 200"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    class="robot"
  >
    <!-- 天线 -->
    <path d="M100 40 L100 24" stroke="#5FC99B" stroke-width="5" stroke-linecap="round" />
    <circle cx="100" cy="18" r="7" fill="#FFEAA7" />
    <!-- 耳朵 -->
    <rect x="42" y="72" width="14" height="34" rx="7" fill="#90CAF9" />
    <rect x="144" y="72" width="14" height="34" rx="7" fill="#90CAF9" />
    <!-- 头部 -->
    <rect x="52" y="40" width="96" height="86" rx="26" fill="#E6F9F1" stroke="#5FC99B" stroke-width="5" />
    <!-- 眼睛 -->
    <circle cx="82" cy="76" r="8" fill="#2F4858" />
    <circle cx="118" cy="76" r="8" fill="#2F4858" />
    <circle cx="85" cy="73" r="3" fill="#FFFFFF" />
    <circle cx="121" cy="73" r="3" fill="#FFFFFF" />
    <!-- 腮红 -->
    <ellipse cx="68" cy="95" rx="8" ry="5" fill="#FFC9DE" opacity="0.9" />
    <ellipse cx="132" cy="95" rx="8" ry="5" fill="#FFC9DE" opacity="0.9" />
    <!-- 嘴巴 -->
    <path d="M88 100 Q100 112 112 100" stroke="#2F4858" stroke-width="4" stroke-linecap="round" fill="none" />
    <!-- 身体 -->
    <rect x="62" y="134" width="76" height="48" rx="20" fill="#FFFFFF" stroke="#5FC99B" stroke-width="5" />
    <!-- 胸口指示灯 -->
    <circle cx="100" cy="158" r="10" fill="#98E4C1" />
    <circle cx="100" cy="158" r="4" fill="#FFFFFF" />
  </svg>
</template>

<script setup>
defineProps({
  /** 边长（px），正方形 */
  size: {
    type: Number,
    default: 200,
  },
})
</script>

<style scoped>
.robot {
  /* 轻微上下浮动，增加"活"的感觉 */
  animation: float 3.5s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}
</style>
