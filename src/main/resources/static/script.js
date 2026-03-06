// DocuMind AI — Frontend JavaScript

let summaryText = '';
let isRecording = false;
let speechRecognition = null;

// On page load, check auth status
document.addEventListener('DOMContentLoaded', async () => {
    try {
        const response = await fetch('/api/auth/status');
        const data = await response.json();
        if (data.loggedIn === 'true') {
            document.getElementById('navUsername').textContent = data.username;
        } else {
            window.location.href = '/login.html';
        }
    } catch (err) {
        console.error('Auth check failed:', err);
    }
});

async function logout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
    } catch (err) {}
    window.location.href = '/login.html?logout=true';
}

async function uploadPdf(event) {
    const file = event.target.files[0];
    if (!file) return;
    await processUpload(file);
}

async function processUpload(file) {
    const uploadZone = document.getElementById('uploadZone');
    const docStatus = document.getElementById('docStatus');

    if (!file.name.toLowerCase().endsWith('.pdf')) {
        showUploadStatus('Please select a PDF file.', 'error'); return;
    }
    if (file.size > 10 * 1024 * 1024) {
        showUploadStatus('File too large. Maximum size is 10MB.', 'error'); return;
    }

    showUploadStatus('⏳ Processing your PDF...', '');
    uploadZone.style.opacity = '0.6';
    uploadZone.style.pointerEvents = 'none';

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch('/api/pdf/upload', { method: 'POST', body: formData });
        const data = await response.json();
        if (response.ok) {
            showUploadStatus(`✅ ${file.name} loaded successfully!`, 'success');
            docStatus.className = 'status-badge';
            docStatus.innerHTML = `<div class="status-dot"></div> ${file.name} ready`;
            addMessage('ai', `📄 I've processed ${file.name}. Ask me anything about it!`);
        } else {
            showUploadStatus(`❌ ${data.error}`, 'error');
        }
    } catch (err) {
        showUploadStatus('❌ Upload failed. Please check your connection.', 'error');
    } finally {
        uploadZone.style.opacity = '1';
        uploadZone.style.pointerEvents = 'auto';
    }
}

function showUploadStatus(message, type) {
    const status = document.getElementById('uploadStatus');
    status.textContent = message;
    status.className = `upload-status ${type}`;
    if (type) status.style.display = 'block';
}

function handleDragOver(event) {
    event.preventDefault();
    document.getElementById('uploadZone').classList.add('drag-over');
}
function handleDragLeave(event) {
    document.getElementById('uploadZone').classList.remove('drag-over');
}
function handleDrop(event) {
    event.preventDefault();
    document.getElementById('uploadZone').classList.remove('drag-over');
    const file = event.dataTransfer.files[0];
    if (file) processUpload(file);
}

async function sendQuestion() {
    const input = document.getElementById('questionInput');
    const question = input.value.trim();
    if (!question) return;

    addMessage('user', question);
    input.value = '';
    autoResize(input);

    const loadingId = addLoadingMessage();
    try {
        const response = await fetch('/api/chat/ask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ question })
        });
        const data = await response.json();
        removeLoadingMessage(loadingId);
        addMessage('ai', response.ok ? data.answer : `❌ Error: ${data.error}`);
    } catch (err) {
        removeLoadingMessage(loadingId);
        addMessage('ai', '❌ Network error. Please check your connection and try again.');
    }
}

function addMessage(type, text) {
    const container = document.getElementById('chatMessages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message message-${type}`;
    if (type === 'ai') {
        messageDiv.innerHTML = `<div class="message-bubble"><div class="ai-label">🧠 DocuMind AI</div>${escapeHtml(text)}</div>`;
    } else {
        messageDiv.innerHTML = `<div class="message-bubble">${escapeHtml(text)}</div>`;
    }
    container.appendChild(messageDiv);
    container.scrollTop = container.scrollHeight;
}

function addLoadingMessage() {
    const container = document.getElementById('chatMessages');
    const id = 'loading-' + Date.now();
    const div = document.createElement('div');
    div.id = id;
    div.className = 'message message-ai';
    div.innerHTML = `<div class="message-bubble"><div class="ai-label">🧠 DocuMind AI</div><div class="loading-dots"><span></span><span></span><span></span></div></div>`;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
    return id;
}

function removeLoadingMessage(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
}

function handleEnterKey(event) {
    if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); sendQuestion(); }
}

function autoResize(textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
}

async function summarize() {
    const btn = document.getElementById('summarizeBtn');
    const summaryBox = document.getElementById('summaryBox');
    const downloadBtn = document.getElementById('downloadBtn');

    btn.disabled = true;
    btn.textContent = '⏳ Summarizing...';
    summaryBox.innerHTML = `<div class="loading-dots" style="justify-content:center;padding:2rem"><span></span><span></span><span></span></div>`;
    downloadBtn.style.display = 'none';

    try {
        const response = await fetch('/api/chat/summarize', {
            method: 'POST', headers: { 'Content-Type': 'application/json' }
        });
        const data = await response.json();
        if (response.ok) {
            summaryText = data.summary;
            summaryBox.textContent = summaryText;
            downloadBtn.style.display = 'inline-flex';
        } else {
            summaryBox.innerHTML = `<span style="color:#fca5a5">❌ ${data.error}</span>`;
        }
    } catch (err) {
        summaryBox.innerHTML = `<span style="color:#fca5a5">❌ Network error. Please try again.</span>`;
    } finally {
        btn.disabled = false;
        btn.textContent = '✨ Summarize PDF';
    }
}

async function downloadSummary() {
    if (!summaryText) { alert('No summary yet. Click Summarize PDF first.'); return; }
    try {
        const response = await fetch('/api/chat/download-summary', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ summary: summaryText })
        });
        if (response.ok) {
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = 'documind-summary.pdf';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);
        }
    } catch (err) { console.error('Download failed:', err); }
}

function toggleSpeech() {
    if (!isRecording) startListening(); else stopListening();
}

function startListening() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        alert('Speech recognition is not supported. Please use Google Chrome.'); return;
    }
    speechRecognition = new SpeechRecognition();
    speechRecognition.continuous = false;
    speechRecognition.interimResults = true;
    speechRecognition.lang = 'en-US';

    const micBtn = document.getElementById('micBtn');
    const input = document.getElementById('questionInput');

    speechRecognition.onresult = (event) => {
        let transcript = '';
        for (let i = event.resultIndex; i < event.results.length; i++) {
            transcript += event.results[i][0].transcript;
        }
        input.value = transcript;
        autoResize(input);
    };

    speechRecognition.onend = () => {
        isRecording = false;
        micBtn.classList.remove('recording');
        micBtn.textContent = '🎤';
        if (input.value.trim()) setTimeout(sendQuestion, 500);
    };

    speechRecognition.onerror = (event) => {
        isRecording = false;
        micBtn.classList.remove('recording');
        micBtn.textContent = '🎤';
        if (event.error === 'not-allowed') alert('Microphone access denied. Please allow microphone in browser settings.');
    };

    speechRecognition.start();
    isRecording = true;
    micBtn.classList.add('recording');
    micBtn.textContent = '🔴';
}

function stopListening() {
    if (speechRecognition) speechRecognition.stop();
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
