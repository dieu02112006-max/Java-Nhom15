(async () => {
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar || document.querySelector('.dayflow-assistant')) return;
    try {
        const packageResponse = await fetch('/api/packages/me', {credentials:'same-origin'});
        if (!packageResponse.ok || Number((await packageResponse.json()).rank) < 3) return;
    } catch (_) { return; }

    const shell = document.createElement('section');
    shell.className = 'dayflow-assistant';
    shell.innerHTML = `
        <button class="assistant-pet" type="button" aria-label="Mở trò chuyện với Chibi" title="Trò chuyện với Chibi">
            <img class="assistant-dog assistant-dog-sleep" src="/assistant/dog-sleep.png" alt="Chú chó đang ngủ">
            <img class="assistant-dog assistant-dog-sit" src="/assistant/dog-sit.png" alt="Chú chó đang ngồi">
            <span class="assistant-unread" aria-hidden="true"></span>
        </button>
        <section class="assistant-window" aria-label="Trò chuyện với Chibi" aria-hidden="true">
            <header class="assistant-header"><img src="/assistant/dog-sit.png" alt=""><div><strong>Chibi</strong><small>Trợ lý Dayflow</small></div><button class="assistant-close" type="button" aria-label="Đóng trò chuyện">×</button></header>
            <div class="assistant-messages" aria-live="polite"><div class="assistant-message assistant-reply">Chào bạn, mình là Chibi. Bạn có thể nhờ mình ghi, sửa hoặc xóa công việc và khoản chi tiêu bằng lời nói tự nhiên.</div></div>
            <form class="assistant-form"><input class="assistant-input" aria-label="Tin nhắn" placeholder="Nhắn cho Chibi…" autocomplete="off" maxlength="500"><button type="submit" aria-label="Gửi tin nhắn">➤</button></form>
            <div class="assistant-hint">Ví dụ: “Tạo mục tiêu tập thể dục”</div>
        </section>
        <div class="assistant-toast" role="status" aria-live="polite"></div>`;
    sidebar.appendChild(shell);

    const pet = shell.querySelector('.assistant-pet');
    const windowEl = shell.querySelector('.assistant-window');
    const messages = shell.querySelector('.assistant-messages');
    const form = shell.querySelector('.assistant-form');
    const input = shell.querySelector('.assistant-input');
    const toast = shell.querySelector('.assistant-toast');
    let toastTimer;
    let reminderPetTimer;
    let pendingNotices = [];
    let deadlineReminderText = '';

    const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
    const normalize = value => String(value ?? '').toLocaleLowerCase('vi').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
    const dateKey = date => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
    const money = value => `${Number(value).toLocaleString('vi-VN')} ₫`;

    function addMessage(text, who = 'reply') {
        const node = document.createElement('div');
        node.className = `assistant-message assistant-${who}`;
        node.textContent = text;
        messages.appendChild(node);
        messages.scrollTop = messages.scrollHeight;
    }
    function showToast(text) {
        if(deadlineReminderText)return;
        toast.textContent = shortenNotice(text);
        toast.dataset.kind='temporary';
        toast.classList.add('show');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => toast.classList.remove('show'), 6500);
        shell.classList.add('reminder-active');
        clearTimeout(reminderPetTimer);
        reminderPetTimer=setTimeout(()=>{if(!deadlineReminderText)shell.classList.remove('reminder-active');},6500);
    }
    function showDeadlineReminder(text) {
        deadlineReminderText=text;
        clearTimeout(toastTimer);
        clearTimeout(reminderPetTimer);
        toast.dataset.kind='deadline';
        toast.textContent=shortenNotice(text);
        toast.classList.add('show');
        shell.classList.add('reminder-active');
    }
    function shortenNotice(text) {
        const task = text.match(/Công việc “(.+?)”/);
        if (/quá hạn/i.test(text)) return task ? `“${task[1]}” đã quá hạn` : 'Có công việc đã quá hạn';
        if (/sắp đến hạn/i.test(text)) return task ? `“${task[1]}” sắp đến hạn` : 'Có công việc sắp đến hạn';
        return text.length > 72 ? `${text.slice(0, 69).trimEnd()}…` : text;
    }
    function clearDeadlineReminder() {
        deadlineReminderText='';
        if(toast.dataset.kind==='deadline'){
            toast.dataset.kind='';
            toast.classList.remove('show');
        }
        if(!shell.classList.contains('open'))shell.classList.remove('reminder-active');
    }
    function setOpen(open) {
        shell.classList.toggle('open', open);
        windowEl.setAttribute('aria-hidden', String(!open));
        if (open) {
            shell.querySelector('.assistant-unread').classList.remove('visible');
            input.focus();
            if ('Notification' in window && Notification.permission === 'default') {
                Notification.requestPermission().catch(() => {});
            }
            pendingNotices.forEach(text => addMessage(text));
            pendingNotices = [];
        }
    }
    pet.addEventListener('click', () => setOpen(!shell.classList.contains('open')));
    shell.querySelector('.assistant-close').addEventListener('click', () => setOpen(false));

    async function requestJson(path, method='GET', body) {
        const headers={};
        if(!['GET','HEAD','OPTIONS'].includes(method.toUpperCase())){
            await fetch('/api/auth/csrf',{credentials:'same-origin'});
            const cookie=document.cookie.split('; ').find(value=>value.startsWith('XSRF-TOKEN='));
            headers['X-XSRF-TOKEN']=cookie?decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)):'';
        }
        if(body!==undefined)headers['Content-Type']='application/json';
        const response=await fetch(path,{method,credentials:'same-origin',headers,body:body===undefined?undefined:JSON.stringify(body)});
        if(!response.ok){const problem=await response.json().catch(()=>null);throw new Error(problem?.detail||problem?.message||`Máy chủ trả về lỗi ${response.status}.`);}
        return response.status===204?null:response.json();
    }
    async function postJson(path, body) {
        await fetch('/api/auth/csrf', {credentials:'same-origin'});
        const cookie = document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='));
        const response = await fetch(path, {
            method:'POST', credentials:'same-origin',
            headers:{'Content-Type':'application/json','X-XSRF-TOKEN':cookie?decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)):''},
            body:JSON.stringify(body)
        });
        if (!response.ok) {
            const problem = await response.json().catch(() => null);
            throw new Error(problem?.detail || problem?.message || `Máy chủ trả về lỗi ${response.status}.`);
        }
        return response.status === 204 ? null : response.json();
    }
    function dueDateFrom(text) {
        const plain=normalize(text), now=new Date();
        const today=new Date(now.getFullYear(),now.getMonth(),now.getDate());
        let date=null, explicitDate=false;
        if (/\b(hom nay|sang nay|chieu nay|toi nay)\b/.test(plain)) { date=today; explicitDate=true; }
        else if (/\b(ngay mai|mai)\b/.test(plain)) { date=new Date(today); date.setDate(date.getDate()+1); explicitDate=true; }
        else if (/\bngay kia\b/.test(plain)) { date=new Date(today); date.setDate(date.getDate()+2); explicitDate=true; }
        else {
            const weekday=plain.match(/\bthu\s*([2-7])\b|\bchu nhat\b|\bthu\s*(hai|ba|tu|nam|sau|bay)\b/);
            if(weekday){
                const days={hai:1,ba:2,tu:3,nam:4,sau:5,bay:6};
                const target=weekday[0].includes('chu nhat')?0:weekday[2]?days[weekday[2]]:Number(weekday[1])-1;
                const delta=(target-now.getDay()+7)%7||7;
                date=new Date(today);date.setDate(date.getDate()+delta);explicitDate=true;
            } else {
                const numeric=text.match(/(?:ngày\s*)?(\d{1,2})[\/.-](\d{1,2})(?:[\/.-](\d{2,4}))?/i);
                if(numeric){let year=numeric[3]?Number(numeric[3]):now.getFullYear();if(year<100)year+=2000;date=new Date(year,Number(numeric[2])-1,Number(numeric[1]));explicitDate=true;}
            }
        }
        const time=plain.match(/(?:\bluc\s*|\bvao luc\s*)?(\d{1,2})\s*(?:h|:|gio)\s*(\d{1,2})?\s*(sang|chieu|toi)?\b/);
        if(!date&&!time)return null;
        if(!date)date=new Date(today);
        const hourValue=time?Number(time[1]):9, minute=time?.[2]?Number(time[2]):0;
        if(hourValue>23||minute>59)return null;
        let hour=hourValue;
        if(time?.[3]==='chieu'||time?.[3]==='toi'){if(hour<12)hour+=12;}
        else if(!time?.[3]&&hour>0&&hour<=6)hour+=12;
        date.setHours(hour,minute,0,0);
        if(!explicitDate&&time&&date<=now)date.setDate(date.getDate()+1);
        return `${dateKey(date)}T${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}:00`;
    }
    function extractLocation(text) {
        const match=String(text||'').match(/(?:^|\s)(?:ở|tại|địa điểm(?:\s+là)?)\s+(.+)$/i);
        if(!match)return null;
        const location=match[1]
            .replace(/\b(?:lúc|vào lúc)\s*\d{1,2}\s*(?:h|:|giờ)\s*\d{0,2}\s*(?:sáng|chiều|tối)?/gi,' ')
            .replace(/\b(?:hôm nay|ngày mai|ngày kia|sáng nay|chiều nay|tối nay|mai|cuối tuần|tuần sau|thứ\s*[2-7]|chủ nhật)\b/gi,' ')
            .replace(/[,.!?;]+$/,'').replace(/\s+/g,' ').trim();
        return location||null;
    }
    function extractTitle(text,isGoal=false) {
        let title=String(text||'');
        title=title.replace(/\s+(?:ở|tại|địa điểm(?:\s+là)?)\s+.+$/i,' ');
        title=title.replace(/(?:ngày\s*)?\d{1,2}[\/.-]\d{1,2}(?:[\/.-]\d{2,4})?/gi,' ');
        title=title.replace(/\b(?:lúc|vào lúc)\s*\d{1,2}\s*(?:h|:|giờ)\s*\d{0,2}\s*(?:sáng|chiều|tối)?/gi,' ');
        title=title.replace(/\b\d{1,2}\s*(?:h|:|giờ)\s*\d{0,2}\s*(?:sáng|chiều|tối)?\b/gi,' ');
        title=title.replace(/\b(?:hôm nay|ngày mai|ngày kia|sáng nay|chiều nay|tối nay|mai|cuối tuần|tuần sau|thứ\s*[2-7]|chủ nhật)\b/gi,' ');
        title=title.trim();
        title=title.replace(/^(?:(?:tôi|mình|tớ|em|anh|chị)\s+(?:muốn|cần|phải|sẽ|định|dự định)\s+)?(?:(?:bạn|hãy|giúp tôi|giúp mình|cho tôi|cho mình)\s+)*(?:(?:thêm|tạo|đặt|ghi lại|lên lịch|nhắc tôi|nhắc mình|nhớ)\s+)*(?:một\s+)?/i,'');
        title=title.replace(/^(?:công việc|việc cần làm|task|mục tiêu|goal)\s*[:\-]?\s*/i,'');
        title=title.replace(/^(?:(?:tôi|mình|tớ|em|anh|chị)\s+)*(?:(?:phải|cần|muốn|sẽ|định|dự định|có thể)\s+)+/i,'');
        if(isGoal)title=title.replace(/^(?:mục tiêu|goal)\s*[:\-]?\s*/i,'');
        return title.replace(/[.!?]+$/,'').replace(/\s+/g,' ').trim();
    }
    function findMatches(rows, query, label) {
        const needle=normalize(query).trim();
        if(!needle)return [];
        const scored=rows.map(row=>{
            const title=normalize(label(row));
            return {row,score:title===needle?3:title.includes(needle)||needle.includes(title)?2:0};
        }).filter(item=>item.score>0);
        const best=Math.max(0,...scored.map(item=>item.score));
        return scored.filter(item=>item.score===best).map(item=>item.row);
    }
    function parseTargetAndReplacement(raw, verb) {
        const match=raw.match(new RegExp(`(?:${verb})\\s+(?:(?:công việc|việc|khoản chi|chi tiêu)\\s+)?(.+?)\\s+(?:thành|là|tên là)\\s+(.+)$`,'i'));
        return match?{target:match[1].replace(/[“”"']/g,'').trim(),replacement:match[2].replace(/[“”"']/g,'').trim()}:null;
    }
    async function editOrDeleteTask(raw, plain, deleting) {
        const tasks=await requestJson('/api/todo/tasks');
        const dateEdit=!deleting&&/\b(?:doi|sua) han\b/.test(plain);
        const parsed=deleting
            ? raw.match(/(?:xóa|xoá|bỏ)\s+(?:(?:công việc|việc)\s+)?(.+)$/i)
            : parseTargetAndReplacement(raw,dateEdit?'(?!)':'(?:sửa|đổi)');
        let target=parsed?.[deleting?1:'target'];
        if(!deleting&&parsed?.target)target=extractTitle(parsed.target);
        if(dateEdit){
            const m=raw.match(/(?:đổi|sửa)\s+hạn\s+(?:(?:công việc|việc)\s+)?(.+?)\s+(?:sang|vào|đến)\s+/i);
            target=m?.[1]?.trim();
        }
        if(!target){
            const targetMatch=raw.match(/(?:sửa|đổi|xóa|xoá|bỏ)\s+(?:(?:công việc|việc)\s+)?(.+)$/i);
            target=targetMatch?.[1];
        }
        if(target)target=target.replace(/\s+(?:sang|vào|đến hạn)\s+(?:hôm nay|ngày mai|mai|ngày kia|\d{1,2}h.*)$/i,'').trim();
        const matches=findMatches(tasks,target,task=>task.title);
        if(!matches.length)return `Mình không tìm thấy công việc “${target||''}”. Bạn kiểm tra lại tên giúp mình nhé.`;
        if(matches.length>1)return `Mình tìm thấy ${matches.length} công việc gần giống “${target}”. Bạn ghi rõ tên công việc hơn để mình không sửa nhầm nhé.`;
        const task=matches[0];
        if(deleting){
            await requestJson(`/api/todo/tasks/${task.id}`,'DELETE');
            window.dispatchEvent(new CustomEvent('dayflow:task-updated',{detail:{id:task.id,deleted:true}}));
            return `Mình đã xóa công việc “${task.title}”.`;
        }
        const replacement=parsed?.replacement?extractTitle(parsed.replacement):'';
        const dueDate=(parsed?.replacement?dueDateFrom(parsed.replacement):null)||dueDateFrom(raw);
        const title=replacement||task.title;
        if(!replacement&&!dueDate)return 'Bạn muốn đổi tên hay hạn hoàn thành của công việc này? Ví dụ: “Sửa việc đi học thành đi làm” hoặc “Dời việc đi học sang ngày mai lúc 14h”.';
        const updated=await requestJson(`/api/todo/tasks/${task.id}`,'PUT',{...task,title,dueDate:dueDate||task.dueDate});
        window.dispatchEvent(new CustomEvent('dayflow:task-updated',{detail:updated}));
        return `Mình đã cập nhật công việc “${task.title}”${replacement?` thành “${replacement}”`:''}${dueDate?' và đổi hạn hoàn thành':''}.`;
    }
    async function editOrDeleteExpense(raw, deleting) {
        const auth=await requestJson('/api/auth/me');
        const userId=auth.id??auth.userId;
        if(!userId)throw new Error('Mình chưa xác định được tài khoản để truy cập chi tiêu.');
        const now=new Date(), months=[];
        for(let offset=0;offset<6;offset++){
            const date=new Date(now.getFullYear(),now.getMonth()-offset,1);
            months.push(`${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}`);
        }
        const batches=await Promise.all(months.map(month=>requestJson(`/api/finance/transactions?userId=${encodeURIComponent(userId)}&month=${month}`)));
        const expenses=batches.flat().filter(row=>row.type==='expense');
        const parsed=deleting
            ? raw.match(/(?:xóa|xoá|bỏ)\s+(?:(?:khoản chi|chi tiêu)\s+)?(.+)$/i)
            : parseTargetAndReplacement(raw,'(?:sửa|đổi)');
        let target=parsed?.[deleting?1:'target'];
        if(!target){const m=raw.match(/(?:sửa|đổi|xóa|xoá|bỏ)\s+(?:(?:khoản chi|chi tiêu)\s+)?(.+)$/i);target=m?.[1];}
        const matches=findMatches(expenses,target,row=>`${row.note||''} ${row.categoryName||''}`);
        if(!matches.length)return `Mình không tìm thấy khoản chi “${target||''}” trong 6 tháng gần đây.`;
        if(matches.length>1)return `Có ${matches.length} khoản chi gần giống “${target}”. Bạn thêm tên danh mục hoặc số tiền để mình xác định đúng khoản nhé.`;
        const row=matches[0];
        if(deleting){
            await requestJson(`/api/finance/transactions/${row.id}?userId=${encodeURIComponent(userId)}`,'DELETE');
            window.dispatchEvent(new CustomEvent('dayflow:expense-updated',{detail:{id:row.id,deleted:true}}));
            return `Mình đã xóa khoản chi “${row.note||row.categoryName}” ${money(row.amount)}.`;
        }
        const amountMatch=normalize(raw).match(/(?:thành|còn|là)\s*(\d[\d.,]*)\s*(?:đ|dong|nghin|ngan|trieu)?/);
        let amount=row.amount;
        if(amountMatch){amount=Number(amountMatch[1].replace(/[.,]/g,''));if(/trieu/.test(normalize(raw)))amount*=1_000_000;else if(/(?:nghin|ngan)/.test(normalize(raw)))amount*=1_000;}
        const replacement=parsed?.replacement?parsed.replacement.replace(/\s+(?:lúc|vào lúc)\s+\d{1,2}\s*(?:h|:|giờ)\s*\d{0,2}.*$/i,'').trim():'';
        const note=replacement||row.note;
        if(!amountMatch&&!replacement)return 'Bạn muốn đổi tên ghi chú hay số tiền của khoản chi này?';
        await requestJson(`/api/finance/transactions/${row.id}`,'PUT',{userId,walletId:row.walletId,categoryId:row.categoryId,type:row.type,amount,transactionDate:row.transactionDate,note});
        window.dispatchEvent(new CustomEvent('dayflow:expense-updated',{detail:{id:row.id}}));
        return `Mình đã cập nhật khoản chi${replacement?` thành “${replacement}”`:''}${amountMatch?` thành ${money(amount)}`:''}.`;
    }
    function parseExpenseAmount(raw) {
        const amountSource=String(raw)
            .replace(/\b\d{1,2}\s*(?:h|:)\s*\d{1,2}\b/gi,' ')
            .replace(/\b\d{1,2}\s*h\b/gi,' ')
            .replace(/\b\d{1,2}[\/.-]\d{1,2}(?:[\/.-]\d{2,4})?\b/g,' ');
        const matches=[...amountSource.matchAll(/(\d[\d.,]*)\s*(k|nghìn|ngàn|triệu|đồng|đ)?/gi)];
        if(!matches.length)return null;
        const withUnit=matches.filter(match=>match[2]);
        const match=(withUnit.length?withUnit:matches).at(-1);
        let amount=Number(match[1].replace(/[.,]/g,''));
        const unit=normalize(match[2]||'');
        if(unit==='k'||unit==='nghin'||unit==='ngan')amount*=1000;
        if(unit==='trieu')amount*=1000000;
        return Number.isFinite(amount)&&amount>0?{amount,token:match[0]}:null;
    }
    async function createExpense(raw, parsedAmount) {
        const auth=await requestJson('/api/auth/me');
        const userId=auth.id??auth.userId;
        if(!userId)throw new Error('Mình chưa xác định được tài khoản để ghi chi tiêu.');
        const [wallets,categories]=await Promise.all([
            requestJson('/api/finance/wallets'),
            requestJson('/api/finance/categories?type=expense')
        ]);
        if(!wallets.length)return 'Bạn chưa có ví đang hoạt động nên mình chưa thể ghi khoản chi.';
        let note=raw
            .replace(/^(?:(?:tôi|mình|tớ|em|anh|chị)\s+)*(?:đã\s+)?(?:thêm|ghi|tạo|lưu|nhập|chi|mua)\s+/i,'')
            .replace(/^(?:(?:một\s+)?(?:khoản\s+)?(?:chi tiêu|chi phí|khoản chi)\s*)/i,'')
            .replace(parsedAmount.token,' ')
            .replace(/\b(?:hết|giá|trị giá|với giá|tốn|đồng|vnđ|vnd)\b/gi,' ')
            .replace(/\s+/g,' ').trim().replace(/^[,.:;\-\s]+|[,.:;\-\s]+$/g,'');
        if(!note)note='Chi tiêu từ Chibi';
        const normalized=normalize(raw);
        const category=categories.find(item=>normalized.includes(normalize(item.name)))||null;
        const now=new Date();
        const transactionDate=now.getFullYear()+'-'+String(now.getMonth()+1).padStart(2,'0')+'-'+String(now.getDate()).padStart(2,'0')+'T'+String(now.getHours()).padStart(2,'0')+':'+String(now.getMinutes()).padStart(2,'0')+':'+String(now.getSeconds()).padStart(2,'0');
        const saved=await postJson('/api/finance/transactions',{
            userId,walletId:wallets[0].id,categoryId:category?.id??null,type:'expense',
            amount:parsedAmount.amount,transactionDate,note
        });
        window.dispatchEvent(new CustomEvent('dayflow:expense-updated',{detail:saved}));
        window.dayflowAssistant?.warnExpense(parsedAmount.amount);
        return 'Mình đã ghi khoản chi “'+note+'” '+money(parsedAmount.amount)+(category?' vào danh mục '+category.name:'')+'.';
    }
    async function handleMessage(raw) {
        const plain=normalize(raw);
        const deleting=/\b(xoa|xoa bo|bo)\b/.test(plain);
        const editing=/\b(sua|doi)\b/.test(plain);
        const expenseIntent=/\b(chi tieu|khoan chi|chi phi|tien)\b/.test(plain);
        if((deleting||editing)&&expenseIntent)return editOrDeleteExpense(raw,deleting);
        if((deleting||editing)&&/\b(cong viec|viec|task)\b/.test(plain))return editOrDeleteTask(raw,plain,deleting);
        const explicitCreate=/\b(them|tao|dat|ghi lai|len lich|nhac toi|nho toi)\b/.test(plain);
        const expenseAmount=parseExpenseAmount(raw);
        const expenseCreateIntent=(expenseIntent&&!!expenseAmount&&(explicitCreate||/\bchi\b/.test(plain)))||(/\bmua\b/.test(plain)&&!!expenseAmount);
        if(expenseCreateIntent)return createExpense(raw,expenseAmount);
        const isGoal=/\b(muc tieu|goal)\b/.test(plain);
        const isTask=/\b(cong viec|viec can lam|task|viec)\b/.test(plain);
        const personalPlan=/\b(toi|minh|to|em|anh|chi)\s+(?:muon|can|phai|se|dinh|du dinh)\b/.test(plain);
        const actionVerb=/\b(di|hoc|lam|hop|nop|mua|gui|goi|tap|don|thanh toan|chuan bi|ve sinh|doc sach|viet|nau|kham|hen|bao cao)\b/.test(plain);
        const hasDateOrTime=/\b(hom nay|sang nay|chieu nay|toi nay|ngay mai|ngay kia|mai|cuoi tuan|tuan sau|thu\s*[2-7]|chu nhat|\d{1,2}\s*(?:h|gio|:)\s*\d*)\b/.test(plain);
        const goalIntent=isGoal&&(explicitCreate||personalPlan||/\bmuc tieu(?: cua toi| cua minh)?\s+(?:la|:)/.test(plain));
        const taskIntent=(explicitCreate&&(isTask||actionVerb))||(personalPlan&&actionVerb)||(hasDateOrTime&&actionVerb);
        if(goalIntent){
            const title=extractTitle(raw,true);
            if(!title)return 'Bạn muốn đặt tên mục tiêu là gì? Ví dụ: “Tạo mục tiêu tập thể dục”.';
            const goal=await postJson('/api/todo/goals',{title});
            window.dispatchEvent(new CustomEvent('dayflow:goal-created',{detail:goal}));
            return `Mình đã tạo mục tiêu “${title}” cho bạn.`;
        }
        if(taskIntent){
            const title=extractTitle(raw);
            if(!title||/^\d{1,2}(?:h\d{1,2}|:\d{2})?$/i.test(title))return 'Mình chưa xác định được tên công việc nên chưa lưu. Bạn cho mình biết cần làm việc gì nhé.';
            const dueDate=dueDateFrom(raw);
            const location=extractLocation(raw);
            const listsResponse=await fetch('/api/todo/task-lists',{credentials:'same-origin'});
            if(!listsResponse.ok)throw new Error('Mình chưa tải được danh sách công việc.');
            const lists=await listsResponse.json();
            const saved=await postJson('/api/todo/tasks',{
                listId:lists[0]?.id??null,parentTaskId:null,goalId:null,title,
                description:null,category:'Cá nhân',location,status:'todo',priority:'medium',
                dueDate,completedAt:null,estimatedCost:null,recurrenceRule:null,position:0
            });
            window.dispatchEvent(new CustomEvent('dayflow:task-created',{detail:saved}));
            return `Mình đã thêm công việc “${title}”${dueDate?' với hạn '+new Intl.DateTimeFormat('vi-VN',{day:'numeric',month:'long',hour:'2-digit',minute:'2-digit'}).format(new Date(dueDate)):''}${location?` tại ${location}`:''}.`;
        }
        if(/\b(chao|xin chao|hello|hi|cam on)\b/.test(plain))return 'Chào bạn! Mình có thể thêm công việc, tạo mục tiêu hoặc nhắc bạn về hạn hoàn thành.';
        return 'Mình có thể giúp thêm công việc hoặc tạo mục tiêu. Ví dụ: “Hôm nay tôi phải đi học lúc 13h30” hoặc “Tạo mục tiêu tập thể dục”.';
    }
    form.addEventListener('submit',async event=>{
        event.preventDefault();
        const text=input.value.trim();
        if (!text) return;
        addMessage(text,'user');
        input.value='';
        const waiting=document.createElement('div');waiting.className='assistant-message assistant-reply assistant-thinking';waiting.textContent='Để mình xem nhé…';messages.appendChild(waiting);messages.scrollTop=messages.scrollHeight;
        try { const answer=await handleMessage(text); waiting.remove(); addMessage(answer); }
        catch(error) { waiting.remove(); addMessage(`Mình chưa làm được: ${error.message}`); }
    });

    const expenseReminders = [
        amount => `Khoản chi ${money(amount)} hơi cao đó! Bạn cân nhắc xem có thật sự cần không nhé.`,
        amount => `Úi, ${money(amount)} cho một khoản chi! Nhớ để dành một chút cho ngày mai nha.`,
        amount => `Chibi thấy khoản ${money(amount)} này vượt mốc 200 rồi. Mình cùng giữ ngân sách nhé!`,
        amount => `Ví vừa nhẹ đi ${money(amount)} đó. Nghỉ một nhịp trước khi mua tiếp nhé!`,
        amount => `${money(amount)} là khoản chi khá lớn. Bạn xem lại ngân sách tháng một chút nha.`
    ];
    window.dayflowAssistant = {
        warnExpense(amount) {
            if (Number(amount) <= 200) return;
            const text=expenseReminders[Math.floor(Math.random()*expenseReminders.length)](Number(amount));
            showToast(text);
            shell.querySelector('.assistant-unread').classList.add('visible');
            pendingNotices.push(text);
            if (shell.classList.contains('open')) { addMessage(text); pendingNotices.pop(); }
        }
    };

    async function checkUpcomingTasks() {
        try {
            const response=await fetch('/api/todo/tasks',{credentials:'same-origin'});
            if (!response.ok) return;
            const tasks=await response.json();
            const now=new Date();
            const upcoming=[];
            tasks.filter(task=>task.status!=='done'&&task.dueDate).forEach(task=>{
                const raw=String(task.dueDate);
                let due=new Date(raw.length===10?`${raw}T23:59:00`:raw);
                if (Number.isNaN(due.getTime())) return;
                const remaining=due-now;
                if (remaining>60*60*1000) return;
                upcoming.push({task,raw,remaining});
            });
            if(!upcoming.length){clearDeadlineReminder();return;}
            const reminderText=upcoming.length===1
                ? reminderMessage(upcoming[0])
                : `Bạn còn ${upcoming.length} công việc sắp đến hạn chưa hoàn thành. ${reminderMessage(upcoming[0])}`;
            showDeadlineReminder(reminderText);
            upcoming.forEach(({task,raw,remaining})=>{
                const key=`dayflow-reminder-v3-${task.id}-${raw}`;
                const lastReminder=Number(localStorage.getItem(key)||0);
                if (lastReminder&&now.getTime()-lastReminder<5*60*1000) return;
                localStorage.setItem(key,String(now.getTime()));
                shell.querySelector('.assistant-unread').classList.add('visible');
                const text=reminderMessage({task,remaining});
                if(shell.classList.contains('open'))addMessage(text); else pendingNotices.push(text);
                if ('Notification' in window && Notification.permission==='granted') {
                    try { new Notification('Chibi nhắc bạn 🐶',{body:text,icon:'/assistant/dog-sleep.png'}); } catch {}
                }
            });
        } catch {}
    }
    function reminderMessage({task,remaining}) {
        return remaining<0
            ? `Công việc “${task.title}” đã quá hạn. Đánh dấu hoàn thành để Chibi dừng nhắc nhé.`
            : `Công việc “${task.title}” sắp đến hạn trong vòng một giờ. Chibi sẽ nhắc lại sau 5 phút nếu việc chưa hoàn thành.`;
    }
    window.addEventListener('dayflow:task-created',checkUpcomingTasks);
    window.addEventListener('dayflow:task-updated',checkUpcomingTasks);
    document.addEventListener('visibilitychange',()=>{if(!document.hidden)checkUpcomingTasks();});
    checkUpcomingTasks();
    window.setInterval(checkUpcomingTasks,60*1000);
})();
