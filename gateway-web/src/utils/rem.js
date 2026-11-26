function setFont() {
  var html = document.documentElement
  var k = 1920
  html.style.fontSize = (html.clientWidth / k) * 100 + 'px'
}

//Rem
export function setFontSize() {
  if (!window.addEventListener) return
  setFont()
  setTimeout(function () {
    setFont()
  }, 300)
  document.addEventListener('DOMContentLoaded', setFont, false)
  window.addEventListener('resize', setFont, false)
  window.addEventListener('load', setFont, false)
}

export function removeFontResize() {
  document.removeEventListener('DOMContentLoaded', setFont, false)
  window.removeEventListener('resize', setFont, false)
  window.removeEventListener('load', setFont, false)
  document.documentElement.style.fontSize = 'initial'
}
