import vue from '@vitejs/plugin-vue'

import createAutoImport from './auto-import'
import createSvgIcon from './svg-icon'
import createCompression from './compression'
import createSetupExtend from './setup-extend'
import getProxyTarget from './get-proxy-target'
import createVersionPlugin from './version-plugin'

export default function createVitePlugins(viteEnv, isBuild = false) {
  const vitePlugins = [vue()]
  vitePlugins.push(createAutoImport())
  vitePlugins.push(createSvgIcon(isBuild))
  if (isBuild) {
    vitePlugins.push(createSetupExtend())
    vitePlugins.push(...createCompression(viteEnv))
    vitePlugins.push(createVersionPlugin())
  } else {
    vitePlugins.push(getProxyTarget())
  }
  return vitePlugins
}
