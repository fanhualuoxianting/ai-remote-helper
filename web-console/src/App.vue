<template>
  <div id="app">
    <header>
      <h1>AI Remote Helper Web Console</h1>
    </header>
    <main>
      <section class="devices">
        <h2>在线设备</h2>
        <div v-if="loading">加载中...</div>
        <div v-else-if="devices.length === 0">暂无在线设备</div>
        <div v-else class="device-list">
          <div v-for="device in devices" :key="device.id" class="device-card">
            <h3>{{ device.deviceName || device.id }}</h3>
            <p>状态: {{ device.status }}</p>
            <p>最后在线: {{ device.lastOnlineAt }}</p>
          </div>
        </div>
      </section>
      <section class="health">
        <h2>系统状态</h2>
        <div v-if="health">
          <p>状态: {{ health.status }}</p>
          <p>在线设备: {{ health.devices }}</p>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'App',
  data() {
    return {
      devices: [],
      health: null,
      loading: true
    }
  },
  async mounted() {
    await this.loadData()
  },
  methods: {
    async loadData() {
      try {
        const [devicesRes, healthRes] = await Promise.all([
          axios.get('/api/devices/online'),
          axios.get('/console/health')
        ])
        this.devices = devicesRes.data
        this.health = healthRes.data
      } catch (error) {
        console.error('Failed to load data:', error)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style>
#app {
  font-family: Arial, sans-serif;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

header {
  background: #2c3e50;
  color: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

h1 {
  margin: 0;
}

section {
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}

.device-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.device-card {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 15px;
}

.device-card h3 {
  margin-top: 0;
  color: #2c3e50;
}
</style>
