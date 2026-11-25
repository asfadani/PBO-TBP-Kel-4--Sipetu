let currentRole = null;
let sekrRefreshTimer = null;

async function api(path, opts = {}) {
  const def = { headers: { 'Content-Type': 'application/json' }, credentials: 'include' };
  return fetch(path, { ...def, ...opts });
}

function $set(id, html) { document.getElementById(id).innerHTML = html; }

function esc(str) {
  if (!str) return '';
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function fmtRemaining(deadline) {
  if (!deadline) return '-';
  const d1 = new Date(); d1.setHours(0, 0, 0, 0);
  const d2 = new Date(deadline + 'T00:00:00');
  const diff = Math.round((d2 - d1) / 86400000);
  if (diff < 0) return Math.abs(diff) + ' hari lewat';
  if (diff === 0) return 'Hari ini';
  return diff + ' hari lagi';
}

async function login() {
  const u = document.getElementById('username').value.trim();
  const p = document.getElementById('password').value;
  document.getElementById('login-msg').textContent = '';
  const res = await api('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username: u, password: p })
  });
  if (res.ok) {
    const d = await res.json();
    currentRole = d.role;
    document.getElementById('login-page').classList.add('hidden');
    document.getElementById('who').textContent = d.username + ' (' + currentRole + ')';
    if (currentRole === 'ADMIN') {
      document.getElementById('app-page').classList.remove('hidden');
      await renderReminders();
      loadDashboard();
    } else {
      const kelasRes = await api('/api/kelas/saya');
      const kelasList = kelasRes.ok ? await kelasRes.json() : [];
      if (kelasList && kelasList.length > 0) {
        const info = kelasList.map(k => k.nama + ' (' + k.kodeEnroll + ')').join(', ');
        document.getElementById('who').textContent += ' — Kelas: ' + info;
        document.getElementById('app-page').classList.remove('hidden');
        await renderReminders();
        loadDashboard();
      } else {
        document.getElementById('enroll-code').value = '';
        document.getElementById('enroll-msg').textContent = '';
        document.getElementById('enroll-page').classList.remove('hidden');
      }
    }
  } else {
    const err = await res.json().catch(() => ({}));
    document.getElementById('login-msg').textContent = err.error || 'Login gagal';
  }
}

async function submitEnroll() {
  const kodeEnroll = document.getElementById('enroll-code').value.trim();
  if (!kodeEnroll) {
    document.getElementById('enroll-msg').textContent = 'Kode enrol wajib diisi';
    return;
  }
  const res = await api('/api/enroll', {
    method: 'POST',
    body: JSON.stringify({ kodeEnroll })
  });
  if (res.ok || res.status === 409) {
    document.getElementById('enroll-page').classList.add('hidden');
    document.getElementById('app-page').classList.remove('hidden');
    const kelasRes = await api('/api/kelas/saya');
    if (kelasRes.ok) {
      const kelasList = await kelasRes.json();
      if (kelasList && kelasList.length > 0) {
        const info = kelasList.map(k => k.nama + ' (' + k.kodeEnroll + ')').join(', ');
        document.getElementById('who').textContent += ' — Kelas: ' + info;
      }
    }
    await renderReminders();
    loadDashboard();
  } else {
    const err = await res.json().catch(() => ({}));
    document.getElementById('enroll-msg').textContent = err.error || 'Kode enroll tidak valid';
  }
}

async function logout() {
  if (sekrRefreshTimer) clearInterval(sekrRefreshTimer);
  await api('/api/auth/logout', { method: 'POST' });
  location.reload();
}

