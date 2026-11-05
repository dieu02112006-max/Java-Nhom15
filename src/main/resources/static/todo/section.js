(async () => {
    const response = await fetch('/api/auth/me', {credentials:'same-origin'});
    if (!response.ok) { location.href = `/login/index.html?next=${encodeURIComponent(location.pathname + location.search)}`; return; }
    const [skinListResponse,currentSkinResponse,packageResponse]=await Promise.all([
        fetch('/api/skins',{credentials:'same-origin'}),
        fetch('/api/auth/me/skin',{credentials:'same-origin'}),
        fetch('/api/packages/me',{credentials:'same-origin'})
    ]);
    if(!skinListResponse.ok||!currentSkinResponse.ok||!packageResponse.ok){location.href='/todo/index.html';return;}
    const skins=(await skinListResponse.json()).filter(skin=>skin.isActive);
    let currentSkin=await currentSkinResponse.json();
    const currentPackage=await packageResponse.json();
    const packageRank=Number(currentPackage.rank||1);
    if(packageRank<2&&localStorage.getItem('dayflow-topic-background')){
        localStorage.removeItem('dayflow-topic-background');
        delete document.documentElement.dataset.topicBackground;
        document.documentElement.style.removeProperty('--topic-background-image');
    }
    const pad = value => String(value).padStart(2,'0');
    const dateKey = date => `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())}`;
    const today = new Date();
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
    let tasks=[], goals=[], dataError='';
    const viewNames = {calendar:'Lịch',goals:'Mục tiêu',stats:'Thống kê',themes:'Chủ đề',packages:'Gói nạp'};
    const requestedView = document.body.dataset.view || new URLSearchParams(location.search).get('view');
    const view = viewNames[requestedView] ? requestedView : 'calendar';
    document.title = `${viewNames[view]} — Dayflow`;
    document.querySelector(`[data-nav="${view}"]`)?.classList.add('active');
    applySkin(currentSkin);
    const content=document.getElementById('page-content');
    const showMessage=message=>{const node=document.getElementById('message');node.textContent=message;node.classList.add('show');clearTimeout(showMessage.timer);showMessage.timer=setTimeout(()=>node.classList.remove('show'),2600);};
    const csrfHeaders=async()=>{await fetch('/api/auth/csrf',{credentials:'same-origin'});const cookie=document.cookie.split('; ').find(value=>value.startsWith('XSRF-TOKEN='));return {'Content-Type':'application/json','X-XSRF-TOKEN':cookie?decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)):''};};
    const heading=()=>'';
    const fmtDate=value=>value?new Intl.DateTimeFormat('vi-VN',{day:'numeric',month:'long',year:'numeric'}).format(new Date(`${value}T00:00:00`)):'Chưa chọn hạn';
    try {
        const [taskResponse,goalResponse]=await Promise.all([
            fetch('/api/todo/tasks',{credentials:'same-origin'}),
            fetch('/api/todo/goals',{credentials:'same-origin'})
        ]);
        if(!taskResponse.ok)throw new Error(`Không thể tải công việc (HTTP ${taskResponse.status})`);
        tasks=(await taskResponse.json()).map(task=>({...task,dueDate:task.dueDate?.slice(0,10)||null,completed:task.status==='done',completedAt:task.completedAt?.slice(0,10)||null}));
        if(!goalResponse.ok)throw new Error(`Không thể tải mục tiêu (HTTP ${goalResponse.status})`);
        goals=await goalResponse.json();
    } catch(error) { dataError=error.message; showMessage(dataError); }
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
        content.innerHTML=`<div class="goal-actions-only"><button class="primary-button goal-add-button" id="focus-add-goal" type="button" aria-label="Thêm mục tiêu" title="Thêm mục tiêu"><svg viewBox="0 0 24 24" fill="none"><path d="M12 5v14m-7-7h14" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"/></svg></button></div>`+
            `<div class="goal-list" id="goal-list"></div><dialog class="goal-dialog" id="goal-dialog" aria-labelledby="goal-dialog-title"><form class="card goal-form goal-dialog-form" id="goal-form"><div class="dialog-heading"><div><h2 id="goal-dialog-title">Thêm mục tiêu</h2><p>Tạo nhóm để gắn công việc vào.</p></div><button class="dialog-close" id="close-goal-dialog" type="button" aria-label="Đóng">×</button></div><div class="field"><label for="goal-title">Tên mục tiêu</label><input id="goal-title" required maxlength="120" placeholder="Ví dụ: Sức khỏe"></div><div class="dialog-actions"><button class="button" id="cancel-goal" type="button">Hủy</button><button class="button primary" id="save-goal" type="submit">Lưu mục tiêu</button></div></form></dialog>`;
        const goalDialog=document.getElementById('goal-dialog');
        const openGoalForm=()=>{document.getElementById('goal-form').reset();goalDialog.showModal();document.getElementById('goal-title').focus();};
        document.getElementById('focus-add-goal').addEventListener('click',openGoalForm);
        document.getElementById('close-goal-dialog').addEventListener('click',()=>goalDialog.close());
        document.getElementById('cancel-goal').addEventListener('click',()=>goalDialog.close());
        goalDialog.addEventListener('click',event=>{if(event.target===goalDialog)goalDialog.close();});
        function drawGoals(){
            document.getElementById('goal-list').innerHTML=goals.length?goals.map(goal=>{const linked=tasks.filter(task=>String(task.goalId)===String(goal.id));const done=linked.filter(task=>task.status==='done').length;const progress=linked.length?Math.round(done/linked.length*100):0;const icon=/sức khỏe|suc khoe/i.test(goal.title)?'🌿':/sự kiện|su kien/i.test(goal.title)?'🎉':/máy tính|may tinh|computer/i.test(goal.title)?'💻':'🎯';const taskRows=linked.length?linked.map(task=>`<li class="goal-task ${task.status==='done'?'done':''}"><span class="goal-task-pin" aria-hidden="true">📌</span><span>${safe(task.title)}</span><small>${task.status==='done'?'Hoàn thành':'Cần làm'}</small></li>`).join(''):'<li class="goal-empty">Chưa có công việc trong mục tiêu này.</li>';return `<article class="card goal-card"><div class="goal-art"><span aria-hidden="true">${icon}</span></div><h2>${safe(goal.title)}</h2><div class="goal-meta"><span>${done}/${linked.length} công việc hoàn thành</span><strong>${progress}%</strong></div><div class="progress"><span style="width:${progress}%"></span></div><ul class="goal-tasks">${taskRows}</ul><div class="goal-footer"><span>${linked.length} công việc</span><button class="delete-goal" data-delete-goal="${goal.id}" type="button">Xóa mục tiêu</button></div></article>`;}).join(''):'<div class="card goal-card">Chưa có mục tiêu. Bấm dấu + để tạo nhóm mục tiêu đầu tiên.</div>';
        }
        document.getElementById('goal-form').addEventListener('submit',async event=>{event.preventDefault();const title=document.getElementById('goal-title').value.trim();if(!title)return;const saveButton=document.getElementById('save-goal');saveButton.disabled=true;try{const headers=await csrfHeaders();const response=await fetch('/api/todo/goals',{method:'POST',credentials:'same-origin',headers,body:JSON.stringify({title})});if(!response.ok)throw new Error('Không thể lưu mục tiêu. Hãy thử lại.');goals.unshift(await response.json());goalDialog.close();drawGoals();showMessage('Đã lưu mục tiêu vào cơ sở dữ liệu.');}catch(error){showMessage(error.message);}finally{saveButton.disabled=false;}});
        document.getElementById('goal-list').addEventListener('click',async event=>{const button=event.target.closest('[data-delete-goal]');if(!button)return;try{const headers=await csrfHeaders();const response=await fetch(`/api/todo/goals/${button.dataset.deleteGoal}`,{method:'DELETE',credentials:'same-origin',headers});if(!response.ok)throw new Error('Không thể xóa mục tiêu. Hãy thử lại.');goals=goals.filter(goal=>String(goal.id)!==button.dataset.deleteGoal);tasks=tasks.map(task=>String(task.goalId)===button.dataset.deleteGoal?{...task,goalId:null}:task);drawGoals();showMessage('Đã xóa mục tiêu.');}catch(error){showMessage(error.message);}});
        drawGoals();
    }
    function renderStats() {
        const completed=tasks.filter(task=>task.completed).length,open=tasks.length-completed,overdue=tasks.filter(task=>!task.completed&&task.dueDate&&task.dueDate<dateKey(today)).length,rate=tasks.length?Math.round(completed/tasks.length*100):0;
        const groups={};tasks.forEach(task=>{const category=task.category||'Khác';groups[category]=(groups[category]||0)+1;});
        const nextDays=Array.from({length:7},(_,index)=>{const date=new Date();date.setDate(date.getDate()+index);const key=dateKey(date);return {label:new Intl.DateTimeFormat('vi-VN',{weekday:'short',day:'numeric'}).format(date),value:tasks.filter(task=>task.dueDate===key).length};});
        const weekStart=new Date(today.getFullYear(),today.getMonth(),today.getDate());weekStart.setDate(weekStart.getDate()-((weekStart.getDay()+6)%7));
        const weekStartKey=dateKey(weekStart),weekEnd=new Date(weekStart);weekEnd.setDate(weekEnd.getDate()+6);const weekEndKey=dateKey(weekEnd),monthPrefix=`${today.getFullYear()}-${pad(today.getMonth()+1)}`;
        const makePie=(title,periodTasks)=>{const done=periodTasks.filter(task=>task.completed).length,total=periodTasks.length,pct=total?Math.round(done/total*100):0;return `<section class="card panel pie-panel"><h2>${title}</h2><div class="pie-layout"><div class="pie-chart ${total?'':'is-empty'}" style="--done:${pct}%" role="img" aria-label="${title}: ${done} trên ${total} công việc hoàn thành"><span>${total?`${pct}%`:'—'}</span></div><div class="pie-details"><strong>${done} / ${total}</strong><span>công việc hoàn thành</span><div class="pie-legend"><span><i class="pie-dot done"></i>Hoàn thành (${done})</span><span><i class="pie-dot pending"></i>Chưa hoàn thành (${total-done})</span></div></div></div></section>`;};
        const weekTasks=tasks.filter(task=>task.dueDate&&task.dueDate>=weekStartKey&&task.dueDate<=weekEndKey),monthTasks=tasks.filter(task=>task.dueDate?.startsWith(monthPrefix));
        const categoryRows=Object.entries(groups).sort((a,b)=>b[1]-a[1]).map(([name,value])=>`<div class="bar-row"><span>${safe(name)}</span><div class="bar-track"><div class="bar-fill" style="width:${Math.max(4,value/Math.max(1,...Object.values(groups))*100)}%"></div></div><span class="bar-number">${value}</span></div>`).join('')||'<p class="subtitle">Chưa có dữ liệu công việc.</p>';
        const dateRows=nextDays.map(day=>`<div class="bar-row"><span>${safe(day.label)}</span><div class="bar-track"><div class="bar-fill" style="width:${Math.max(day.value?5:0,day.value/Math.max(1,...nextDays.map(item=>item.value))*100)}%"></div></div><span class="bar-number">${day.value}</span></div>`).join('');
        content.innerHTML=heading('TỔNG QUAN TIẾN ĐỘ','Thống kê công việc','Tổng hợp tự động từ danh sách công việc của bạn.')+
            `<div class="stats-grid"><article class="card stat-card"><div class="stat-label">Tổng công việc</div><div class="stat-value">${tasks.length}</div></article><article class="card stat-card"><div class="stat-label">Đã hoàn thành</div><div class="stat-value">${completed}</div></article><article class="card stat-card"><div class="stat-label">Đang thực hiện</div><div class="stat-value">${open}</div></article><article class="card stat-card"><div class="stat-label">Quá hạn</div><div class="stat-value">${overdue}</div></article></div><div class="stats-panels pie-panels">${makePie('Tiến độ trong tuần này',weekTasks)}${makePie('Tiến độ trong tháng này',monthTasks)}</div><div class="stats-panels"><section class="card panel"><h2>Tỉ lệ hoàn thành</h2><div class="stat-value">${rate}%</div><div class="progress" style="margin:12px 0"><span style="width:${rate}%"></span></div><p class="subtitle">${completed} trên ${tasks.length} công việc đã được đánh dấu hoàn thành.</p></section><section class="card panel"><h2>Công việc theo danh mục</h2>${categoryRows}</section><section class="card panel" style="grid-column:1/-1"><h2>Lịch đến hạn trong 7 ngày tới</h2>${dateRows}</section></div>`;
    }
    function renderThemes() {
        const topicPackages=[
            {id:'tet',name:'Tết',image:'/home/topics/tet.png'},
            {id:'noel',name:'Noel',image:'/home/topics/noel.png'},
            {id:'ocean',name:'Đại dương',image:'/home/topics/ocean.png'}
        ];
        const selectedTopic=topicPackages.find(topic=>topic.id===localStorage.getItem('dayflow-topic-background'));
        const locked=packageRank<2;
        content.innerHTML=heading('CÁ NHÂN HÓA','Chủ đề','Chọn một gói chủ đề để thay đổi hình nền hoặc chọn bảng màu hiển thị.')+
            (locked?'<div class="package-note"><strong>Chủ đề dành cho gói Chủ đề hoặc Toàn diện.</strong> Nâng cấp gói để mở khóa hình nền và bảng màu.</div>':'')+
            `<section class="theme-section"><h2>Gói chủ đề</h2><p class="subtitle">Hình nền được lấy từ bộ ảnh chủ đề và áp dụng cho các trang trong ứng dụng.</p><div class="theme-grid">${topicPackages.map(topic=>`<button class="card theme-card ${selectedTopic?.id===topic.id?'selected':''}" data-topic-id="${topic.id}" type="button" ${locked?'disabled':''} aria-pressed="${selectedTopic?.id===topic.id}"><div class="theme-preview topic-preview"><img class="topic-preview-image" src="${safe(topic.image)}" alt="Ảnh xem trước chủ đề ${safe(topic.name)}"></div><div class="theme-info"><span><strong>${safe(topic.name)}</strong><br><small>Gói chủ đề</small></span><small>${locked?'Cần nâng cấp':selectedTopic?.id===topic.id?'Đang dùng':'Chọn chủ đề'}</small></div></button>`).join('')}</div></section>`+
            `<section class="theme-section"><h2>Bảng màu</h2><div class="theme-grid">${skins.map(skin=>`<button class="card theme-card ${currentSkin.id===skin.id?'selected':''}" data-theme-id="${skin.id}" type="button" ${locked?'disabled':''}><div class="theme-preview" style="--preview-bg:${safe(skin.backgroundColor)};--preview-primary:${safe(skin.primaryColor)};--preview-secondary:${safe(skin.secondaryColor)};--preview-accent:${safe(skin.secondaryColor)};--preview-image:${safeImageUrl(skin.previewUrl||skin.backgroundImageUrl)}"><div class="preview-bar"></div><div class="preview-line"></div><div class="preview-line short"></div><div class="preview-chip"></div></div><div class="theme-info"><span><strong>${safe(skin.name)}${skin.isDefault?' · Mặc định':''}</strong><br><small>${safe(skin.code)}</small></span><small>${locked?'Cần nâng cấp':currentSkin.id===skin.id?'Đang dùng':'Chọn chủ đề'}</small></div></button>`).join('')}</div></section>`;
        document.querySelectorAll('[data-topic-id]').forEach(button=>button.addEventListener('click',()=>{
            const topic=topicPackages.find(item=>item.id===button.dataset.topicId);
            if(!topic)return;
            try {
                localStorage.setItem('dayflow-topic-background',topic.id);
            } catch(error) {
                showMessage('Không thể lưu gói chủ đề trên trình duyệt này.');
                return;
            }
            document.documentElement.dataset.topicBackground=topic.id;
            document.documentElement.style.setProperty('--topic-background-image',safeImageUrl(topic.image));
            renderThemes();
            showMessage(`Đã áp dụng gói chủ đề ${topic.name}.`);
        }));
        document.querySelectorAll('[data-theme-id]').forEach(button=>button.addEventListener('click',async()=>{
            const skinId=Number(button.dataset.themeId);
            await fetch('/api/auth/csrf',{credentials:'same-origin'});
            const csrfCookie=document.cookie.split('; ').find(value=>value.startsWith('XSRF-TOKEN='));
            const response=await fetch('/api/auth/me/skin',{method:'PUT',credentials:'same-origin',headers:{'Content-Type':'application/json','X-XSRF-TOKEN':csrfCookie?decodeURIComponent(csrfCookie.slice('XSRF-TOKEN='.length)):''},body:JSON.stringify({skinId})});
            if(!response.ok){showMessage('Không thể lưu chủ đề. Hãy thử lại.');return;}
            localStorage.removeItem('dayflow-topic-background');
            delete document.documentElement.dataset.topicBackground;
            document.documentElement.style.removeProperty('--topic-background-image');
            currentSkin=await response.json();applySkin(currentSkin);renderThemes();showMessage(`Đã lưu chủ đề ${currentSkin.name} cho tài khoản của bạn.`);
        }));
    }
    async function renderPackages() {
        content.innerHTML=heading('GÓI NẠP','Chọn gói phù hợp','Mở khóa thêm chức năng cho tài khoản Dayflow của bạn.')+'<div class="card panel">Đang tải danh sách gói…</div>';
        try {
            const response=await fetch('/api/packages',{credentials:'same-origin'});
            if(!response.ok)throw new Error('Không thể tải danh sách gói từ máy chủ.');
            const overview=await response.json();
            const money=value=>`${Number(value||0).toLocaleString('vi-VN')} ₫`;
            const active=overview.currentPlan;
            content.innerHTML=heading('GÓI NẠP','Chọn gói phù hợp','Mở khóa thêm chức năng cho tài khoản Dayflow của bạn.')+
                `<div class="package-note"><strong>Gói hiện tại: ${safe(active.name)}.</strong> Quét mã QR để thanh toán. Khi email được cấu hình, yêu cầu xác nhận sẽ gửi tới Gmail và gói mở sau khi xác nhận đã nhận tiền.</div><div class="package-grid">${overview.packages.map((plan,index)=>{const owned=plan.rank<=active.rank;const label=owned?(plan.rank===active.rank&&plan.priceVnd===0?'Đang sử dụng':'Đã mua'):'Mua gói';return `<article class="card package-card ${index===2?'featured':''}" ${plan.code===active.code?'aria-current="true"':''}>${index===2?'<span class="package-badge">Đầy đủ tính năng</span>':''}<h2>${safe(plan.name)}</h2><div class="package-description">${safe(plan.features.join(' · '))}</div><div class="package-credits">${plan.priceVnd===0?'Miễn phí':money(plan.priceVnd)}</div><div class="package-price">${plan.priceVnd===0?'Không mất phí':'Thanh toán một lần · QR ngân hàng/MoMo'}</div><ul>${plan.features.map(feature=>`<li>${safe(feature)}</li>`).join('')}</ul><button class="button ${owned?'':'primary'}" type="button" data-buy-package="${safe(plan.code)}" ${owned?'disabled':''}>${label}</button></article>`}).join('')}</div>`;
            content.querySelectorAll('[data-buy-package]').forEach(button=>button.addEventListener('click',async()=>{
                const plan=overview.packages.find(item=>item.code===button.dataset.buyPackage);
                if(!plan)return;
                button.disabled=true;
                button.textContent='Đang xử lý…';
                showMessage('Đang tạo yêu cầu thanh toán…');
                try {
                    const headers=await csrfHeaders();
                    const result=await fetch(plan.priceVnd===0?'/api/packages/purchase':'/api/packages/checkout',{method:'POST',credentials:'same-origin',headers,body:JSON.stringify({planCode:plan.code})});
                    if(!result.ok){
                        if(result.status===401)throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại rồi thử mua gói.');
                        if(result.status===403)throw new Error('Phiên bảo mật đã hết hạn. Tải lại trang rồi thử mua gói.');
                        const problem=await result.json().catch(()=>null);
                        throw new Error(problem?.message||problem?.detail||'Không thể tạo yêu cầu mua gói. Vui lòng thử lại.');
                    }
                    if(plan.priceVnd>0){showPackagePayment(await result.json(),button);return;}
                    showMessage(`Đã kích hoạt gói ${plan.name} cho tài khoản của bạn.`);
                    window.setTimeout(()=>location.reload(),700);
                } catch(error) { showMessage(error.message);button.disabled=false;button.textContent='Mua gói'; }
            }));
        } catch(error) { content.innerHTML=heading('GÓI NẠP','Chọn gói phù hợp','Không tải được danh sách gói.')+`<div class="card package-note">${safe(error.message)}</div>`; }
    }
    function showPackagePayment(checkout, buyButton) {
        const dialog=document.createElement('dialog');
        dialog.className='package-payment-dialog';
        const emailMessage=checkout.emailSent
            ?'Yêu cầu xác nhận đã được gửi tới Gmail dieu02112006@gmail.com. Gói sẽ mở sau khi xác nhận đã nhận tiền.'
            :'Chưa gửi được email xác nhận. Mã QR vẫn dùng để thanh toán; cần cấu hình Gmail SMTP để nhận yêu cầu xác nhận và mở gói.';
        dialog.innerHTML=`<div class="package-payment-content"><button class="package-payment-close" type="button" aria-label="Đóng">×</button><h2>Thanh toán gói ${safe(checkout.planName)}</h2><p>Quét mã bằng ứng dụng ngân hàng hoặc MoMo và chuyển đúng số tiền bên dưới.</p><img class="package-payment-qr" src="${safe(checkout.qrImageUrl)}" alt="Mã QR thanh toán gói Dayflow"><strong class="package-payment-amount">${Number(checkout.amountVnd||0).toLocaleString('vi-VN')} ₫</strong><p class="package-payment-hint">Nội dung chuyển khoản: <strong>DAYFLOW ${safe(checkout.transactionRef)}</strong><br>${safe(emailMessage)}</p><div class="package-payment-status" role="status" aria-live="polite">Đang chờ xác nhận thanh toán…</div></div>`;
        document.body.appendChild(dialog);
        dialog.showModal();
        const statusNode=dialog.querySelector('.package-payment-status');
        let checking=false;
        const check=async()=>{
            if(checking||!dialog.open)return;
            checking=true;
            try{
                const response=await fetch(`/api/packages/orders/${encodeURIComponent(checkout.transactionRef)}`,{credentials:'same-origin'});
                if(!response.ok)return;
                const payment=await response.json();
                if(payment.status==='PAID'){
                    statusNode.textContent='Thanh toán thành công — gói đã được mở!';
                    window.clearInterval(timer);
                    window.setTimeout(()=>location.reload(),1100);
                }else if(payment.status==='FAILED'){
                    statusNode.textContent='Thanh toán thất bại — gói chưa được mở.';
                    window.clearInterval(timer);
                }
            }catch(_){/* kiểm tra lại ở lần tiếp theo */}
            finally{checking=false;}
        };
        const timer=window.setInterval(check,2500);
        dialog.addEventListener('close',()=>{window.clearInterval(timer);dialog.remove();buyButton.disabled=false;buyButton.textContent='Mua gói';});
        dialog.querySelector('.package-payment-close').addEventListener('click',()=>dialog.close());
        check();
    }
    window.addEventListener('dayflow:task-created',event=>{
        const saved=event.detail;
        if(!saved?.id||tasks.some(task=>String(task.id)===String(saved.id)))return;
        tasks.unshift(saved);
        if(view==='calendar')renderCalendar();
        if(view==='stats')renderStats();
        if(view==='goals'){
            const list=document.getElementById('goal-list');
            if(list)list.querySelectorAll('.goal-card').forEach(card=>card.remove());
            renderGoals();
        }
    });
    window.addEventListener('dayflow:task-updated',event=>{
        const changed=event.detail;
        if(!changed?.id)return;
        const index=tasks.findIndex(task=>String(task.id)===String(changed.id));
        if(changed.deleted){if(index>=0)tasks.splice(index,1);}
        else if(index>=0)tasks[index]={...tasks[index],...changed};
        else tasks.unshift(changed);
        if(view==='calendar')renderCalendar();
        if(view==='stats')renderStats();
        if(view==='goals')renderGoals();
    });
    window.addEventListener('dayflow:goal-created',event=>{
        const saved=event.detail;
        if(!saved?.id||goals.some(goal=>String(goal.id)===String(saved.id)))return;
        goals.unshift(saved);
        if(view==='goals')renderGoals();
    });
    if(view==='calendar')renderCalendar();
    if(view==='goals')renderGoals();
    if(view==='stats')renderStats();
    if(view==='themes')renderThemes();
    if(view==='packages')renderPackages();
})();
