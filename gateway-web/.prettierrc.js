/**
 * @type {import('prettier').Options}
 */
export default {
  // 单行长度
  printWidth: 150,
  // 缩进长度
  tabWidth: 2,
  // 句末使用分号
  semi: false,
  // 使用单引号
  singleQuote: true,
  // 多行时尽可能打印尾随逗号
  trailingComma: 'none',
  // 在对象前后添加空格-eg: { foo: bar }
  bracketSpacing: true,
  // 单参数箭头函数参数周围使用圆括号-eg: (x) => x
  arrowParens: 'always',
  // 结尾符-auto:自动根据文件内容判断
  endOfLine: 'auto'
}
