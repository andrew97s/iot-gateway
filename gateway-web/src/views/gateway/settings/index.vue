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
            <!-- 上级平台列表（多平台） -->
            <div class="gw-card mb14">
              <div class="gw-card-head">
                <h2>上级平台（消息推送）</h2>
                <el-button size="small" type="primary" icon="Plus" v-hasPermi="['sys:upstream:add']" @click="openUpstreamDialog()">新增上级平台</el-button>
              </div>
              <div class="gw-card-body no-pad">
                <el-table :data="upstreamList" size="default">
                  <el-table-column label="平台名称" prop="name" min-width="130" />
                  <el-table-column label="平台代码" prop="code" width="130" align="center">
                    <template #default="scope"><span class="gw-mono">{{ scope.row.code }}</span></template>
                  </el-table-column>
                  <el-table-column label="推送方式" width="110" align="center">
                    <template #default="scope">
                      <el-tag size="small" :type="PUSH_TYPE_META[scope.row.pushType]?.tag || 'info'" effect="plain">
                        {{ PUSH_TYPE_META[scope.row.pushType]?.label || scope.row.pushType }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="目标地址" min-width="220">
                    <template #default="scope">
                      <span class="gw-mono gw-small">{{ upstreamTarget(scope.row) }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="通道状态" width="100" align="center">
                    <template #default="scope">
                      <span v-if="scope.row.status !== '1'" class="gw-badge off">已停用</span>
                      <span v-else class="gw-badge" :class="upstreamAliveMap[scope.row.code] ? 'ok' : 'err'">
                        {{ upstreamAliveMap[scope.row.code] ? '在线' : '断开' }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="启用" width="80" align="center">
                    <template #default="scope">
                      <el-switch
                        v-model="scope.row.status"
                        active-value="1"
                        inactive-value="0"
                        v-hasPermi="['sys:upstream:edit']"
                        @change="toggleUpstream(scope.row)"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="170" align="center">
                    <template #default="scope">
                      <el-button link type="primary" v-hasPermi="['sys:upstream:edit']" @click="openUpstreamDialog(scope.row)">编辑</el-button>
                      <el-button link type="primary" :loading="testingMap[scope.row.id]" v-hasPermi="['sys:upstream:edit']" @click="handleTestUpstream(scope.row)">测试</el-button>
                      <el-button link type="danger" v-hasPermi="['sys:upstream:remove']" @click="deleteUpstream(scope.row)">删除</el-button>
                    </template>
                  </el-table-column>
                  <template #empty>
                    <el-empty description="暂无上级平台，新增后接入消息将统一格式化并同步推送" :image-size="70" />
                  </template>
                </el-table>
              </div>
              <div class="gw-card-body" style="border-top: 1px solid #f1f5f9">
                <span class="gw-muted gw-small">
                  支持同时对接多个上级平台；推送方式支持 HTTP/HTTPS 直推、Redis 队列、RabbitMQ 消息队列。
                  消息按统一格式（含全局唯一 messageId，上级按其去重保证幂等）推送，失败自动标记并支持重推；配置保存后热生效。
                </span>
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

    <!-- 上级平台编辑对话框 -->
    <el-dialog v-model="upstreamOpen" :title="upstreamForm.id ? '编辑上级平台' : '新增上级平台'" width="720px" append-to-body>
      <el-form :model="upstreamForm" label-width="110px" ref="upstreamFormRef" class="dialog-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="平台名称" prop="name" :rules="[{ required: true, message: '请输入平台名称' }]">
              <el-input v-model="upstreamForm.name" placeholder="如：市级物联平台" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平台代码" prop="code" :rules="[{ required: true, message: '请输入平台代码' }]">
              <el-input v-model="upstreamForm.code" placeholder="唯一代码，如 city-pf" :disabled="!!upstreamForm.id" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="推送方式" prop="pushType">
          <el-select v-model="upstreamForm.pushType" style="width: 100%">
            <el-option label="HTTP / HTTPS 直接推送" value="url" />
            <el-option label="Redis 队列推送" value="redis" />
            <el-option label="RabbitMQ 消息队列" value="mq" />
          </el-select>
          <div class="gw-muted gw-small">切换后下方参数动态变化；保存后推送通道热重载生效</div>
        </el-form-item>

        <template v-if="upstreamForm.pushType === 'url'">
          <el-form-item label="推送地址列表">
            <el-input v-model="upstreamCfg.pushUrls" type="textarea" :rows="4" placeholder="每行一个 URL，或英文逗号分隔；网关将统一消息 JSON 以 POST 推送" />
          </el-form-item>
        </template>

        <template v-if="upstreamForm.pushType === 'mq'">
          <el-row :gutter="16">
            <el-col :span="14">
              <el-form-item label="MQ 地址">
                <el-input v-model="upstreamCfg.ip" placeholder="RabbitMQ 主机地址" />
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label="端口">
                <el-input-number v-model="upstreamCfg.port" :min="1" :max="65535" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="用户名">
                <el-input v-model="upstreamCfg.username" placeholder="用户名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="密码">
                <el-input v-model="upstreamCfg.password" type="password" show-password placeholder="密码" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="vhost">
                <el-input v-model="upstreamCfg.vhost" placeholder="默认 /" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="队列名">
                <el-input v-model="upstreamCfg.queue" placeholder="默认 za_monitor" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="交换机">
                <el-input v-model="upstreamCfg.exchange" placeholder="默认 za" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="路由键">
                <el-input v-model="upstreamCfg.key" placeholder="默认 za" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-if="upstreamForm.pushType === 'redis'">
          <el-row :gutter="16">
            <el-col :span="14">
              <el-form-item label="Redis 地址">
                <el-input v-model="upstreamCfg.ip" placeholder="默认 127.0.0.1" />
              </el-form-item>
            </el-col>
            <el-col :span="10">
              <el-form-item label="端口">
                <el-input-number v-model="upstreamCfg.port" :min="1" :max="65535" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="密码">
                <el-input v-model="upstreamCfg.password" type="password" show-password placeholder="密码" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="库编号（db）">
                <el-input v-model="upstreamCfg.db" placeholder="默认 6" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-form-item label="启用">
          <el-switch v-model="upstreamForm.status" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="upstreamForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :loading="upstreamTesting" @click="testUpstreamForm">测试连接</el-button>
        <el-button @click="upstreamOpen = false">取 消</el-button>
        <el-button type="primary" :loading="upstreamSaving" @click="saveUpstream">保存并生效</el-button>
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
import {
  selectUpstream, addUpstream, updateUpstream, delUpstream, testUpstream, getUpstreamStatus
} from '@/api/sys/upstream'
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

// ==================== 上级平台（多平台） ====================
const upstreamLoading = ref(false)
const upstreamList = ref([])
const upstreamAliveMap = ref({})
const upstreamOpen = ref(false)
const upstreamSaving = ref(false)
const upstreamTesting = ref(false)
const testingMap = reactive({})
const upstreamForm = ref({})
const upstreamCfg = ref({})

const PUSH_TYPE_META = {
  url: { label: 'HTTP直推', tag: 'success' },
  redis: { label: 'Redis队列', tag: 'warning' },
  mq: { label: 'RabbitMQ', tag: 'primary' }
}

function parseCfg(row) {
  if (!row?.config) return {}
  try { return JSON.parse(row.config) } catch { return {} }
}

function upstreamTarget(row) {
  const c = parseCfg(row)
  if (row.pushType === 'url') {
    return (c.pushUrls || c.pushUrl || '(未配置URL)').toString().replace(/\n/g, ' ; ')
  }
  if (row.pushType === 'mq') {
    return `${c.ip || '-'}:${c.port || 5672} vhost=${c.vhost || '/'} queue=${c.queue || 'za_monitor'}`
  }
  if (row.pushType === 'redis') {
    return `${c.ip || '127.0.0.1'}:${c.port || 6379} db=${c.db || 6}`
  }
  return '-'
}

async function loadUpstream() {
  upstreamLoading.value = true
  try {
    const [upRes, statusRes, cascadeRes] = await Promise.all([
      selectUpstream(),
      getUpstreamStatus().catch(() => ({ data: [] })),
      listCascade({ pageNum: 1, pageSize: 100 }).catch(() => ({ rows: [] }))
    ])
    upstreamList.value = upRes.data || []
    const aliveMap = {}
    ;(statusRes.data || []).forEach(s => { aliveMap[s.code] = s.alive })
    upstreamAliveMap.value = aliveMap
    cascadeList.value = cascadeRes.rows || []
  } finally {
    upstreamLoading.value = false
  }
}

function openUpstreamDialog(row) {
  if (row) {
    upstreamForm.value = { id: row.id, name: row.name, code: row.code, pushType: row.pushType, status: row.status || '1', remark: row.remark }
    upstreamCfg.value = { vhost: '/', ...parseCfg(row) }
  } else {
    upstreamForm.value = { id: null, name: '', code: '', pushType: 'url', status: '1', remark: '' }
    upstreamCfg.value = { pushUrls: '', ip: '', port: null, username: '', password: '', vhost: '/', queue: 'za_monitor', exchange: 'za', key: 'za', db: '6' }
  }
  upstreamOpen.value = true
}

function buildUpstreamPayload() {
  const c = upstreamCfg.value
  let cfg = {}
  if (upstreamForm.value.pushType === 'url') {
    cfg = { pushUrls: c.pushUrls || '' }
  } else if (upstreamForm.value.pushType === 'mq') {
    cfg = { ip: c.ip, port: c.port, username: c.username, password: c.password, vhost: c.vhost || '/', queue: c.queue || 'za_monitor', exchange: c.exchange || 'za', key: c.key || 'za' }
  } else if (upstreamForm.value.pushType === 'redis') {
    cfg = { ip: c.ip || '127.0.0.1', port: String(c.port || 6379), password: c.password, db: String(c.db || 6) }
  }
  return { ...upstreamForm.value, config: JSON.stringify(cfg) }
}

async function saveUpstream() {
  try {
    await proxy.$refs['upstreamFormRef'].validate()
  } catch (e) { return }
  upstreamSaving.value = true
  try {
    const payload = buildUpstreamPayload()
    if (payload.id) {
      await updateUpstream(payload)
    } else {
      await addUpstream(payload)
    }
    proxy.$modal.msgSuccess('保存成功，推送通道已热重载')
    upstreamOpen.value = false
    await loadUpstream()
  } finally {
    upstreamSaving.value = false
  }
}

async function toggleUpstream(row) {
  try {
    await updateUpstream({ id: row.id, code: row.code, status: row.status })
    proxy.$modal.msgSuccess(row.status === '1' ? '已启用，推送通道已重载' : '已停用')
    await loadUpstream()
  } catch (e) {
    row.status = row.status === '1' ? '0' : '1'
  }
}

async function handleTestUpstream(row) {
  testingMap[row.id] = true
  try {
    const res = await testUpstream(row)
    proxy.$modal.msgSuccess(res.msg || '连接成功')
  } catch (e) {
    // 错误提示由 request 拦截器统一弹出
  } finally {
    testingMap[row.id] = false
  }
}

async function testUpstreamForm() {
  upstreamTesting.value = true
  try {
    const res = await testUpstream(buildUpstreamPayload())
    proxy.$modal.msgSuccess(res.msg || '连接成功')
  } catch (e) {
  } finally {
    upstreamTesting.value = false
  }
}

function deleteUpstream(row) {
  proxy.$modal.confirm(`确认删除上级平台「${row.name}」？删除后消息不再同步至该平台。`)
    .then(() => delUpstream(row.id))
    .then(() => {
      proxy.$modal.msgSuccess('删除成功')
      loadUpstream()
    })
    .catch(() => {})
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
.dialog-form :deep(.el-form-item__content) { min-width: 0; }
.dialog-form :deep(.el-col .el-form-item) { width: 100%; }
</style>
