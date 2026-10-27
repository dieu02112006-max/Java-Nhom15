(async () => {
    const response = await fetch('/api/auth/me', {credentials:'same-origin'});
    if (!response.ok) { location.href = `/login/index.html?next=${encodeURIComponent(location.pathname + location.search)}`; return; }
    const user = await response.json();
    const userId = user.id;
    const TASK_KEY = `dayflow.todo.tasks.v1.${userId}`;
    const GOAL_KEY = `dayflow.todo.work-goals.v1.${userId}`;
    const PACKAGE_KEY = `dayflow.todo.package.v1.${userId}`;
    const [skinListResponse,currentSkinResponse]=await Promise.all([
        fetch('/api/skins',{credentials:'same-origin'}),
        fetch('/api/auth/me/skin',{credentials:'same-origin'})
    ]);
    if(!skinListResponse.ok||!currentSkinResponse.ok){location.href='/todo/index.html';return;}
    const skins=(await skinListResponse.json()).filter(skin=>skin.isActive);
    let currentSkin=await currentSkinResponse.json();
    const pad = value => String(value).padStart(2,'0');
    const dateKey = date => `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())}`;
    const today = new Date();
    const plusDays = days => { const date = new Date(); date.setDate(date.getDate()+days); return dateKey(date); };
    const safe = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
    function safeImageUrl(value) {
        if(!value)return 'none';
        try { const url=new URL(value,location.origin);return url.origin===location.origin?`url("${url.href.replaceAll('"','%22')}")`:'none'; } catch { return 'none'; }
    }
    function applySkin(skin) {
        if(!skin)return;
        const root=document.documentElement;
        const dark=skin.code==='dark';
        root.dataset.skin=skin.code;
        root.style.setProperty('--blue',skin.primaryColor);
        root.style.setProperty('--blue-dark',`color-mix(in srgb, ${skin.primaryColor} 78%, #111827)`);
        root.style.setProperty('--blue-pale',`color-mix(in srgb, ${skin.primaryColor} 13%, ${skin.backgroundColor})`);
        root.style.setProperty('--pink',skin.secondaryColor);
        root.style.setProperty('--pink-pale',`color-mix(in srgb, ${skin.secondaryColor} 18%, ${skin.backgroundColor})`);
        root.style.setProperty('--canvas',skin.backgroundColor);
        root.style.setProperty('--ink',skin.textColor);
        root.style.setProperty('--muted',dark?'#B9C3D2':'#73798C');
        root.style.setProperty('--line',dark?'#465168':'#E4E8F0');
        root.style.setProperty('--card',dark?'rgba(37,45,57,.94)':'rgba(255,255,255,.88)');
        root.style.setProperty('--theme-background-image',safeImageUrl(skin.backgroundImageUrl));
        document.querySelector('meta[name="theme-color"]').content=skin.backgroundColor;
    }
    let tasks;
    try { tasks = JSON.parse(localStorage.getItem(TASK_KEY)); } catch { tasks = null; }
    if (!Array.isArray(tasks)) {
        tasks = [
            {id:crypto.randomUUID(),title:'Hoàn thành bài tập Java chương 4',category:'Học tập',dueDate:dateKey(today),completed:false},
            {id:crypto.randomUUID(),title:'Gửi bản kế hoạch cho nhóm',category:'Công việc',dueDate:plusDays(1),completed:false},
            {id:crypto.randomUUID(),title:'Đặt lịch khám sức khỏe định kì',category:'Cá nhân',dueDate:plusDays(3),completed:false},
            {id:crypto.randomUUID(),title:'Sắp xếp lại ghi chú tuần này',category:'Cá nhân',dueDate:plusDays(-1),completed:true,completedAt:dateKey(today)}
        ];
        localStorage.setItem(TASK_KEY,JSON.stringify(tasks));
    }
    let migratedTasks=false;
    tasks=tasks.map(task=>{if(task.completed&&!task.completedAt){migratedTasks=true;return {...task,completedAt:dateKey(today)};}return task;});
    if(migratedTasks)localStorage.setItem(TASK_KEY,JSON.stringify(tasks));
    let goals;
    try { goals = JSON.parse(localStorage.getItem(GOAL_KEY)); } catch { goals = null; }
    if (!Array.isArray(goals)) {
        const due = new Date(); due.setDate(due.getDate()+14);
        goals = [{id:crypto.randomUUID(),title:'Hoàn thành 10 công việc',target:10,deadline:dateKey(due),createdAt:dateKey(today)}];
        localStorage.setItem(GOAL_KEY,JSON.stringify(goals));
    }
    const viewNames = {calendar:'Lịch',goals:'Mục tiêu',stats:'Thống kê',themes:'Chủ đề',packages:'Gói nạp'};
    const requestedView = document.body.dataset.view || new URLSearchParams(location.search).get('view');
    const view = viewNames[requestedView] ? requestedView : 'calendar';
    document.title = `${viewNames[view]} — Dayflow`;
    document.getElementById('avatar').textContent = user.fullName.trim().split(/\s+/).slice(-2).map(part=>part[0]).join('').toUpperCase();
    document.querySelector(`[data-nav="${view}"]`).classList.add('active');
    applySkin(currentSkin);
    document.getElementById('logout').addEventListener('click',async()=>{
        await fetch('/api/auth/csrf',{credentials:'same-origin'});
        const cookie=document.cookie.split('; ').find(value=>value.startsWith('XSRF-TOKEN='));
        await fetch('/api/auth/logout',{method:'POST',credentials:'same-origin',headers:{'X-XSRF-TOKEN':cookie?decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)):''}});
        location.href='/login/index.html';
    });
    const content=document.getElementById('page-content');
    const showMessage=message=>{const node=document.getElementById('message');node.textContent=message;node.classList.add('show');clearTimeout(showMessage.timer);showMessage.timer=setTimeout(()=>node.classList.remove('show'),2600);};
    const heading=(eyebrow,title,subtitle)=>`<section class="heading"><div><div class="eyebrow"><span class="dot"></span>${eyebrow}</div><h1>${title}</h1><p class="subtitle">${subtitle}</p></div></section>`;
    const fmtDate=value=>value?new Intl.DateTimeFormat('vi-VN',{day:'numeric',month:'long',year:'numeric'}).format(new Date(`${value}T00:00:00`)):'Chưa chọn hạn';
    const saveTasks=()=>localStorage.setItem(TASK_KEY,JSON.stringify(tasks));
    function renderCalendar() {
        let cursor=new Date(today.getFullYear(),today.getMonth(),1),selected=dateKey(today),mode='month';
        content.innerHTML=heading('KẾ HOẠCH CỦA BẠN','Lịch công việc','Xem hạn công việc theo tháng hoặc tuần và chọn một ngày để xem lịch trình.')+
            `<div class="calendar-controls"><div class="control-row"><button class="button" id="prev-period" aria-label="Kì trước">‹</button><button class="button" id="today-button">Hôm nay</button><button class="button" id="next-period" aria-label="Kì tiếp theo">›</button></div><div class="calendar-title" id="calendar-title"></div><div class="control-row"><button class="button" id="month-mode">Tháng</button><button class="button" id="week-mode">Tuần</button></div></div><div class="calendar-grid" id="calendar-grid"></div><section class="card agenda"><h2 id="agenda-title"></h2><div id="agenda-list"></div></section>`;
        const grid=document.getElementById('calendar-grid');
        function draw() {
            const base=mode==='month'?new Date(cursor.getFullYear(),cursor.getMonth(),1):new Date(`${selected}T00:00:00`);
            const first=mode==='month'?base:new Date(base.getFullYear(),base.getMonth(),base.getDate()-((base.getDay()+6)%7));
            const count=mode==='month'?42:7;
            document.getElementById('calendar-title').textContent=new Intl.DateTimeFormat('vi-VN',{month:'long',year:'numeric'}).format(base);
            document.getElementById('month-mode').classList.toggle('primary',mode==='month');
            document.getElementById('week-mode').classList.toggle('primary',mode==='week');
            grid.innerHTML=['T2','T3','T4','T5','T6','T7','CN'].map(day=>`<div class="weekday">${day}</div>`).join('');
            for(let index=0;index<count;index++) {
                const date=new Date(first.getFullYear(),first.getMonth(),first.getDate()+index),key=dateKey(date),items=tasks.filter(task=>task.dueDate===key);
                const outside=mode==='month'&&date.getMonth()!==cursor.getMonth();
                grid.insertAdjacentHTML('beforeend',`<button class="day-cell ${outside?'outside':''} ${key===selected?'selected':''} ${key===dateKey(today)?'today':''}" data-day="${key}"><span class="day-number">${date.getDate()}</span>${items.slice(0,2).map(task=>`<div class="event-chip ${task.completed?'done':''}" title="${safe(task.title)}">${safe(task.title)}</div>`).join('')}${items.length>2?`<div class="more-events">+${items.length-2} việc</div>`:''}</button>`);
            }
            document.getElementById('agenda-title').textContent=new Intl.DateTimeFormat('vi-VN',{weekday:'long',day:'numeric',month:'long'}).format(new Date(`${selected}T00:00:00`));
            const dayTasks=tasks.filter(task=>task.dueDate===selected);
            document.getElementById('agenda-list').innerHTML=dayTasks.length?dayTasks.map(task=>`<div class="agenda-item ${task.completed?'done':''}"><span class="agenda-time"></span><span>${safe(task.title)}</span><span style="margin-left:auto;color:var(--muted)">${safe(task.category||'Cá nhân')}</span></div>`).join(''):'<div class="agenda-empty">Không có công việc đến hạn trong ngày này.</div>';
        }
        grid.addEventListener('click',event=>{const cell=event.target.closest('[data-day]');if(!cell)return;selected=cell.dataset.day;if(mode==='month'&&new Date(`${selected}T00:00:00`).getMonth()!==cursor.getMonth())cursor=new Date(new Date(`${selected}T00:00:00`).getFullYear(),new Date(`${selected}T00:00:00`).getMonth(),1);draw();});
        document.getElementById('prev-period').addEventListener('click',()=>{if(mode==='month')cursor=new Date(cursor.getFullYear(),cursor.getMonth()-1,1);else selected=dateKey(new Date(new Date(`${selected}T00:00:00`).getFullYear(),new Date(`${selected}T00:00:00`).getMonth(),new Date(`${selected}T00:00:00`).getDate()-7));draw();});
        document.getElementById('next-period').addEventListener('click',()=>{if(mode==='month')cursor=new Date(cursor.getFullYear(),cursor.getMonth()+1,1);else selected=dateKey(new Date(new Date(`${selected}T00:00:00`).getFullYear(),new Date(`${selected}T00:00:00`).getMonth(),new Date(`${selected}T00:00:00`).getDate()+7));draw();});
        document.getElementById('today-button').addEventListener('click',()=>{selected=dateKey(today);cursor=new Date(today.getFullYear(),today.getMonth(),1);draw();});
        document.getElementById('month-mode').addEventListener('click',()=>{mode='month';cursor=new Date(new Date(`${selected}T00:00:00`).getFullYear(),new Date(`${selected}T00:00:00`).getMonth(),1);draw();});
        document.getElementById('week-mode').addEventListener('click',()=>{mode='week';draw();});
        draw();
    }
    function renderGoals() {
        content.innerHTML=heading('KẾ HOẠCH CÁ NHÂN','Mục tiêu công việc','Đặt mục tiêu rõ ràng, chọn số lượng công việc và thời hạn hoàn thành.')+
            `<form class="card goal-form" id="goal-form"><div class="field"><label for="goal-title">Mục tiêu</label><input id="goal-title" required maxlength="120" placeholder="Ví dụ: Hoàn thành đồ án Java"></div><div class="field"><label for="goal-target">Số việc cần xong</label><input id="goal-target" type="number" min="1" max="999" value="10" required></div><div class="field"><label for="goal-deadline">Hoàn thành trước</label><input id="goal-deadline" type="date" min="${dateKey(today)}" required></div><button class="button primary" type="submit">Tạo mục tiêu</button></form><div class="goal-list" id="goal-list"></div>`;
        const due=new Date();due.setDate(due.getDate()+14);document.getElementById('goal-deadline').value=dateKey(due);
        function drawGoals(){
            document.getElementById('goal-list').innerHTML=goals.length?goals.map(goal=>{const done=tasks.filter(task=>task.completed&&task.completedAt&&task.completedAt>=goal.createdAt&&task.completedAt<=goal.deadline).length;const progress=Math.min(100,Math.round(done/goal.target*100));const remaining=Math.round((new Date(`${goal.deadline}T00:00:00`)-new Date(`${dateKey(today)}T00:00:00`))/86400000);const timeLabel=remaining<0?`Quá hạn ${Math.abs(remaining)} ngày`:remaining===0?'Hạn hôm nay':`Còn ${remaining} ngày`;return `<article class="card goal-card"><h2>${safe(goal.title)}</h2><div class="goal-meta"><span>${done} / ${goal.target} công việc hoàn thành</span><strong>${progress}%</strong></div><div class="progress"><span style="width:${progress}%"></span></div><div class="goal-footer"><span>Hạn: ${fmtDate(goal.deadline)} · ${timeLabel}</span><button class="delete-goal" data-delete-goal="${goal.id}" type="button">Xóa mục tiêu</button></div></article>`;}).join(''):'<div class="card goal-card">Chưa có mục tiêu. Hãy tạo mục tiêu đầu tiên ở phía trên.</div>';
        }
        document.getElementById('goal-form').addEventListener('submit',event=>{event.preventDefault();const title=document.getElementById('goal-title').value.trim(),target=Number(document.getElementById('goal-target').value),deadline=document.getElementById('goal-deadline').value;if(!title||!target||!deadline)return;goals.unshift({id:crypto.randomUUID(),title,target,deadline,createdAt:dateKey(today)});localStorage.setItem(GOAL_KEY,JSON.stringify(goals));event.target.reset();document.getElementById('goal-deadline').value=deadline;drawGoals();showMessage('Đã tạo mục tiêu công việc.');});
        document.getElementById('goal-list').addEventListener('click',event=>{const button=event.target.closest('[data-delete-goal]');if(!button)return;goals=goals.filter(goal=>goal.id!==button.dataset.deleteGoal);localStorage.setItem(GOAL_KEY,JSON.stringify(goals));drawGoals();});
        drawGoals();
    }
    function renderStats() {
        const completed=tasks.filter(task=>task.completed).length,open=tasks.length-completed,overdue=tasks.filter(task=>!task.completed&&task.dueDate&&task.dueDate<dateKey(today)).length,rate=tasks.length?Math.round(completed/tasks.length*100):0;
        const groups={};tasks.forEach(task=>{const category=task.category||'Khác';groups[category]=(groups[category]||0)+1;});
        const nextDays=Array.from({length:7},(_,index)=>{const date=new Date();date.setDate(date.getDate()+index);const key=dateKey(date);return {label:new Intl.DateTimeFormat('vi-VN',{weekday:'short',day:'numeric'}).format(date),value:tasks.filter(task=>task.dueDate===key).length};});
        const categoryRows=Object.entries(groups).sort((a,b)=>b[1]-a[1]).map(([name,value])=>`<div class="bar-row"><span>${safe(name)}</span><div class="bar-track"><div class="bar-fill" style="width:${Math.max(4,value/Math.max(1,...Object.values(groups))*100)}%"></div></div><span class="bar-number">${value}</span></div>`).join('')||'<p class="subtitle">Chưa có dữ liệu công việc.</p>';
        const dateRows=nextDays.map(day=>`<div class="bar-row"><span>${safe(day.label)}</span><div class="bar-track"><div class="bar-fill" style="width:${Math.max(day.value?5:0,day.value/Math.max(1,...nextDays.map(item=>item.value))*100)}%"></div></div><span class="bar-number">${day.value}</span></div>`).join('');
        content.innerHTML=heading('TỔNG QUAN TIẾN ĐỘ','Thống kê công việc','Tổng hợp tự động từ danh sách công việc của bạn.')+
            `<div class="stats-grid"><article class="card stat-card"><div class="stat-label">Tổng công việc</div><div class="stat-value">${tasks.length}</div></article><article class="card stat-card"><div class="stat-label">Đã hoàn thành</div><div class="stat-value">${completed}</div></article><article class="card stat-card"><div class="stat-label">Đang thực hiện</div><div class="stat-value">${open}</div></article><article class="card stat-card"><div class="stat-label">Quá hạn</div><div class="stat-value">${overdue}</div></article></div><div class="stats-panels"><section class="card panel"><h2>Tỉ lệ hoàn thành</h2><div class="stat-value">${rate}%</div><div class="progress" style="margin:12px 0"><span style="width:${rate}%"></span></div><p class="subtitle">${completed} trên ${tasks.length} công việc đã được đánh dấu hoàn thành.</p></section><section class="card panel"><h2>Công việc theo danh mục</h2>${categoryRows}</section><section class="card panel" style="grid-column:1/-1"><h2>Lịch đến hạn trong 7 ngày tới</h2>${dateRows}</section></div>`;
    }
    function renderThemes() {
        content.innerHTML=heading('CÁ NHÂN HÓA','Chủ đề','Chọn màu sắc hiển thị cho không gian công việc của bạn.')+
            `<div class="theme-grid">${skins.map(skin=>`<button class="card theme-card ${currentSkin.id===skin.id?'selected':''}" data-theme-id="${skin.id}" type="button"><div class="theme-preview" style="--preview-bg:${safe(skin.backgroundColor)};--preview-primary:${safe(skin.primaryColor)};--preview-secondary:${safe(skin.secondaryColor)};--preview-accent:${safe(skin.secondaryColor)};--preview-image:${safeImageUrl(skin.previewUrl||skin.backgroundImageUrl)}"><div class="preview-bar"></div><div class="preview-line"></div><div class="preview-line short"></div><div class="preview-chip"></div></div><div class="theme-info"><span><strong>${safe(skin.name)}${skin.isDefault?' · Mặc định':''}</strong><br><small>${safe(skin.code)}</small></span><small>${currentSkin.id===skin.id?'Đang dùng':'Chọn chủ đề'}</small></div></button>`).join('')}</div>`;
        document.querySelectorAll('[data-theme-id]').forEach(button=>button.addEventListener('click',async()=>{
            const skinId=Number(button.dataset.themeId);
            await fetch('/api/auth/csrf',{credentials:'same-origin'});
            const csrfCookie=document.cookie.split('; ').find(value=>value.startsWith('XSRF-TOKEN='));
            const response=await fetch('/api/auth/me/skin',{method:'PUT',credentials:'same-origin',headers:{'Content-Type':'application/json','X-XSRF-TOKEN':csrfCookie?decodeURIComponent(csrfCookie.slice('XSRF-TOKEN='.length)):''},body:JSON.stringify({skinId})});
            if(!response.ok){showMessage('Không thể lưu chủ đề. Hãy thử lại.');return;}
            currentSkin=await response.json();applySkin(currentSkin);renderThemes();showMessage(`Đã lưu chủ đề ${currentSkin.name} cho tài khoản của bạn.`);
        }));
    }
    function renderPackages() {
        const packages=[
            {id:'starter',name:'Gói nhỏ',credits:100,price:19000,description:'Dùng thử các tiện ích mở rộng.',items:['100 điểm Dayflow','Dùng cho các tiện ích trong ứng dụng','Không tự động gia hạn']},
            {id:'popular',name:'Gói phổ biến',credits:500,price:79000,description:'Phù hợp với kế hoạch đều đặn.',items:['500 điểm Dayflow','Tiết kiệm hơn 17% so với gói nhỏ','Không tự động gia hạn'],featured:true},
            {id:'value',name:'Gói tiết kiệm',credits:1200,price:149000,description:'Nhiều điểm hơn cho kế hoạch dài hạn.',items:['1.200 điểm Dayflow','Tiết kiệm hơn 34% so với gói nhỏ','Không tự động gia hạn']}
        ];
        const selected=localStorage.getItem(PACKAGE_KEY)||'';
        content.innerHTML=heading('GÓI DỊCH VỤ','Gói nạp','Tham khảo các gói điểm để mở rộng trải nghiệm Dayflow.')+
            `<div class="package-note"><strong>Dữ liệu minh họa.</strong> Giá và số điểm bên dưới là mẫu để xem giao diện. Ứng dụng chưa kết nối thanh toán hoặc ví điểm, nên chọn gói chỉ lưu lựa chọn trên trình duyệt.</div><div class="package-grid">${packages.map(plan=>`<article class="card package-card ${plan.featured?'featured':''}">${plan.featured?'<span class="package-badge">Phổ biến</span>':''}<h2>${plan.name}</h2><div class="package-description">${plan.description}</div><div class="package-credits">${plan.credits.toLocaleString('vi-VN')} <small>điểm</small></div><div class="package-price">${plan.price.toLocaleString('vi-VN')} ₫ · thanh toán một lần</div><ul>${plan.items.map(item=>`<li>${item}</li>`).join('')}</ul><button class="button ${plan.featured?'primary':''}" data-package="${plan.id}" type="button">${selected===plan.id?'Đang chọn':'Chọn gói'}</button></article>`).join('')}</div>`;
        document.querySelectorAll('[data-package]').forEach(button=>button.addEventListener('click',()=>{localStorage.setItem(PACKAGE_KEY,button.dataset.package);renderPackages();showMessage('Đã lưu lựa chọn gói mẫu. Chưa phát sinh thanh toán.');}));
    }
    if(view==='calendar')renderCalendar();
    if(view==='goals')renderGoals();
    if(view==='stats')renderStats();
    if(view==='themes')renderThemes();
    if(view==='packages')renderPackages();
})();