async function renderReminders() {
  const [upRes, ovRes] = await Promise.all([api('/api/reminders/upcoming'), api('/api/reminders/overdue')]);
  const up = upRes.ok ? await upRes.json() : [];
  const ov = ovRes.ok ? await ovRes.json() : [];
  const banner = document.getElementById('reminder-banner');
  if (up.length === 0 && ov.length === 0) {
    banner.classList.add('hidden');
    return;
  }
  let html = '';
  if (ov.length > 0) {
    html += '<div class="reminder" style="border-color:#991b1b"><strong>⚠️ Tugas melewati deadline:</strong><ul>' +
      ov.map(t => '<li><strong>' + esc(t.title) + '</strong> (' + esc(t.subject) + ') — ' + fmtRemaining(t.deadline) + '</li>').join('') +
      '</ul></div>';
  }
  if (up.length > 0) {
    html += '<div class="reminder"><strong>🔔 Pengingat deadline mendekat (≤ 3 hari):</strong><ul>' +
      up.map(t => '<li><strong>' + esc(t.title) + '</strong> (' + esc(t.subject) + ') — ' + fmtRemaining(t.deadline) + '</li>').join('') +
      '</ul></div>';
  }
  banner.innerHTML = html;
  banner.classList.remove('hidden');
}

async function loadDashboard() {
  if (currentRole === 'ADMIN') { $set('view', adminHtml()); await adminInit(); }
  else if (currentRole === 'SEKRETARIS') { $set('view', sekretarisHtml()); await sekretarisInit(); }
  else { $set('view', pelajarHtml()); await pelajarInit(); }
}

// ==================== ADMIN ====================
function adminHtml() {
  return '<div class="panel">' +
    '<div class="card">' +
      '<h3>Kelas</h3>' +
      '<div class="form-row">' +
        '<input id="kelas-nama" placeholder="Nama kelas">' +
        '<input id="kelas-desc" placeholder="Deskripsi (opsional)">' +
        '<button class="btn btn-primary" onclick="adminCreateKelas()">Buat Kelas</button>' +
      '</div>' +
      '<ul id="kelas-list"></ul>' +
    '</div>' +
    '<div class="card">' +
      '<h3>Pengguna</h3>' +
      '<div class="form-row">' +
        '<input id="user-u" placeholder="Username">' +
        '<input id="user-p" type="password" placeholder="Password">' +
        '<select id="user-r"><option>ADMIN</option><option>SEKRETARIS</option><option>PELAJAR</option></select>' +
        '<button class="btn btn-primary" onclick="adminCreateUser()">Tambah</button>' +
      '</div>' +
      '<ul id="user-list"></ul>' +
    '</div>' +
    '<div class="card" style="grid-column: 1 / -1">' +
      '<h3>Bidang Studi</h3>' +
      '<div class="form-row">' +
        '<input id="sub-nama" placeholder="Nama bidang studi">' +
        '<input id="sub-kode" placeholder="Kode">' +
        '<select id="sub-kelas"><option value="">Pilih kelas</option></select>' +
        '<button class="btn btn-primary" onclick="adminCreateSubj()">Tambah</button>' +
      '</div>' +
      '<ul id="sub-list"></ul>' +
    '</div>' +
  '</div>';
}

async function adminInit() {
  const [kelas, users, subs] = await Promise.all([
    api('/api/admin/classes').then(r => r.json()),
    api('/api/admin/users').then(r => r.json()),
    api('/api/admin/subjects').then(r => r.json())
  ]);
  const kelasMap = Object.fromEntries(kelas.map(k => [k.id, k.nama]));
  $set('kelas-list', kelas.map(k => '<li><div class="row-top"><strong>' + esc(k.nama) + '</strong><span class="badge">' + esc(k.kodeEnroll) + '</span></div>' +
    (k.deskripsi ? '<div class="row-meta">' + esc(k.deskripsi) + '</div>' : '') + '</li>').join(''));
  $set('sub-kelas', '<option value="">Pilih kelas</option>' + kelas.map(k => '<option value="' + k.id + '">' + esc(k.nama) + '</option>').join(''));
  $set('user-list', users.map(u => '<li><div class="row-top"><span>' + esc(u.username) + ' (' + esc(u.role) + ')</span>' +
    '<button class="btn btn-small btn-danger" onclick="adminDeleteUser(' + u.id + ')">Hapus</button></div></li>').join(''));
  $set('sub-list', subs.map(s => '<li><div class="row-top"><span><strong>' + esc(s.nama) + '</strong> (' + esc(s.kode) + ') — ' + esc(kelasMap[s.kelasId] || '-') + '</span>' +
    '<button class="btn btn-small btn-danger" onclick="adminDeleteSubj(' + s.id + ')">Hapus</button></div></li>').join(''));
}

