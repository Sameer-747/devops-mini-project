const API = '/api/tasks';

const list = document.getElementById('taskList');
const form = document.getElementById('taskForm');
const titleInput = document.getElementById('taskTitle');
const prioritySelect = document.getElementById('taskPriority');
const formError = document.getElementById('formError');

async function request(url, options = {}) {
    const res = await fetch(url, {
        headers: { 'Content-Type': 'application/json' },
        ...options,
    });
    if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || `Request failed (${res.status})`);
    }
    return res.status === 204 ? null : res.json();
}

function render(tasks) {
    list.innerHTML = '';
    if (tasks.length === 0) {
        list.innerHTML = '<li class="empty">No tasks yet. Add one above.</li>';
        return;
    }
    for (const task of tasks) {
        const li = document.createElement('li');
        li.className = 'task' + (task.completed ? ' done' : '');

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.checked = task.completed;
        checkbox.addEventListener('change', () => toggleTask(task.id));

        const title = document.createElement('span');
        title.className = 'title';
        title.textContent = task.title;

        const pill = document.createElement('span');
        pill.className = 'pill ' + task.priority;
        pill.textContent = task.priority;

        const del = document.createElement('button');
        del.className = 'delete';
        del.title = 'Delete task';
        del.textContent = '✕';
        del.addEventListener('click', () => deleteTask(task.id));

        li.append(checkbox, title, pill, del);
        list.appendChild(li);
    }
}

async function refresh() {
    const [tasks, stats] = await Promise.all([request(API), request(API + '/stats')]);
    render(tasks);
    document.getElementById('statTotal').textContent = stats.total;
    document.getElementById('statPending').textContent = stats.pending;
    document.getElementById('statDone').textContent = stats.completed;
}

async function toggleTask(id) {
    await request(`${API}/${id}/toggle`, { method: 'PATCH' });
    refresh();
}

async function deleteTask(id) {
    await request(`${API}/${id}`, { method: 'DELETE' });
    refresh();
}

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    formError.textContent = '';
    try {
        await request(API, {
            method: 'POST',
            body: JSON.stringify({ title: titleInput.value, priority: prioritySelect.value }),
        });
        titleInput.value = '';
        refresh();
    } catch (err) {
        formError.textContent = err.message;
    }
});

async function loadBuildInfo() {
    try {
        const info = await request('/api/info');
        document.getElementById('buildText').textContent =
            `v${info.version} · build #${info.build} · ${info.environment}`;
    } catch {
        document.getElementById('buildText').textContent = 'build info unavailable';
    }
}

loadBuildInfo();
refresh();
