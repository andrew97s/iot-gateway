/* 原型交互演示脚本：页签切换、弹窗/抽屉开关（仅用于原型演示，非业务实现） */

function openModal(id) { document.getElementById(id).classList.add('show'); }
function closeModal(id) { document.getElementById(id).classList.remove('show'); }

function openDrawer(id) {
  document.getElementById(id).classList.add('show');
  var mask = document.getElementById(id + '-mask');
  if (mask) mask.classList.add('show');
}
function closeDrawer(id) {
  document.getElementById(id).classList.remove('show');
  var mask = document.getElementById(id + '-mask');
  if (mask) mask.classList.remove('show');
}

/* 页签：.tabs 内 .tab 带 data-pane，与同级 .tab-pane 的 id 对应 */
document.addEventListener('click', function (e) {
  var tab = e.target.closest('.tab[data-pane]');
  if (!tab) return;
  var tabs = tab.closest('.tabs');
  tabs.querySelectorAll('.tab').forEach(function (t) { t.classList.remove('active'); });
  tab.classList.add('active');
  var scope = tabs.dataset.scope ? document.getElementById(tabs.dataset.scope) : document;
  scope.querySelectorAll('.tab-pane').forEach(function (p) { p.classList.remove('active'); });
  var pane = document.getElementById(tab.dataset.pane);
  if (pane) pane.classList.add('active');
});

/* 点击遮罩关闭弹窗 */
document.addEventListener('click', function (e) {
  if (e.target.classList.contains('modal-mask')) e.target.classList.remove('show');
});

/* 推送方式动态表单切换（系统配置-上级平台） */
function switchPushType(sel) {
  var wrap = sel.closest('.modal-body') || document;
  wrap.querySelectorAll('.push-fields').forEach(function (p) { p.style.display = 'none'; });
  var target = wrap.querySelector('.push-fields[data-type="' + sel.value + '"]');
  if (target) target.style.display = '';
}