async function adminCreateKelas() {
  const nama = document.getElementById('kelas-nama').value.trim();
  const deskripsi = document.getElementById('kelas-desc').value.trim();
  if (!nama) return;
  const r = await api('/api/admin/classes', { method: 'POST', body: JSON.stringify({ nama, deskripsi }) });
  if (r.ok) {
    document.getElementById('kelas-nama').value = '';
    document.getElementById('kelas-desc').value = '';
    await adminInit();
  }
}

async function adminCreateUser() {
  const username = document.getElementById('user-u').value.trim();
  const password = document.getElementById('user-p').value;
  const role = document.getElementById('user-r').value;
  if (!username || !password) return;
  const r = await api('/api/admin/users', { method: 'POST', body: JSON.stringify({ username, password, role }) });
  if (r.ok) {
    document.getElementById('user-u').value = '';
    document.getElementById('user-p').value = '';
    await adminInit();
  } else {
    const err = await r.json().catch(() => ({}));
    alert(err.error || 'Gagal menambah user');
  }
}

async function adminDeleteUser(id) {
  await api('/api/admin/users/' + id, { method: 'DELETE' });
  await adminInit();
}

async function adminCreateSubj() {
  const nama = document.getElementById('sub-nama').value.trim();
  const kode = document.getElementById('sub-kode').value.trim();
  const kelasId = document.getElementById('sub-kelas').value;
  if (!nama || !kode || !kelasId) return;
  const r = await api('/api/admin/subjects', { method: 'POST', body: JSON.stringify({ nama, kode, kelasId }) });
  if (r.ok) {
    document.getElementById('sub-nama').value = '';
    document.getElementById('sub-kode').value = '';
    await adminInit();
  }
}

async function adminDeleteSubj(id) {
  await api('/api/admin/subjects/' + id, { method: 'DELETE' });
  await adminInit();
}

// ==================== SEKRETARIS ====================
let editingTaskId = null;

