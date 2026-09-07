import fs from 'fs'
import path from 'path'
import { execSync } from 'child_process'

// 更可靠的显示宽度估算（简化的 wcwidth 实现）
function charWidth(code) {
  // 控制字符
  if (code === 0) return 0
  if (code < 32 || (code >= 0x7f && code < 0xa0)) return 0

  // 常见的全宽/宽字符区间（CJK、表情等）
  const ranges = [
    [0x1100, 0x115f],
    [0x2329, 0x232a],
    [0x2e80, 0xa4cf],
    [0xac00, 0xd7a3],
    [0xf900, 0xfaff],
    [0xfe10, 0xfe19],
    [0xfe30, 0xfe6f],
    [0xff00, 0xff60],
    [0xffe0, 0xffe6],
    [0x20000, 0x2fffd],
    [0x30000, 0x3fffd]
  ]

  for (const [start, end] of ranges) {
    if (code >= start && code <= end) return 2
  }

  return 1
}

function displayLength(str) {
  let len = 0
  for (const ch of [...String(str)]) {
    len += charWidth(ch.codePointAt(0))
  }
  return len
}

function truncateToWidth(str, maxWidth) {
  let out = ''
  let cur = 0
  for (const ch of [...String(str)]) {
    const w = charWidth(ch.codePointAt(0))
    if (cur + w > maxWidth - 1) break // 为省略号保留至少1列
    out += ch
    cur += w
  }
  return out
}

function padText(text, innerWidth) {
  let t = String(text)
  let tLen = displayLength(t)
  if (tLen > innerWidth) {
    t = truncateToWidth(t, innerWidth) + '…'
    tLen = displayLength(t)
  }
  const totalSpaces = innerWidth - tLen
  const leftSpaces = Math.floor(totalSpaces / 2)
  const rightSpaces = totalSpaces - leftSpaces
  return '║' + ' '.repeat(leftSpaces) + t + ' '.repeat(rightSpaces) + '║'
}
export default function createVersionPlugin() {
  return {
    name: 'vite-plugin-version',
    // 使用closeBundle钩子，这是构建过程的最后一步
    // 添加延迟以确保所有压缩文件都已完全写入
    closeBundle: async () => {
      try {
        const versionPath = path.resolve(process.cwd(), 'version.json')
        const webVersionPath = path.resolve(process.cwd(), 'web', 'version.json')

        if (fs.existsSync(versionPath)) {
          const versionContent = fs.readFileSync(versionPath, 'utf-8')
          const { version, projectVersion } = JSON.parse(versionContent)

          // 确保web目录存在
          const webDir = path.resolve(process.cwd(), 'web')
          if (!fs.existsSync(webDir)) {
            fs.mkdirSync(webDir, { recursive: true })
          }

          // 复制版本文件到web目录
          fs.writeFileSync(webVersionPath, versionContent, 'utf-8')

          // 根据git仓库名决定使用哪个版本号
          let zipVersion
          try {
            // 获取当前git仓库的远程URL
            const gitRemote = execSync('git remote get-url origin', { encoding: 'utf8', cwd: process.cwd() }).trim()
            // 检查是否包含指定产品仓库
            const productRepos = ['fire-base.git', 'fire-getway.git', 'fire-robot.git', 'fire-robot.git', 'fire-pension.git']
            if (productRepos.some((repo) => gitRemote.includes(repo))) {
              zipVersion = version
            } else {
              zipVersion = projectVersion
            }
          } catch (error) {
            // 如果获取git信息失败，默认使用version
            console.warn('获取git仓库信息失败，使用默认版本:', error.message)
            zipVersion = projectVersion
          }

          const zipFileName = `web_v${zipVersion}.zip`

          try {
            // 添加延迟以确保所有压缩文件都已完全写入
            await new Promise((resolve) => setTimeout(resolve, 2000))

            // 压缩web目录，确保解压后是web目录
            if (process.platform === 'win32') {
              // Windows系统使用PowerShell命令，使用完整路径
              const webDir = path.resolve(process.cwd(), 'web')
              const zipPath = path.resolve(process.cwd(), zipFileName)
              const command = `powershell -Command "Compress-Archive -Path '${webDir}' -DestinationPath '${zipPath}' -Force"`
              execSync(command, {
                stdio: 'pipe',
                shell: true
              })
            } else {
              // Linux/Mac系统使用zip命令
              execSync(`zip -r '${zipFileName}' 'web'`, { stdio: 'inherit' })
            }

            console.log(`\x1b[32m✓ 构建压缩包: ${zipFileName}\x1b[0m`)
          } catch (err) {
            console.warn('\x1b[33m警告: 创建压缩文件失败，请确保系统支持压缩命令:\x1b[0m', err.message)
          }

          // 使用矩形框显示版本信息（更紧凑，支持文本截断避免错位）
          const boxWidth = 40 // 总宽度（含两侧边框）
          const innerWidth = boxWidth - 2 // 可用文本宽度

          // 打印更紧凑的框（减少垂直空白行）
          console.log('\n\x1b[32m╔' + '═'.repeat(innerWidth) + '╗')
          console.log(padText('✓ 构建完成', innerWidth))
          console.log(padText(`基线版本: ${version}`, innerWidth))
          console.log(padText(`项目版本: ${projectVersion}`, innerWidth))
          console.log('╚' + '═'.repeat(innerWidth) + '╝\x1b[0m\n')
        }
      } catch (error) {
        console.error('处理版本信息时出错:', error)
      }
    }
  }
}
