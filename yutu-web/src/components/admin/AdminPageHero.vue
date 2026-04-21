<template>
  <section class="admin-page-hero">
    <div>
      <div class="admin-page-kicker">{{ kicker }}</div>
      <h2>{{ title }}</h2>
    </div>
  </section>
</template>

<script setup>
defineProps({
  kicker: {
    type: String,
    default: "Admin Module"
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ""
  },
  note: {
    type: String,
    default: ""
  }
});
</script>

<style scoped>
.admin-page-hero {
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  gap: 18px;
  padding: 24px 26px;
  border-radius: 22px;
  background: linear-gradient(135deg, #081526, #0f4c53 58%, #1d4ed8);
  color: #f8fafc;
}

.admin-page-kicker {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.admin-page-hero h2 {
  margin: 14px 0 10px;
  font-size: 30px;
}

@media (max-width: 900px) {
  .admin-page-hero {
    flex-direction: column;
  }
}
</style>
