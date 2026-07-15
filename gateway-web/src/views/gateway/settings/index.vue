<template>
  <div class="gw-page">
    <div class="gw-card">
      <el-tabs v-model="activeTab" class="settings-tabs">
        <!-- ==================== 网络 / IP 配置 ==================== -->
        <el-tab-pane label="网络 / IP 配置" name="network">
          <div class="tab-body" v-loading="netLoading">
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="mb14"
              title="下方展示网关宿主机的实时网卡信息；修改后的 IP 配置将保存至网关配置库，实际写入操作系统需配合部署环境的网络管理工具（netplan / nmcli）。"
            />
            <el-empty v-if="!netLoading && nics.length === 0" description="未识别到可配置网卡" />
            <el-row :gutter="14">
              <el-col :xs="24" :lg="12" v-for="nic in nics" :key="nic.name">
                <div class="gw-card nic-card">
                  <div class="gw-card-head">
                    <h2>网卡 {{ nic.name }}</h2>
                    <span class="gw-badge" :class="nic.up ? 'ok' : 'off'">{{ nic.up ? '已连接' : '未连接' }}</span>
                  </div>
                  <div class="gw-card-body">
                    <el-form label-width="90px" size="default">
                      <el-form-item label="获取方式">
                        <el-radio-group v-model="netForm[nic.name].mode">
                          <el-radio label="dhcp">DHCP 自动获取</el-radio>
                          <el-radio label="static">静态 IP</el-radio>
                        </el-radio-group>
                      </el-form-item>
                      <template v-if="netForm[nic.name].mode === 'static'">
                        <el-form-item label="IP 地址">
                          <el-input v-model="netForm[nic.name].ip" placeholder="如 192.168.1.100" />
                        </el-form-item>
                        <el-form-item label="子网掩码">
                          <el-input v-model="netForm[nic.name].netmask" placeholder="如 255.255.255.0" />
                        </el-form-item>
                        <el-form-item label="默认网关">
                          <el-input v-model="netForm[nic.name].gateway" placeholder="可留空" />
                        </el-form-item>
                        <el-form-item label="DNS">
                          <el-input v-model="netForm[nic.name].dns" placeholder="多个以英文逗号分隔，可留空" />
                        </el-form-item>
                      </template>
                      <el-form-item label=" ">
                        <div class="gw-muted gw-small">
                          MAC：<span class="gw-mono">{{ nic.mac || '-' }}</span>
                          <template v-if="currentAddr(nic)"> · 当前地址 <span class="gw-mono">{{ currentAddr(nic) }}</span></template>
                        </div>
                      </el-form-item>
                    </el-form>
                  </div>
                </div>
              </el-col>
            </el-row>
            <div class="tab-foot" v-if="nics.length > 0">
              <el-button icon="Refresh" @click="loadNetwork">重新读取网卡</el-button>
              <el-button type="primary" :loading="netSaving" v-hasPermi="['system:config:edit']" @click="saveNetwork">保存网络配置</el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- ==================== 系统核心参数 ==================== -->
        <el-tab-pane label="系统核心参数" name="core">
          <div class="tab-body" v-loading="coreLoading">
            <el-row :gutter="14">
              <el-col :xs="24" :lg="12">
                <div class="gw-card">
                  <div class="gw-card-head"><h2>网关标识</h2></div>
                  <div class="gw-card-body">
                    <el-form label-width="170px">
                      <el-form-item label="网关名称">
                        <el-input v-model="coreForm['gateway.name']" placeholder="如：园区一号网关" />
                      </el-form-item>
                      <el-form-item label="网关编码">
                        <el-input v-model="coreForm['gateway.code']" placeholder="如：GW-0001" />
                        <div class="gw-muted gw-small">同步上级平台时作为本网关标识</div>
                      </el-form-item>
                    </el-form>
                  </div>
                </div>
              </el-col>
              <el-col :xs="24" :lg="12">
                <div class="gw-card">
                  <div class="gw-card-head"><h2>数据与存储</h2></div>
                  <div class="gw-card-body">
                    <el-form label-width="170px">
                      <el-form-item label="数据保留天数">
                        <el-input-number v-model="coreNumbers.preservedDays" :min="1" :max="3650" />
                        <div class="gw-muted gw-small">消息/错误日志超期后每日凌晨自动清理</div>
                      </el-form-item>
                      <el-form-item label="设备离线判定阈值（秒）">
                        <el-input-number v-model="coreNumbers.offlineThreshold" :min="10" :max="86400" />
                      </el-form-item>
                    </el-form>
                  </div>
                </div>
              </el-col>
              <el-col :xs="24" :lg="12">
                <div class="gw-card">
                  <div class="gw-card-head"><h2>插件与同步策略</h2></div>
                  <div class="gw-card-body">
                    <el-form label-width="170px">
                      <el-form-item label="插件异常自动重启">
                        <el-switch v-model="coreSwitches.autorestart" />
                        <div class="gw-muted gw-small">网关每 10 分钟巡检插件存活并自动拉起异常插件</div>
                      </el-form-item>
                      <el-form-item label="新设备自动同步上级平台">
                        <el-switch v-model="coreSwitches.autosync" />
                        <div class="gw-muted gw-small">关闭时需在设备管理中手工指定同步</div>
                      </el-form-item>
                    </el-form>
                  </div>
                </div>
              </el-col>
            </el-row>
            <div class="tab-foot">
              <el-button type="primary" :loading="coreSaving" v-hasPermi="['system:config:edit']" @click="saveCore">保存核心参数</el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- ==================== 上级平台配置 ==================== -->
        <el-tab-pane label="上级平台配置" name="upstream">
          <div class="tab-body" v-loading="upstreamLoading">
            <!-- 消息推送通道（gateway 插件） -->
            <div class="gw-card mb14">
              <div class="gw-card-head">
                <h2>消息推送通道</h2>
                <span v-if="gatewayPlugin" class="gw-badge" :class="gatewayAlive ? 'ok' : (gatewayPlugin.status === '1' ? 'err' : 'off')">
                  {{ gatewayPlugin.status !== '1' ? '已禁用' : (gatewayAlive ? '推送中' : '已断开') }}
                </span>
              </div>
              <div class="gw-card-body">
                <el-empty v-if="!gatewayPlugin" description="未找到消息推送插件（code=gateway），请检查插件配置" :image-size="70" />
                <template v-else>
                  <el-descriptions :column="2" border size="small">
                    <el-descriptions-item label="推送方式">
                      <el-tag size="small" :type="pushTypeTag">{{ pushTypeLabel }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="启用状态">
                      <el-switch
                        v-model="gatewayPlugin.status"
                        active-value="1"
                        inactive-value="0"
                        :loading="gatewaySwitchLoading"
                        active-text="启用"
                        inactive-text="禁用"
                        inline-prompt
                        @change="toggleGatewayPlugin"
                      />
                    </el-descriptions-item>
                    <el-descriptions-item label="目标地址" :span="2">
                      <span class="gw-mono gw-small">{{ gatewayTargetSummary }}</span>
                    </el-descriptions-item>
                  </el-descriptions>
                  <div class="mt12">
                    <el-button type="primary" plain icon="Edit" v-hasPermi="['sys:platform:edit']" @click="openGatewayConfig">配置推送通道</el-button>
                    <span class="gw-muted gw-small" style="margin-left: 10px">
                      接入消息统一转换后经此通道同步至上级平台（支持 HTTP / MQ / Redis，可扩展）；保存后热生效
                    </span>
                  </div>
                </template>
              </div>
            </div>

            <!-- 级联上级平台 -->
            <div class="gw-card">
              <div class="gw-card-head">
                <h2>级联上级平台（WebSocket 级联）</h2>
                <el-button size="small" type="primary" icon="Plus" v-hasPermi="['sys:cascade:add']" @click="openCascadeDialog()">新增平台</el-button>
              </div>
              <div class="gw-card-body no-pad">
                <el-table :data="cascadeList" size="default">
                  <el-table-column label="平台名称" prop="name" min-width="140" />
                  <el-table-column label="平台代码" prop="code" width="140" align="center">
                    <template #default="scope"><span class="gw-mono">{{ scope.row.code }}</span></template>
                  </el-table-column>
                  <el-table-column label="平台地址" prop="ip" min-width="180">
                    <template #default="scope"><span class="gw-mono">{{ scope.row.ip }}</span></template>
                  </el-table-column>
                  <el-table-column label="连接状态" width="110" align="center">
                    <template #default="scope">
                      <span class="gw-badge" :class="scope.row.online === '1' ? 'ok' : 'off'">
                        {{ scope.row.online === '1' ? '已连接' : '未连接' }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="启用" width="90" align="center">
                    <template #default="scope">
                      <el-switch
                        v-model="scope.row.status"
                        active-value="1"
                        inactive-value="0"
                        v-hasPermi="['sys:cascade:edit']"
                        @change="toggleCascade(scope.row)"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="140" align="center">
                    <template #default="scope">
                      <el-button link type="primary" v-hasPermi="['sys:cascade:edit']" @click="openCascadeDialog(scope.row)">编辑</el-button>
                      <el-button link type="danger" v-hasPermi="['sys:cascade:remove']" @click="deleteCascade(scope.row)">删除</el-button>
                    </template>
                  </el-table-column>
                  <template #empty>
                    <el-empty description="暂无级联上级平台" :image-size="70" />
                  </template>
                </el-table>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 推送通道配置对话框 -->
    <el-dialog v-model="gatewayConfigOpen" title="配置消息推送通道" width="620px" append-to-body>
      <el-form label-width="110px">
        <el-form-item label="推送方式">
          <el-select v-model="gatewayForm.pushType" style="width: 100%">
            <el-option label="MQ 消息队列（RabbitMQ）" value="mq" />
            <el-option label="HTTP URL 推送" value="url" />
            <el-option label="Redis 队列" value="redis" />
          </el-select>
          <div class="gw-muted gw-small">切换后下方参数动态变化；保存后插件自动重载生效</div>
        </el-form-item>

        <template v-if="gatewayForm.pushType === 'url'">
          <el-form-item label="推送地址列表">
            <el-input v-model="gatewayForm.pushUrls" type="textarea" :rows="4" placeholder="每行一个 URL，或英文逗号分隔" />
          </el-form-item>
        </template>

        <template v-if="gatewayForm.pushType === 'mq'">
          <el-form-item label="MQ 地址">
            <el-input v-model="gatewayForm.ip" placeholder="RabbitMQ 主机地址" style="width: 60%" />
            <el-input-number v-model="gatewayForm.port" :min="1" :max="65535" placeholder="端口" style="width: 38%; margin-left: 2%" />
          </el-form-item>
          <el-form-item label="vhost">
            <el-input v-model="gatewayForm.vhost" placeholder="默认 /" />
          </el-form-item>
          <el-form-item label="队列名">
            <el-input v-model="gatewayForm.queueName" placeholder="默认 za_monitor" />
          </el-form-item>
        </template>

        <template v-if="gatewayForm.pushType === 'redis'">
          <el-form-item label="Redis 地址">
            <el-input v-model="gatewayForm.ip" placeholder="默认 127.0.0.1" style="width: 60%" />
            <el-input-number v-model="gatewayForm.port" :min="1" :max="65535" placeholder="6379" style="width: 38%; margin-left: 2%" />
          </el-form-item>
          <el-form-item label="库编号（db）">
            <el-input v-model="gatewayForm.db" placeholder="默认 6" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="gatewayConfigOpen = false">取 消</el-button>
        <el-button type="primary" :loading="gatewaySaving" @click="saveGatewayConfig">保存并生效</el-button>
      </template>
    </el-dialog>

    <!-- 级联平台编辑对话框 -->
    <el-dialog v-model="cascadeOpen" :title="cascadeForm.id ? '编辑级联平台' : '新增级联平台'" width="480px" append-to-body>
      <el-form :model="cascadeForm" label-width="90px" ref="cascadeFormRef">
        <el-form-item label="平台名称" prop="name" :rules="[{ required: true, message: '请输入平台名称' }]">
          <el-input v-model="cascadeForm.name" placeholder="如：市级物联平台" />
        </el-form-item>
        <el-form-item label="平台代码" prop="code" :rules="[{ required: true, message: '请输入平台代码' }]">
          <el-input v-model="cascadeForm.code" placeholder="唯一代码，如 city-pf" />
        </el-form-item>
        <el-form-item label="平台地址" prop="ip" :rules="[{ required: true, message: '请输入平台地址' }]">
          <el-input v-model="cascadeForm.ip" placeholder="上级平台级联接入地址" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="cascadeForm.status" active-value="1" inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cascadeOpen = false">取 消</el-button>
        <el-button type="primary" :loading="cascadeSaving" @click="saveCascade">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="GatewaySettings">
import {
  getCoreParams, saveCoreParams,
  listNetworkInterfaces, getNetworkConfig, saveNetworkConfig
} from '@/api/sys/setting'
import { selectPlatform, updatePlatform, getAllPlatformStats } from '@/api/sys/platform'
import { listCascade, addCascade, updateCascade, delCascade } from '@/api/sys/cascade'

const { proxy } = getCurrentInstance()

const activeTab = ref('network')

// ==================== 网络 / IP ====================
const netLoading = ref(false)
const netSaving = ref(false)
const nics = ref([])
const netForm = reactive({})

function currentAddr(nic) {
  const a = (nic.addresses || [])[0]
  return a ? `${a.ip}/${a.prefixLength}` : ''
}

async function loadNetwork() {
  netLoading.value = true
  try {
    const [nicRes, cfgRes] = await Promise.all([
      listNetworkInterfaces(),
      getNetworkConfig().catch(() => ({ data: '{}' }))
    ])
    nics.value = nicRes.data || []
    let saved = {}
    try { saved = JSON.parse(cfgRes.data || '{}') } catch {}
    nics.value.forEach(nic => {
      const s = saved[nic.name] || {}
      const cur = (nic.addresses || [])[0] || {}
      netForm[nic.name] = {
        mode: s.mode || 'static',
        ip: s.ip ?? cur.ip ?? '',
        netmask: s.netmask ?? cur.netmask ?? '',
        gateway: s.gateway ?? '',
        dns: s.dns ?? ''
      }
    })
  } finally {
    netLoading.value = false
  }
}

async function saveNetwork() {
  try {
    await proxy.$modal.confirm('确认保存网络配置？错误的 IP 配置可能导致网关管理页面无法访问。')
  } catch (e) { return }
  netSaving.value = true
  try {
    const payload = {}
    nics.value.forEach(nic => { payload[nic.name] = netForm[nic.name] })
    await saveNetworkConfig(JSON.stringify(payload))
    proxy.$modal.msgSuccess('网络配置已保存')
  } catch (e) {
    proxy.$modal.msgError('保存失败')
  } finally {
    netSaving.value = false
  }
}

// ==================== 核心参数 ====================
const coreLoading = ref(false)
const coreSaving = ref(false)
const coreForm = reactive({})
const coreNumbers = reactive({ preservedDays: 30, offlineThreshold: 300 })
const coreSwitches = reactive({ autorestart: true, autosync: false })

async function loadCore() {
  coreLoading.value = true
  try {
    const res = await getCoreParams()
    Object.assign(coreForm, res.data || {})
    coreNumbers.preservedDays = Number(coreForm['data_preserved_day_count'] || 30)
    coreNumbers.offlineThreshold = Number(coreForm['gateway.offline.threshold'] || 300)
    coreSwitches.autorestart = String(coreForm['gateway.plugin.autorestart']) !== 'false'
    coreSwitches.autosync = String(coreForm['gateway.device.autosync']) === 'true'
  } finally {
    coreLoading.value = false
  }
}

async function saveCore() {
  coreSaving.value = true
  try {
    await saveCoreParams({
      'gateway.name': coreForm['gateway.name'] || '',
      'gateway.code': coreForm['gateway.code'] || '',
      'data_preserved_day_count': String(coreNumbers.preservedDays),
      'gateway.offline.threshold': String(coreNumbers.offlineThreshold),
      'gateway.plugin.autorestart': String(coreSwitches.autorestart),
      'gateway.device.autosync': String(coreSwitches.autosync)
    })
    proxy.$modal.msgSuccess('核心参数已保存')
  } catch (e) {
    proxy.$modal.msgError('保存失败')
  } finally {
    coreSaving.value = false
  }
}

// ==================== 上级平台：推送通道 ====================
const upstreamLoading = ref(false)
const gatewayPlugin = ref(null)
const gatewayAlive = ref(false)
const gatewaySwitchLoading = ref(false)
const gatewayConfigOpen = ref(false)
const gatewaySaving = ref(false)
const gatewayForm = reactive({ pushType: 'mq', pushUrls: '', queueName: '', ip: '', port: null, vhost: '', db: '' })

const PUSH_TYPE_META = {
  mq: { label: 'MQ 消息队列', tag: 'primary' },
  url: { label: 'HTTP URL 推送', tag: 'success' },
  redis: { label: 'Redis 队列', tag: 'warning' }
}
const gatewayConfigObject = computed(() => {
  const cfg = gatewayPlugin.value?.config
  if (!cfg) return {}
  try { return JSON.parse(cfg) } catch { return {} }
})
const pushTypeLabel = computed(() => PUSH_TYPE_META[gatewayConfigObject.value.pushType || 'mq']?.label || gatewayConfigObject.value.pushType)
const pushTypeTag = computed(() => PUSH_TYPE_META[gatewayConfigObject.value.pushType || 'mq']?.tag || 'info')
const gatewayTargetSummary = computed(() => {
  const c = gatewayConfigObject.value
  const type = c.pushType || 'mq'
  if (type === 'url') return (c.pushUrls || c.pushUrl || '(未配置推送URL)').toString().replace(/\n/g, ' ; ')
  if (type === 'mq') return `RabbitMQ ${c.ip || gatewayPlugin.value?.ip || '-'}:${c.port || gatewayPlugin.value?.port || '-'} vhost=${c.vhost || '/'} queue=${c.queueName || 'za_monitor'}`
  return `Redis ${c.ip || '127.0.0.1'}:${c.port || 6379} db=${c.db || 6} list=gateway_queue`
})

async function loadUpstream() {
  upstreamLoading.value = true
  try {
    const [pfRes, statsRes, cascadeRes] = await Promise.all([
      selectPlatform(),
      getAllPlatformStats().catch(() => ({ data: [] })),
      listCascade({ pageNum: 1, pageSize: 100 }).catch(() => ({ rows: [] }))
    ])
    const list = pfRes.data || []
    gatewayPlugin.value = list.find(p => p.code === 'gateway') || null
    const stats = (statsRes.data || []).find(s => s.platformCode === 'gateway')
    gatewayAlive.value = Boolean(stats?.alive)
    cascadeList.value = cascadeRes.rows || []
  } finally {
    upstreamLoading.value = false
  }
}

async function toggleGatewayPlugin() {
  const row = gatewayPlugin.value
  const text = row.status === '1' ? '启用' : '禁用'
  try {
    await proxy.$modal.confirm(`确认${text}消息推送通道？${text === '禁用' ? '禁用后接入消息将不再同步至上级平台。' : ''}`)
    gatewaySwitchLoading.value = true
    await updatePlatform({ id: row.id, code: row.code, status: row.status })
    proxy.$modal.msgSuccess(text + '成功')
    await loadUpstream()
  } catch (e) {
    row.status = row.status === '1' ? '0' : '1'
  } finally {
    gatewaySwitchLoading.value = false
  }
}

function openGatewayConfig() {
  const c = gatewayConfigObject.value
  gatewayForm.pushType = c.pushType || 'mq'
  gatewayForm.pushUrls = c.pushUrls || c.pushUrl || ''
  gatewayForm.queueName = c.queueName || ''
  gatewayForm.ip = c.ip || ''
  gatewayForm.port = c.port ? Number(c.port) : null
  gatewayForm.vhost = c.vhost || ''
  gatewayForm.db = c.db || ''
  gatewayConfigOpen.value = true
}

async function saveGatewayConfig() {
  const row = gatewayPlugin.value
  if (!row) return
  gatewaySaving.value = true
  try {
    const cfg = { ...gatewayConfigObject.value, pushType: gatewayForm.pushType }
    if (gatewayForm.pushType === 'url') {
      cfg.pushUrls = gatewayForm.pushUrls
      cfg.pushUrl = (gatewayForm.pushUrls || '').split(/[\n,]/).map(s => s.trim()).filter(Boolean)[0] || ''
    }
    if (gatewayForm.pushType === 'mq') {
      cfg.ip = gatewayForm.ip
      cfg.port = gatewayForm.port
      cfg.vhost = gatewayForm.vhost
      cfg.queueName = gatewayForm.queueName
    }
    if (gatewayForm.pushType === 'redis') {
      cfg.ip = gatewayForm.ip
      cfg.port = gatewayForm.port
      cfg.db = gatewayForm.db
    }
    await updatePlatform({ id: row.id, code: row.code, config: JSON.stringify(cfg) })
    proxy.$modal.msgSuccess('推送通道配置已保存并生效')
    gatewayConfigOpen.value = false
    await loadUpstream()
  } catch (e) {
    proxy.$modal.msgError('保存失败')
  } finally {
    gatewaySaving.value = false
  }
}

// ==================== 上级平台：级联 ====================
const cascadeList = ref([])
const cascadeOpen = ref(false)
const cascadeSaving = ref(false)
const cascadeForm = ref({})

function openCascadeDialog(row) {
  cascadeForm.value = row
    ? { id: row.id, name: row.name, code: row.code, ip: row.ip, status: row.status || '0' }
    : { id: null, name: '', code: '', ip: '', status: '1' }
  cascadeOpen.value = true
}

async function saveCascade() {
  try {
    await proxy.$refs['cascadeFormRef'].validate()
  } catch (e) { return }
  cascadeSaving.value = true
  try {
    if (cascadeForm.value.id) {
      await updateCascade(cascadeForm.value)
    } else {
      await addCascade(cascadeForm.value)
    }
    proxy.$modal.msgSuccess('保存成功')
    cascadeOpen.value = false
    await loadUpstream()
  } catch (e) {
    proxy.$modal.msgError('保存失败')
  } finally {
    cascadeSaving.value = false
  }
}

async function toggleCascade(row) {
  try {
    await updateCascade({ id: row.id, code: row.code, status: row.status })
    proxy.$modal.msgSuccess(row.status === '1' ? '已启用' : '已停用')
  } catch (e) {
    row.status = row.status === '1' ? '0' : '1'
  }
}

function deleteCascade(row) {
  proxy.$modal.confirm(`确认删除级联平台「${row.name}」？`)
    .then(() => delCascade(row.id))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      loadUpstream()
    })
    .catch(() => {})
}

// ==================== 初始化 ====================
loadNetwork()
loadCore()
loadUpstream()
</script>

<style scoped>
.settings-tabs { padding: 0 18px; }
.settings-tabs :deep(.el-tabs__content) { padding-bottom: 16px; }
.tab-body { padding-top: 4px; }
.tab-foot {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.nic-card { margin-bottom: 14px; }
.mb14 { margin-bottom: 14px; }
.mt12 { margin-top: 12px; }
</style>