function sekretarisHtml() {
  return '<div class="tabs">' +
      '<button id="btn-tab-sekr" class="tab-btn active" onclick="switchSekrTab(\'manage\')">Menu 1: Upload &amp; Pantau Tugas</button>' +
      '<button id="btn-tab-student" class="tab-btn" onclick="switchSekrTab(\'student\')">Menu 2: Mode Pelajar (Tugas Saya)</button>' +
    '</div>' +
    '<div id="tab-sekr-manage" class="panel">' +
      '<div class="card" style="grid-column: 1 / -1">' +
        '<h3 id="sekr-form-title">Upload Tugas Bersama</h3>' +
        '<div class="form-row">' +
          '<input id="t-title" placeholder="Judul tugas">' +
          '<select id="t-subject"><option value="">Pilih pelajaran</option></select>' +
          '<input id="t-deadline" type="date">' +
          '<select id="t-priority"><option>LOW</option><option selected>MEDIUM</option><option>HIGH</option></select>' +
        '</div>' +
        '<div class="form-row">' +
          '<input id="t-desc" placeholder="Deskripsi tugas (opsional)">' +
          '<input id="t-tempat" placeholder="Kumpulin di mana (cth: Google Classroom)">' +
          '<input id="t-format" placeholder="Format tugas (cth: PDF, ZIP)">' +
          '<button id="btn-sekr-submit" class="btn btn-primary" onclick="sekretarisSaveTugas()">Upload Tugas</button>' +
          '<button id="btn-sekr-cancel" class="btn btn-ghost hidden" onclick="sekretarisCancelEdit()">Batal Edit</button>' +
        '</div>' +
      '</div>' +
      '<div class="card" style="grid-column: 1 / -1">' +
        '<div class="row-top" style="margin-bottom:10px">' +
          '<h3 style="margin:0">Monitoring Tugas yang Diupload</h3>' +
          '<div class="row-actions">' +
            '<input id="s-filter-q" placeholder="Cari judul..." style="width:140px" oninput="loadSharedTasks()">' +
            '<select id="s-filter-subject" style="width:140px" onchange="loadSharedTasks()"><option value="">Semua Pelajaran</option></select>' +
            '<select id="s-filter-status" style="width:130px" onchange="loadSharedTasks()">' +
              '<option value="">Semua Status</option><option value="BELUM">Belum</option><option value="SELESAI">Selesai</option>' +
            '</select>' +
            '<select id="s-filter-priority" style="width:140px" onchange="loadSharedTasks()">' +
              '<option value="">Semua Prioritas</option><option value="LOW">LOW</option><option value="MEDIUM">MEDIUM</option><option value="HIGH">HIGH</option>' +
            '</select>' +
          '</div>' +
        '</div>' +
        '<ul id="tugas-list"></ul>' +
      '</div>' +
    '</div>' +
    '<div id="tab-sekr-student" class="hidden">' + pelajarHtml() + '</div>';
}

function switchSekrTab(tab) {
  const isManage = tab === 'manage';
  document.getElementById('tab-sekr-manage').classList.toggle('hidden', !isManage);
  document.getElementById('tab-sekr-student').classList.toggle('hidden', isManage);
  document.getElementById('btn-tab-sekr').classList.toggle('active', isManage);
  document.getElementById('btn-tab-student').classList.toggle('active', !isManage);
  if (!isManage) pelajarInit();
}

async function sekretarisInit() {
  editingTaskId = null;
  const subjList = await api('/api/subjects').then(r => r.json());
  $set('t-subject', '<option value="">Pilih pelajaran</option>' + subjList.map(s => '<option value="' + esc(s.nama) + '">' + esc(s.nama) + ' (' + esc(s.kode) + ')</option>').join(''));
  $set('s-filter-subject', '<option value="">Semua Pelajaran</option>' + subjList.map(s => '<option value="' + esc(s.nama) + '">' + esc(s.nama) + '</option>').join(''));
  await loadSharedTasks();
  if (sekrRefreshTimer) clearInterval(sekrRefreshTimer);
  sekrRefreshTimer = setInterval(() => {
    loadSharedTasks();
    if (!document.getElementById('tab-sekr-student')?.classList.contains('hidden')) {
      loadSharedTasksPelajar();
    }
  }, 4000);
}

async function sekretarisSaveTugas() {
  const title = document.getElementById('t-title').value.trim();
  const subject = document.getElementById('t-subject').value.trim();
  const deadline = document.getElementById('t-deadline').value;
  if (!title || !subject || !deadline) { alert('Judul, pelajaran, dan deadline wajib diisi'); return; }
  const body = {
    title,
    subject,
    deadline,
    priority: document.getElementById('t-priority').value,
    deskripsi: document.getElementById('t-desc').value.trim(),
    tempatPengumpulan: document.getElementById('t-tempat').value.trim(),
    formatDokumen: document.getElementById('t-format').value.trim()
  };
  const r = editingTaskId
    ? await api('/api/tasks/' + editingTaskId, { method: 'PUT', body: JSON.stringify(body) })
    : await api('/api/tasks', { method: 'POST', body: JSON.stringify(body) });
  if (r.ok) {
    sekretarisCancelEdit();
    await loadSharedTasks();
    await renderReminders();
  }
}

