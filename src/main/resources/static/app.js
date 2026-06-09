const API_BASE = '/api/v1/workflows';
const MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

let activeWorkflows = {}; // Map of id -> workflow object
let selectedWorkflowId = null;
let pollInterval = null;

// DOM Elements
const dropZone = document.getElementById('dropZone');
const fileInput = document.getElementById('fileInput');
const uploadStatus = document.getElementById('uploadStatus');
const uploadProgress = document.getElementById('uploadProgress');
const uploadStatusText = document.getElementById('uploadStatusText');
const workflowsList = document.getElementById('workflowsList');
const activeCount = document.getElementById('activeCount');
const timelineSection = document.getElementById('timelineSection');
const timelineTitle = document.getElementById('timelineTitle');
const timelineId = document.getElementById('timelineId');
const eventsList = document.getElementById('eventsList');

const dashboardView = document.getElementById('dashboardView');
const analyticsView = document.getElementById('analyticsView');

// Stages order for timeline logic
const STAGE_ORDER = ['VALIDATE', 'TRANSCRIBE', 'SUMMARIZE', 'EXTRACT_KEYWORDS', 'PUBLISH'];

// Navigation Logic
function switchView(viewName) {
    // Update nav links
    document.getElementById('navDashboard').classList.remove('active');
    document.getElementById('navAnalytics').classList.remove('active');
    
    // Hide all views
    dashboardView.classList.add('hidden');
    analyticsView.classList.add('hidden');
    
    if (viewName === 'dashboard') {
        document.getElementById('navDashboard').classList.add('active');
        dashboardView.classList.remove('hidden');
    } else if (viewName === 'analytics') {
        document.getElementById('navAnalytics').classList.add('active');
        analyticsView.classList.remove('hidden');
        closeTimeline();
        fetchAnalytics();
    }
}

async function fetchAnalytics() {
    try {
        const response = await fetch('/api/v1/analytics');
        if (!response.ok) return;
        const data = await response.json();
        
        document.getElementById('statCompleted').innerText = data.totalCompletedWorkflows;
        document.getElementById('statFailed').innerText = data.totalFailedWorkflows;
        document.getElementById('statTaskFailures').innerText = data.totalTaskFailures;
        document.getElementById('statRetries').innerText = data.totalRetries;
        document.getElementById('statDurationAvg').innerText = data.transcriptionAvgDurationSeconds.toFixed(2) + 's';
        document.getElementById('statDurationMax').innerText = data.transcriptionMaxDurationSeconds.toFixed(2) + 's';
        
    } catch (e) {
        console.error('Failed to fetch analytics', e);
    }
}

// Drag and Drop Events
dropZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropZone.classList.add('dragover');
});

dropZone.addEventListener('dragleave', () => {
    dropZone.classList.remove('dragover');
});

dropZone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropZone.classList.remove('dragover');
    if (e.dataTransfer.files.length) {
        handleFile(e.dataTransfer.files[0]);
    }
});

fileInput.addEventListener('change', (e) => {
    if (e.target.files.length) {
        handleFile(e.target.files[0]);
    }
});

function handleFile(file) {
    if (file.size > MAX_FILE_SIZE) {
        alert('File is too large! Maximum allowed size is 100MB.');
        return;
    }

    uploadFile(file);
}

async function uploadFile(file) {
    const formData = new FormData();
    formData.append('file', file);

    uploadStatus.classList.remove('hidden');
    uploadProgress.style.width = '30%';
    uploadStatusText.innerText = `Uploading ${file.name}...`;

    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) throw new Error('Upload failed');
        
        const data = await response.json();
        uploadProgress.style.width = '100%';
        uploadStatusText.innerText = 'Upload successful! Workflow starting...';
        
        setTimeout(() => {
            uploadStatus.classList.add('hidden');
            uploadProgress.style.width = '0%';
        }, 3000);

        // Add to tracking
        activeWorkflows[data.workflowId] = data;
        renderWorkflows();
        startPolling();

    } catch (error) {
        console.error(error);
        uploadStatusText.innerText = 'Error: ' + error.message;
        uploadStatusText.style.color = 'var(--danger)';
    }
}

// Render active workflows list
function renderWorkflows() {
    const ids = Object.keys(activeWorkflows);
    activeCount.innerText = ids.length;

    if (ids.length === 0) {
        workflowsList.innerHTML = '<div class="empty-state">No active workflows. Upload a file to start.</div>';
        return;
    }

    workflowsList.innerHTML = '';
    
    // Sort by newest first
    const sorted = Object.values(activeWorkflows).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    sorted.forEach(wf => {
        const item = document.createElement('div');
        item.className = 'workflow-item';
        item.onclick = () => openTimeline(wf.workflowId);
        
        const statusClass = `status-${wf.status}`;
        
        item.innerHTML = `
            <div class="workflow-info">
                <h4><i class="fa-solid fa-file-audio"></i> ${wf.audioFileKey.split('/').pop()}</h4>
                <p>Started: ${new Date(wf.createdAt).toLocaleTimeString()}</p>
            </div>
            <div class="status-tag ${statusClass}">${wf.status}</div>
        `;
        
        workflowsList.appendChild(item);
    });
}

