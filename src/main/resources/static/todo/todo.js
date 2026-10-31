(async () => {
    const authResponse = await fetch('/api/auth/me', { credentials:'same-origin' });
    if (!authResponse.ok) { location.href = `/login/index.html?next=${encodeURIComponent(location.pathname)}`; return; }
    const currentUser = await authResponse.json();
    const skinResponse = await fetch('/api/auth/me/skin', {credentials:'same-origin'});
    if (skinResponse.ok) {
        const skin = await skinResponse.json();
        const root = document.documentElement;
        const dark = skin.code === 'dark';
        let image = 'none';
        try { const url = new URL(skin.backgroundImageUrl, location.origin); if (url.origin === location.origin) image = `url("${url.href.replaceAll('"','%22')}")`; } catch {}
        root.dataset.skin = skin.code;
        root.style.setProperty('--blue', skin.primaryColor);
        root.style.setProperty('--blue-dark', `color-mix(in srgb, ${skin.primaryColor} 78%, #111827)`);
        root.style.setProperty('--blue-pale', `color-mix(in srgb, ${skin.primaryColor} 13%, ${skin.backgroundColor})`);
        root.style.setProperty('--pink', skin.secondaryColor);
        root.style.setProperty('--pink-pale', `color-mix(in srgb, ${skin.secondaryColor} 18%, ${skin.backgroundColor})`);
        root.style.setProperty('--canvas', skin.backgroundColor);
        root.style.setProperty('--ink', skin.textColor);
        root.style.setProperty('--muted', dark ? '#B9C3D2' : '#73798C');
        root.style.setProperty('--line', dark ? '#465168' : '#E4E8F0');
        root.style.setProperty('--card', dark ? 'rgba(37,45,57,.94)' : 'rgba(255,255,255,.88)');
        root.style.setProperty('--theme-background-image', image);
        document.querySelector('meta[name="theme-color"]').content = skin.backgroundColor;
    }
    const today = new Date();
    const dateKey = (date) => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
    let tasks = [];
    let goals = [];
    let currentFilter = 'all';
    let selectedDay = null;
    const list = document.getElementById('task-list');
    const search = document.getElementById('search');
    const empty = document.getElementById('empty-state');
    let loadError = '';
    const taskDialog = document.getElementById('task-dialog');
    const taskForm = document.getElementById('task-form');
    const taskIdInput = document.getElementById('task-id');
    const taskTitleInput = document.getElementById('task-title-input');
    const taskCategoryInput = document.getElementById('task-category');
    const taskGoalInput = document.getElementById('task-goal');
    const categoryPreview = document.getElementById('category-preview');
    const taskDateInput = document.getElementById('task-date-input');
    const taskLocationInput = document.getElementById('task-location');
    const taskStatusInput = document.getElementById('task-status');
    const taskPriorityInput = document.getElementById('task-priority');
    const taskDescriptionInput = document.getElementById('task-description');
    const mapsLink = document.getElementById('maps-link');
    const tick = '<svg viewBox="0 0 16 16" fill="none"><path d="m3.5 8 3 3 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
    const categoryStickers = {'Cá nhân':'👤','Công việc':'💼','Học tập':'📚','Sức khỏe':'🌿','Khác':'✨'};
    const csrfHeaders = async () => {
        await fetch('/api/auth/csrf',{credentials:'same-origin'});
        const cookie=document.cookie.split('; ').find(value=>value.startsWith('XSRF-TOKEN='));
        return {'Content-Type':'application/json','X-XSRF-TOKEN':cookie?decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)):''};
    };
    const api = async (path, options={}) => {
        const response=await fetch(path,{credentials:'same-origin',...options});
        if(!response.ok) {
            const problem=await response.json().catch(()=>null);
            throw new Error(problem?.message||problem?.detail||`Không thể đọc hoặc lưu công việc (HTTP ${response.status}).`);
        }
        return response.status===204?null:response.json();
    };
    const calendarLabel = (value) => {
        if (!value) return '—';
        const day=value.slice(0,10);
        let label;
        if (day === dateKey(new Date())) label='Hôm nay';
        else if (day === dateKey(new Date(Date.now()+86400000))) label='Ngày mai';
        else { const [y,m,d]=day.split('-').map(Number); label=new Intl.DateTimeFormat('vi-VN',{day:'numeric',month:'short'}).format(new Date(y,m-1,d)); }
        const time=value.length>10&&value.slice(11,16)!=='00:00'?value.slice(11,16):'';
        return time?`${label} · ${time}`:label;
    };
    const escapeHtml = (text='') => String(text).replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
    function renderGoalOptions() {
        taskGoalInput.innerHTML='<option value="">Không thuộc mục tiêu</option>'+goals.map(goal=>`<option value="${goal.id}">${escapeHtml(goal.title)}</option>`).join('');
    }
    function renderWeek() {
        const monday = new Date(today);
        monday.setHours(0, 0, 0, 0);
        monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7));
        const dayNames = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
        document.getElementById('week-days').innerHTML = Array.from({length:7}, (_, index) => {
            const day = new Date(monday);
            day.setDate(monday.getDate() + index);
            const key = dateKey(day);
            const count = tasks.filter(task => task.dueDate?.slice(0,10) === key).length;
            const active = selectedDay === key || (!selectedDay && key === dateKey(today));
            return `<button class="week-day ${active?'active':''} ${key===dateKey(today)?'is-today':''}" type="button" data-day="${key}" aria-pressed="${active}"><span>${dayNames[index]}</span><strong>${day.getDate()}</strong><small>${count} việc</small></button>`;
        }).join('');
        document.getElementById('week-days').querySelectorAll('[data-day]').forEach(button => button.addEventListener('click', () => {
            selectedDay = button.dataset.day;
            document.querySelectorAll('.filter-button').forEach(item => { item.classList.remove('active'); item.setAttribute('aria-selected','false'); });
            render();
        }));
    }
    function render() {
        const query = search.value.trim().toLocaleLowerCase('vi');
        const filtered = tasks.filter(task => {
            const matchText = `${task.title} ${task.description||''} ${task.category} ${task.location||''} ${task.status} ${task.priority}`.toLocaleLowerCase('vi').includes(query);
            const taskDay=task.dueDate?.slice(0,10);
            const matchFilter = selectedDay ? taskDay === selectedDay : currentFilter === 'all' || (currentFilter === 'today' && taskDay === dateKey(new Date()) && task.status !== 'done') || (currentFilter === 'upcoming' && taskDay > dateKey(new Date()) && task.status !== 'done') || (currentFilter === 'completed' && task.status === 'done');
            return matchText && matchFilter;
        });
        const statusLabels={todo:'Cần làm',in_progress:'Đang làm',done:'Hoàn thành'};
        const priorityLabels={low:'Thấp',medium:'Trung bình',high:'Cao'};
        const showDate=value=>value?escapeHtml(value.replace('T',' ').slice(0,19)):'—';
        list.innerHTML = filtered.map(task => {
            const categoryClass={'Công việc':'work','Cá nhân':'personal','Học tập':'study','Sức khỏe':'health'}[task.category]||'other';
            const map=task.location?`<a class="cell-location" href="https://www.google.com/maps/search/?api=1&amp;query=${encodeURIComponent(task.location)}" target="_blank" rel="noopener noreferrer">${escapeHtml(task.location)}</a>`:'—';
            return `<tr class="task-row ${task.status==='done'?'is-complete':''}"><td><div class="task-main"><button class="task-check ${task.status==='done'?'checked':''}" data-action="toggle" data-id="${task.id}" aria-label="Đổi trạng thái">${task.status==='done'?tick:''}</button><span class="task-title" title="${escapeHtml(task.title)}">${escapeHtml(task.title)}${task.description?`<small class="task-subdescription">${escapeHtml(task.description)}</small>`:''}</span></div></td><td><span class="task-tag ${categoryClass}"><span class="category-sticker" aria-hidden="true">${categoryStickers[task.category]||categoryStickers['Khác']}</span>${escapeHtml(task.category||'Khác')}</span></td><td><span class="status-pill ${escapeHtml(task.status)}">${escapeHtml(statusLabels[task.status]||task.status)}</span></td><td><span class="priority-pill ${escapeHtml(task.priority)}">${escapeHtml(priorityLabels[task.priority]||task.priority)}</span></td><td>${showDate(task.dueDate)}</td><td>${map}</td><td><span class="task-actions"><button class="row-menu edit" data-action="edit" data-id="${task.id}" aria-label="Sửa công việc"><svg viewBox="0 0 24 24" fill="none"><path d="m14 6 4 4M4 20l4-.8L19 8a2.1 2.1 0 0 0-3-3L5 16l-1 4Z" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg></button><button class="row-menu" data-action="delete" data-id="${task.id}" aria-label="Xóa công việc"><svg viewBox="0 0 24 24" fill="none"><path d="M5 7h14M10 11v6m4-6v6M6.5 7l.8 13h9.4l.8-13M9 7V4h6v3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg></button></span></td></tr>`;
        }).join('');
        empty.classList.toggle('visible', Boolean(loadError) || filtered.length === 0);
        if(loadError) empty.textContent=loadError;
        document.getElementById('stat-total').textContent = tasks.length;
        document.getElementById('stat-done').textContent = tasks.filter(task => task.status==='done').length;
        document.getElementById('stat-open').textContent = tasks.filter(task => task.status!=='done').length;
        document.getElementById('count-all').textContent = tasks.length;
        renderWeek();
    }
    function showToast(message) { const toast=document.getElementById('toast'); toast.textContent=message; toast.classList.add('show'); clearTimeout(showToast.timer); showToast.timer=setTimeout(()=>toast.classList.remove('show'),2100); }
    function openTaskForm(task=null) {
        taskForm.reset();
        taskIdInput.value=task?.id||'';
        taskTitleInput.value=task?.title||'';
        taskCategoryInput.value=task?.category||'Cá nhân';
        taskGoalInput.value=task?.goalId?String(task.goalId):'';
        updateCategoryPreview();
        taskDescriptionInput.value=task?.description||'';
        taskStatusInput.value=task?.status||'todo';
        taskPriorityInput.value=task?.priority||'medium';
        taskDateInput.value=task?.dueDate?.slice(0,16)||'';
        taskLocationInput.value=task?.location||'';
        document.getElementById('task-dialog-title').textContent=task?'Sửa công việc':'Thêm công việc';
        document.getElementById('save-task').textContent=task?'Lưu thay đổi':'Lưu công việc';
        taskDialog.showModal();
        taskTitleInput.focus();
        updateMapsLink();
    }
    function taskPayload(task) {
        return {listId:Number(task.listId),parentTaskId:task.parentTaskId??null,goalId:task.goalId??null,title:task.title,description:task.description||null,category:task.category||'Cá nhân',location:task.location||null,status:task.status||'todo',priority:task.priority||'medium',dueDate:task.dueDate||null,completedAt:task.completedAt||null,estimatedCost:task.estimatedCost??null,recurrenceRule:task.recurrenceRule||null,position:task.position??0};
    }
    function updateMapsLink() {
        const place=taskLocationInput.value.trim();
        mapsLink.href=place?`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(place)}`:'https://maps.google.com/';
    }
    function updateCategoryPreview() {
        categoryPreview.textContent=categoryStickers[taskCategoryInput.value]||categoryStickers['Khác'];
        categoryPreview.classList.remove('pop');
        requestAnimationFrame(()=>categoryPreview.classList.add('pop'));
        setTimeout(()=>categoryPreview.classList.remove('pop'),220);
    }
    document.querySelectorAll('.filter-button').forEach(button => button.addEventListener('click', () => {
        selectedDay = null;
        currentFilter = button.dataset.filter;
        document.querySelectorAll('.filter-button').forEach(item => { const active=item===button; item.classList.toggle('active',active); item.setAttribute('aria-selected',String(active)); });
        render();
    }));
    document.getElementById('week-today').addEventListener('click', () => { selectedDay=null; currentFilter='all'; document.querySelectorAll('.filter-button').forEach(item=>{const active=item.dataset.filter==='all';item.classList.toggle('active',active);item.setAttribute('aria-selected',String(active));});render(); });
    search.addEventListener('input', render);
    list.addEventListener('click', event => {
        const button = event.target.closest('[data-action]');
        if (!button) return;
        const task = tasks.find(item => String(item.id) === button.dataset.id);
        if (!task) return;
        if (button.dataset.action === 'edit') { openTaskForm(task); return; }
        if (button.dataset.action === 'delete' && !confirm(`Xóa công việc “${task.title}”?`)) return;
        (async()=>{
            try {
                if (button.dataset.action === 'delete') {
                    await api(`/api/todo/tasks/${task.id}`,{method:'DELETE',headers:await csrfHeaders()});
                    tasks=tasks.filter(item=>String(item.id)!==String(task.id));
                    showToast('Đã xóa công việc');
                } else {
                    const payload=taskPayload(task);
                    if(button.dataset.action==='toggle') { payload.status=task.status==='done'?'todo':'done'; if(payload.status!=='done') payload.completedAt=null; }
                    const updated=await api(`/api/todo/tasks/${task.id}`,{method:'PUT',headers:await csrfHeaders(),body:JSON.stringify(payload)});
                    tasks=tasks.map(item=>String(item.id)===String(task.id)?updated:item);
                    showToast(updated.status==='done'?'Đã hoàn thành công việc':'Đã cập nhật công việc');
                }
                render();
            } catch(error) { showToast(error.message); }
        })();
    });
    taskForm.addEventListener('submit', async event => {
        event.preventDefault();
        const id=taskIdInput.value;
        const existing=tasks.find(task=>String(task.id)===id);
        const status=taskStatusInput.value;
        const payload={
            listId:existing?.listId??null,parentTaskId:existing?.parentTaskId??null,goalId:taskGoalInput.value?Number(taskGoalInput.value):null,
            title:taskTitleInput.value.trim(),description:taskDescriptionInput.value.trim()||null,
            category:taskCategoryInput.value,location:taskLocationInput.value.trim()||null,
            status,priority:taskPriorityInput.value,dueDate:taskDateInput.value||null,
            completedAt:status==='done'?(existing?.completedAt||null):null,
            estimatedCost:existing?.estimatedCost??null,recurrenceRule:existing?.recurrenceRule||null,
            position:existing?.position??0
        };
        const saveButton=document.getElementById('save-task');
        saveButton.disabled=true;
        try {
            const saved=await api(id?`/api/todo/tasks/${id}`:'/api/todo/tasks',{method:id?'PUT':'POST',headers:await csrfHeaders(),body:JSON.stringify(payload)});
            if(id) tasks=tasks.map(task=>String(task.id)===id?saved:task); else tasks.unshift(saved);
            taskDialog.close(); render(); showToast(id?'Đã cập nhật công việc':'Đã thêm công việc mới');
        } catch(error) { showToast(error.message); }
        finally { saveButton.disabled=false; }
    });
    document.getElementById('focus-add').addEventListener('click',()=>openTaskForm());
    document.getElementById('close-task-dialog').addEventListener('click',()=>taskDialog.close());
    document.getElementById('cancel-task').addEventListener('click',()=>taskDialog.close());
    taskDialog.addEventListener('click',event=>{if(event.target===taskDialog)taskDialog.close();});
    taskLocationInput.addEventListener('input',updateMapsLink);
    taskCategoryInput.addEventListener('change',updateCategoryPreview);
    try { [tasks,goals]=await Promise.all([api('/api/todo/tasks'),api('/api/todo/goals')]); renderGoalOptions(); }
    catch(error) { loadError=`Không tải được công việc từ cơ sở dữ liệu: ${error.message}`; showToast(error.message); }
    render();
})();
