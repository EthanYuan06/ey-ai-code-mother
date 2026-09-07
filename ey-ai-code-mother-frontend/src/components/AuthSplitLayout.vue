<script setup lang="ts">
defineProps<{ title: string }>()
</script>

<template>
  <div class="auth-page">
    <div class="auth-container">
      <!-- 左侧：项目介绍 -->
      <div class="auth-intro">
        <div class="intro-badge">
          <span class="badge-dot"></span>
          Code Zero Studio
        </div>
        <h1 class="intro-title">不写一行代码，创造你的应用</h1>
        <p class="intro-desc">一句话轻松创建网站应用</p>
        <div class="intro-cards">
          <div class="intro-card">
            <h3>对话式生成</h3>
            <p>一句话描述需求，AI 全程完成编码</p>
          </div>
          <div class="intro-card">
            <h3>一键部署</h3>
            <p>生成即用，秒级发布可分享链接</p>
          </div>
        </div>
      </div>
      <!-- 右侧：表单卡片 -->
      <div class="auth-card">
        <h2 class="auth-title">{{ title }}</h2>
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  flex: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background:
    linear-gradient(180deg, #f8fafc 0%, #f1f5f9 8%, #e2e8f0 20%, #cbd5e1 100%),
    radial-gradient(circle at 20% 80%, rgba(59, 130, 246, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(139, 92, 246, 0.12) 0%, transparent 50%);
  position: relative;
  overflow: hidden;
}

/* 科技感网格背景，与主页一致 */
.auth-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.05) 1px, transparent 1px);
  background-size: 100px 100px;
  pointer-events: none;
}

.auth-container {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 64px;
  width: 100%;
  max-width: 1200px;
}

/* 左侧介绍区 */
.auth-intro {
  flex: 1;
  min-width: 0;
}

.intro-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  font-size: 14px;
  color: #475569;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
}

.intro-title {
  margin: 0 0 20px;
  font-size: 48px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -1px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 50%, #10b981 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.intro-desc {
  margin: 0 0 40px;
  font-size: 18px;
  color: #64748b;
}

.intro-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  max-width: 560px;
}

.intro-card {
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  transition:
    border-color 0.25s,
    box-shadow 0.25s;
}

.intro-card:hover {
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.15);
}

.intro-card h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: #3b82f6;
}

.intro-card p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #475569;
}

/* 右侧表单卡片 */
.auth-card {
  width: 420px;
  flex-shrink: 0;
  padding: 40px 36px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.12);
}

.auth-title {
  margin: 0 0 24px;
  font-size: 24px;
  font-weight: 600;
  text-align: center;
  color: #1e293b;
}

/* 表单控件统一圆角与按钮质感 */
.auth-card :deep(.ant-input),
.auth-card :deep(.ant-input-affix-wrapper) {
  border-radius: 10px;
}

.auth-card :deep(.ant-btn-primary) {
  height: 44px;
  border-radius: 10px;
  font-size: 16px;
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.3);
  transition: all 0.25s;
}

/* 响应式：窄屏隐藏介绍区，卡片居中 */
@media (max-width: 768px) {
  .auth-intro {
    display: none;
  }

  .auth-container {
    justify-content: center;
  }

  .auth-card {
    width: 100%;
    max-width: 420px;
    padding: 32px 24px;
  }
}
</style>