// Fetch details and timeline
async function openTimeline(id) {
    selectedWorkflowId = id;
    timelineSection.classList.remove('hidden');
    
    const wf = activeWorkflows[id];
    if(wf) {
        timelineTitle.innerText = wf.audioFileKey.split('/').pop();
        timelineId.innerText = `ID: ${wf.workflowId}`;
    }

    await fetchTimeline(id);
}

function closeTimeline() {
    selectedWorkflowId = null;
    timelineSection.classList.add('hidden');
}

const resultsContainer = document.getElementById('resultsContainer');
const resultText = document.getElementById('resultText');

let currentResults = null;
let currentTab = 'transcript';

// Fetch single workflow timeline
async function fetchTimeline(id) {
    try {
        const response = await fetch(`${API_BASE}/${id}/timeline`);
        if (!response.ok) return;
        const events = await response.json();
        
        renderTimeline(events);
        
        // If workflow is COMPLETED, fetch results
        const wf = activeWorkflows[id];
        if (wf && wf.status === 'COMPLETED') {
            await fetchResults(id);
            resultsContainer.classList.remove('hidden');
        } else {
            resultsContainer.classList.add('hidden');
        }
    } catch (e) {
        console.error('Failed to fetch timeline', e);
    }
}

async function fetchResults(id) {
    try {
        const response = await fetch(`${API_BASE}/${id}/results`);
        if (!response.ok) return;
        currentResults = await response.json();
        renderResultTab(currentTab);
    } catch (e) {
        console.error('Failed to fetch results', e);
    }
}

window.switchTab = function(tabName) {
    currentTab = tabName;
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.innerText.toLowerCase() === tabName) btn.classList.add('active');
    });
    renderResultTab(tabName);
}

function renderResultTab(tabName) {
    if (!currentResults) return;
    let text = currentResults[tabName];
    if (!text) {
        text = "Not available yet or error reading result.";
    }
    resultText.innerText = text;
}

window.copyResult = function() {
    if (!currentResults || !currentResults[currentTab]) return;
    navigator.clipboard.writeText(currentResults[currentTab]).then(() => {
        const btn = document.querySelector('.copy-btn i');
        btn.className = 'fa-solid fa-check';
        setTimeout(() => {
            btn.className = 'fa-regular fa-copy';
        }, 2000);
    });
}

function renderTimeline(events) {
    eventsList.innerHTML = '';
    
    // Reset stages
    STAGE_ORDER.forEach(stage => {
        const el = document.getElementById(`stage-${stage}`);
        if(el) el.className = 'stage';
    });
    document.querySelectorAll('.stage-line').forEach(line => line.classList.remove('filled'));

    let currentHighestStageIndex = -1;

    events.forEach(ev => {
        // Build event log
        const li = document.createElement('li');
        const time = new Date(ev.occurredAt).toLocaleTimeString();
        li.innerHTML = `<span class="log-time">[${time}]</span> [${ev.taskType}] <span class="log-status ${ev.newStatus}">${ev.newStatus}</span> - ${ev.message}`;
        eventsList.appendChild(li);

        // Update visual stages
        const stageIndex = STAGE_ORDER.indexOf(ev.taskType);
        if (stageIndex >= 0) {
            const el = document.getElementById(`stage-${ev.taskType}`);
            if (el) {
                if (ev.newStatus === 'COMPLETED') {
                    el.className = 'stage completed';
                    currentHighestStageIndex = Math.max(currentHighestStageIndex, stageIndex);
                } else if (ev.newStatus === 'FAILED' || ev.newStatus === 'DEAD') {
                    el.className = 'stage failed';
                } else if (ev.newStatus === 'IN_PROGRESS') {
                    el.className = 'stage active';
                }
            }
        }
    });

    // Fill connecting lines up to highest completed stage
    const lines = document.querySelectorAll('.stage-line');
    for (let i = 0; i < currentHighestStageIndex && i < lines.length; i++) {
        lines[i].classList.add('filled');
    }
}

// Polling for updates
async function pollWorkflows() {
    const ids = Object.keys(activeWorkflows);
    if (ids.length === 0) return;

    for (const id of ids) {
        try {
            const response = await fetch(`${API_BASE}/${id}`);
            if (response.ok) {
                const wf = await response.json();
                activeWorkflows[id] = wf;
                
                // If it finished, we could optionally remove it from tracking or leave it to show history
                // We'll leave it in the list for now.
            }
        } catch (e) {
            console.error('Poll failed for ' + id);
        }
    }
    
    renderWorkflows();

    if (selectedWorkflowId) {
        await fetchTimeline(selectedWorkflowId);
    }
}

function startPolling() {
    if (!pollInterval) {
        pollInterval = setInterval(pollWorkflows, 3000);
    }
}

// Init
startPolling();