function sekretarisEditTugas(t) {
  editingTaskId = t.id;
  document.getElementById('sekr-form-title').textContent = 'Edit Tugas Bersama';
  document.getElementById('btn-sekr-submit').textContent = 'Simpan Perubahan';
  document.getElementById('btn-sekr-cancel').classList.remove('hidden');
  document.getElementById('t-title').value = t.title || '';
  document.getElementById('t-subject').value = t.subject || '';
  document.getElementById('t-deadline').value = t.deadline || '';
  document.getElementById('t-priority').value = t.priority || 'MEDIUM';
  document.getElementById('t-desc').value = t.deskripsi || '';
  document.getElementById('t-tempat').value = t.tempatPengumpulan || '';
  document.getElementById('t-format').value = t.formatDokumen || '';
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function sekretarisCancelEdit() {
  editingTaskId = null;
  document.getElementById('sekr-form-title').textContent = 'Upload Tugas Bersama';
  document.getElementById('btn-sekr-submit').textContent = 'Upload Tugas';
  document.getElementById('btn-sekr-cancel').classList.add('hidden');
  ['t-title', 't-deadline', 't-desc', 't-tempat', 't-format'].forEach(id => document.getElementById(id).value = '');
}

async function loadSharedTasks() {
  const params = new URLSearchParams();
  const q = document.getElementById('s-filter-q')?.value.trim() || '';
  const sb = document.getElementById('s-filter-subject')?.value || '';
  const st = document.getElementById('s-filter-status')?.value || '';
  const pr = document.getElementById('s-filter-priority')?.value || '';
  if (q) params.set('q', q);
  if (sb) params.set('subject', sb);
  if (st) params.set('status', st);
  if (pr) params.set('priority', pr);
  const qs = params.toString();
  const res = await api('/api/tasks' + (qs ? '?' + qs : ''));
  if (!res.ok) return;
  const list = await res.json();
  if (list.length === 0) {
    $set('tugas-list', '<li class="row-meta">Tidak ada tugas yang sesuai filter.</li>');
    return;
  }
  window._sekrTasks = Object.fromEntries(list.map(t => [t.id, t]));
  $set('tugas-list', list.map(t =>
    '<li>' +
      '<div class="row-top">' +
        '<div><strong>' + esc(t.title) + '</strong> — ' + esc(t.subject) + ' (' + esc(t.priority) + ') ' +
        '<span class="badge ' + (t.overdue ? 'badge-overdue' : '') + '">' + fmtRemaining(t.deadline) + '</span> ' +
        '<span class="badge badge-done">Selesai: ' + (t.selesaiCount ?? 0) + '/' + (t.totalPelajar ?? 0) + '</span></div>' +
        '<div class="row-actions">' +
          '<button class="btn btn-small btn-ghost" onclick="sekretarisEditTugas(window._sekrTasks[' + t.id + '])">Edit</button>' +
          '<button class="btn btn-small btn-danger" onclick="sekretarisDeleteTugas(' + t.id + ')">Hapus</button>' +
        '</div>' +
      '</div>' +
      (t.deskripsi ? '<div class="row-meta">' + esc(t.deskripsi) + '</div>' : '') +
      '<div class="row-meta">Deadline: ' + esc(t.deadline) + ' | Format Tugas: ' + esc(t.formatDokumen || '-') + ' | Kumpulin di: ' + esc(t.tempatPengumpulan || '-') + '</div>' +
    '</li>'
  ).join(''));
}

async function sekretarisDeleteTugas(id) {
  await api('/api/tasks/' + id, { method: 'DELETE' });
  await loadSharedTasks();
  await renderReminders();
}

// ==================== PELAJAR ====================
function pelajarHtml() {
  return '<div class="panel">' +
    '<div class="card" style="grid-column: 1 / -1">' +
      '<div class="row-top" style="margin-bottom:10px">' +
        '<h3 style="margin:0">Tugas Bersama (Kelas Saya)</h3>' +
        '<div class="row-actions">' +
          '<input id="p-filter-q" placeholder="Cari tugas..." style="width:140px" oninput="loadSharedTasksPelajar()">' +
          '<select id="p-filter-subject" style="width:140px" onchange="loadSharedTasksPelajar()"><option value="">Semua Pelajaran</option></select>' +
          '<select id="p-filter-status" style="width:130px" onchange="loadSharedTasksPelajar()">' +
            '<option value="">Semua Status</option><option value="BELUM">Belum</option><option value="SELESAI">Selesai</option>' +
          '</select>' +
          '<select id="p-filter-priority" style="width:140px" onchange="loadSharedTasksPelajar()">' +
            '<option value="">Semua Prioritas</option><option value="LOW">LOW</option><option value="MEDIUM">MEDIUM</option><option value="HIGH">HIGH</option>' +
          '</select>' +
        '</div>' +
      '</div>' +
      '<ul id="shared-tugas-list"></ul>' +
    '</div>' +
    '<div class="card" style="grid-column: 1 / -1">' +
      '<h3>Tugas Pribadi</h3>' +
      '<div class="form-row">' +
        '<input id="p-title" placeholder="Judul">' +
        '<input id="p-subject" placeholder="Mata kuliah">' +
        '<input id="p-deadline" type="date">' +
        '<select id="p-priority"><option>LOW</option><option selected>MEDIUM</option><option>HIGH</option></select>' +
        '<input id="p-desc" placeholder="Deskripsi (opsional)">' +
        '<button class="btn btn-primary" onclick="pelajarCreateTugas()">Tambah</button>' +
      '</div>' +
      '<ul id="p-tugas-list"></ul>' +
    '</div>' +
  '</div>';
}

async function pelajarInit() {
  const subjList = await api('/api/subjects').then(r => r.json());
  $set('p-filter-subject', '<option value="">Semua Pelajaran</option>' + subjList.map(s => '<option value="' + esc(s.nama) + '">' + esc(s.nama) + '</option>').join(''));
  await Promise.all([loadSharedTasksPelajar(), loadPersonalTasks()]);
}

async function loadSharedTasksPelajar() {
  const q = document.getElementById('p-filter-q')?.value.trim() || '';
  const sb = document.getElementById('p-filter-subject')?.value || '';
  const st = document.getElementById('p-filter-status')?.value || '';
  const pr = document.getElementById('p-filter-priority')?.value || '';
  const params = new URLSearchParams();
  if (q) params.set('q', q);
  if (sb) params.set('subject', sb);
  if (st) params.set('status', st);
  if (pr) params.set('priority', pr);
  const qs = params.toString();
  const list = await (await api('/api/tasks' + (qs ? '?' + qs : ''))).json();
  if (list.length === 0) {
    $set('shared-tugas-list', '<li class="row-meta">Tidak ada tugas yang sesuai.</li>');
    return;
  }
  $set('shared-tugas-list', list.map(t => {
    const isSelesai = t.myStatus === 'SELESAI';
    return '<li>' +
      '<div class="row-top">' +
        '<div><strong>' + esc(t.title) + '</strong> — ' + esc(t.subject) + ' (' + esc(t.priority) + ') ' +
        '<span class="badge ' + (t.overdue ? 'badge-overdue' : '') + '">' + fmtRemaining(t.deadline) + '</span>' +
        (isSelesai ? ' <span class="badge badge-done">Selesai ✓</span>' : '') + '</div>' +
        '<div class="row-actions">' +
          '<input id="s-desc-' + t.id + '" placeholder="Keterangan" style="width:140px">' +
          (isSelesai
            ? '<button class="btn btn-small btn-ghost" onclick="pelajarSetStatus(' + t.id + ', \'BELUM\')">Batalkan</button>'
            : '<button class="btn btn-small btn-success" onclick="pelajarSetStatus(' + t.id + ', \'SELESAI\')">Selesai</button>') +
        '</div>' +
      '</div>' +
      (t.deskripsi ? '<div class="row-meta">' + esc(t.deskripsi) + '</div>' : '') +
      '<div class="row-meta">Deadline: ' + esc(t.deadline) + ' | Format Tugas: ' + esc(t.formatDokumen || '-') + ' | Kumpulin di: ' + esc(t.tempatPengumpulan || '-') + '</div>' +
    '</li>';
  }).join(''));
}

async function loadPersonalTasks() {
  const list = await (await api('/api/tasks/personal')).json();
  if (list.length === 0) {
    $set('p-tugas-list', '<li class="row-meta">Belum ada tugas pribadi.</li>');
    return;
  }
  $set('p-tugas-list', list.map(t => {
    const isSelesai = t.myStatus === 'SELESAI';
    return '<li>' +
      '<div class="row-top">' +
        '<div><strong>' + esc(t.title) + '</strong> — ' + esc(t.subject) + ' (' + esc(t.priority) + ') ' +
        '<span class="badge ' + (t.overdue ? 'badge-overdue' : '') + '">' + fmtRemaining(t.deadline) + '</span>' +
        (isSelesai ? ' <span class="badge badge-done">Selesai ✓</span>' : '') + '</div>' +
        '<div class="row-actions">' +
          (isSelesai
            ? '<button class="btn btn-small btn-ghost" onclick="pelajarSetPersonalStatus(' + t.id + ', \'BELUM\')">Batalkan</button>'
            : '<button class="btn btn-small btn-success" onclick="pelajarSetPersonalStatus(' + t.id + ', \'SELESAI\')">Selesai</button>') +
          '<button class="btn btn-small btn-danger" onclick="pelajarDeleteTugas(' + t.id + ')">Hapus</button>' +
        '</div>' +
      '</div>' +
      (t.deskripsi ? '<div class="row-meta">' + esc(t.deskripsi) + '</div>' : '') +
    '</li>';
  }).join(''));
}

async function pelajarCreateTugas() {
  const title = document.getElementById('p-title').value.trim();
  const subject = document.getElementById('p-subject').value.trim();
  const deadline = document.getElementById('p-deadline').value;
  if (!title || !subject || !deadline) { alert('Judul, mata kuliah, dan deadline wajib diisi'); return; }
  const body = {
    title,
    subject,
    deadline,
    priority: document.getElementById('p-priority').value,
    deskripsi: document.getElementById('p-desc').value.trim()
  };
  const r = await api('/api/tasks/personal', { method: 'POST', body: JSON.stringify(body) });
  if (r.ok) {
    ['p-title', 'p-subject', 'p-deadline', 'p-desc'].forEach(id => document.getElementById(id).value = '');
    await pelajarInit();
    await renderReminders();
  }
}

async function pelajarDeleteTugas(id) {
  await api('/api/tasks/personal/' + id, { method: 'DELETE' });
  await pelajarInit();
  await renderReminders();
}

async function pelajarSetStatus(id, status) {
  const ket = document.getElementById('s-desc-' + id)?.value.trim() || '';
  await api('/api/tasks/' + id + '/status', { method: 'POST', body: JSON.stringify({ status, keterangan: ket }) });
  await pelajarInit();
  await renderReminders();
}

async function pelajarSetPersonalStatus(id, status) {
  await api('/api/tasks/personal/' + id + '/status', { method: 'POST', body: JSON.stringify({ status }) });
  await pelajarInit();
  await renderReminders();
}

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btn-login').onclick = login;
  document.getElementById('password').onkeydown = (e) => { if (e.key === 'Enter') login(); };
  document.getElementById('enroll-code').onkeydown = (e) => { if (e.key === 'Enter') submitEnroll(); };
});
