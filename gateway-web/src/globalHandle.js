document.addEventListener('focusin', function (e) {
  // input框 type='text'
  //e.target.getAttribute('maxlength') === null，本身没有设置maxlength长度，防止全局设置覆盖所在页面设置的长度
  if (e.target.type === 'text' && e.target.getAttribute('maxlength') === null) {
    e.target.setAttribute('maxlength', 150)
  }
  // input框 type='textarea'，且本身没有设置maxlength长度
  if (e.target.type === 'textarea' && e.target.getAttribute('maxlength') === null) {
    e.target.setAttribute('maxlength', 350)
  }
})

// toFixed 重写解决四舍五入bug
Number.prototype.toFixedDefault = Number.prototype.toFixed
Number.prototype.toFixed = function (length) {
  const num = this
  const numStr = String(num)
  const dot = numStr.includes('.')
  let result = numStr
  // 有小数位则使用自定处理，否则使用原生方法
  if (dot) {
    // 整数位
    const int = numStr.split('.')[0]
    // 小数位
    const decimal = numStr.split('.')[1]
    let zeroStr = ''
    if (decimal.length < length) {
      for (let i = 0; i < length - decimal.length; i++) {
        zeroStr += '0'
      }
    }
    result = int + '.' + decimal.slice(0, length) + zeroStr
  } else {
    result = num.toFixedDefault(length)
  }

  return result
}
