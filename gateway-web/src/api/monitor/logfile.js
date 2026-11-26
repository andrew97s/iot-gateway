import request from '@/utils/request'
import { ElLoading, ElMessage } from 'element-plus'
import { blobValidate } from '@/utils/ruoyi'
import { saveAs } from 'file-saver'
import errorCode from '@/utils/errorCode'

export function listLogFiles(query) {
    return request({
        url: '/monitor/logfile/list',
        method: 'get',
        params: query,
        headers: { timeout: 60000 }
    })
}

export function currentLogFile() {
    return request({
        url: '/monitor/logfile/current',
        method: 'get'
    })
}

export function readLogFile(query) {
    return request({
        url: '/monitor/logfile/content',
        method: 'get',
        params: query,
        headers: { timeout: 60000 }
    })
}

export function downloadLogFile(fileName) {
    return downloadBlob('/monitor/logfile/download', { params: { fileName } }, fileName)
}

export function downloadLogFiles(fileNames) {
    const filename = fileNames.length === 1 ? fileNames[0] : 'gateway-logs.zip'
    return downloadBlob('/monitor/logfile/download', { method: 'post', data: fileNames }, filename)
}

function downloadBlob(url, config, filename) {
    const loading = ElLoading.service({ text: '正在下载日志，请稍候', background: 'rgba(0, 0, 0, 0.7)' })
    return request({
        url,
        method: 'get',
        responseType: 'blob',
        headers: { timeout: 120000 },
        ...config
    })
        .then(async (data) => {
            const isBlob = blobValidate(data)
            if (isBlob) {
                saveAs(new Blob([data]), filename)
            } else {
                const resText = await data.text()
                const rspObj = JSON.parse(resText)
                ElMessage.error(errorCode[rspObj.code] || rspObj.msg || errorCode['default'])
            }
            loading.close()
        })
        .catch(() => {
            ElMessage.error('下载日志失败')
            loading.close()
        })
}