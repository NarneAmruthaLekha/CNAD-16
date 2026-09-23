// ImpactFlow App Core Logic
const GATEWAY_URL = ''; // Relative to current host

let currentUser = null;
let currentToken = null;
let pollInterval = null;

// DOM Elements
const loginContainer = document.getElementById('login-container');
const appContainer = document.getElementById('app-container');
const loginForm = document.getElementById('login-form');
const loginError = document.getElementById('login-error');

const userDisplay = document.getElementById('user-display');
const logoutBtn = document.getElementById('logout-btn');

const submissionsTbody = document.getElementById('submissions-tbody');
const openSubmitModalBtn = document.getElementById('open-submit-modal');
const submitModal = document.getElementById('submit-modal');
const closeSubmitModalBtn = document.getElementById('close-submit-modal');
const cancelSubmitModalBtn = document.getElementById('cancel-submit-modal');
const submitChangeForm = document.getElementById('submit-change-form');

const reportModal = document.getElementById('report-modal');
const closeReportModalBtn = document.getElementById('close-report-modal');
const closeReportModalBtn2 = document.getElementById('close-report-modal-btn');

// Initialize App
document.addEventListener('DOMContentLoaded', () => {
    const savedToken = localStorage.getItem('if_token');
    const savedUser = localStorage.getItem('if_user');
    
    if (savedToken && savedUser) {
        currentToken = savedToken;
        currentUser = savedUser;
        showDashboard();
    } else {
        showLogin();
    }
});

// Auth Routing
function showLogin() {
    loginContainer.classList.remove('hidden');
    appContainer.classList.add('hidden');
    if (pollInterval) {
        clearInterval(pollInterval);
        pollInterval = null;
    }
}

function showDashboard() {
    loginContainer.classList.add('hidden');
    appContainer.classList.remove('hidden');
    userDisplay.textContent = currentUser;
    
    fetchReports();
    // Start polling report list
    pollInterval = setInterval(fetchReports, 3000);
}

// Login Handler
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    loginError.classList.add('hidden');
    
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();
    
    try {
        await attemptLogin(username, password);
    } catch (err) {
        // If login fails, auto-register and try login again (UX optimization for in-memory H2 DBs)
        console.warn("Login failed, attempting auto-registration...", err);
        try {
            const role = username.toLowerCase().includes('admin') ? 'ROLE_ADMIN' : 'ROLE_DEVELOPER';
            const registerRes = await fetch(`${GATEWAY_URL}/api/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password, roles: [role] })
            });
            
            if (registerRes.ok) {
                await attemptLogin(username, password);
            } else {
                throw new Error("Registration failed");
            }
        } catch (regErr) {
            loginError.textContent = "Authentication failed. Make sure auth-service is running.";
            loginError.classList.remove('hidden');
        }
    }
});

async function attemptLogin(username, password) {
    const res = await fetch(`${GATEWAY_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });
    
    if (!res.ok) {
        throw new Error("Unauthorized");
    }
    
    const data = await res.json();
    currentToken = data.token;
    currentUser = data.username;
    
    localStorage.setItem('if_token', currentToken);
    localStorage.setItem('if_user', currentUser);
    
    showDashboard();
}

// Logout
logoutBtn.addEventListener('click', () => {
    localStorage.removeItem('if_token');
    localStorage.removeItem('if_user');
    currentToken = null;
    currentUser = null;
    showLogin();
});

// Modals Interaction
openSubmitModalBtn.addEventListener('click', () => {
    // Inject mock commit hash and branch variations to make testing easy and fun
    document.getElementById('form-commit').value = Math.random().toString(16).substring(2, 10) + 'a9d04423405786ef';
    
    const branchPrefixes = ['feature/add-apple-pay', 'bugfix/auth-leak', 'refactor/core-db-pool', 'hotfix/gateway-timeout'];
    document.getElementById('form-branch').value = branchPrefixes[Math.floor(Math.random() * branchPrefixes.length)];
    
    submitModal.classList.remove('hidden');
});

function hideSubmitModal() {
    submitModal.classList.add('hidden');
}

closeSubmitModalBtn.addEventListener('click', hideSubmitModal);
cancelSubmitModalBtn.addEventListener('click', hideSubmitModal);

function hideReportModal() {
    reportModal.classList.add('hidden');
}
closeReportModalBtn.addEventListener('click', hideReportModal);
closeReportModalBtn2.addEventListener('click', hideReportModal);

// Submit Proposed Change
submitChangeForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const repoUrl = document.getElementById('form-repo').value.trim();
    const branch = document.getElementById('form-branch').value.trim();
    const commitId = document.getElementById('form-commit').value.trim();
    const filesInput = document.getElementById('form-files').value;
    
    const changedFiles = filesInput.split(',')
        .map(f => f.trim())
        .filter(f => f.length > 0);
        
    try {
        const res = await fetch(`${GATEWAY_URL}/api/changes/submit`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${currentToken}`
            },
            body: JSON.stringify({ repoUrl, branch, commitId, changedFiles })
        });
        
        if (res.status === 401) {
            logoutBtn.click();
            return;
        }
        
        if (res.ok) {
            hideSubmitModal();
            fetchReports();
        } else {
            alert("Error submitting change analysis request.");
        }
    } catch (err) {
        console.error("Error submitting change", err);
        alert("Failed to connect to API Gateway. Check if ingestion service is active.");
    }
});

// Fetch Reports & Render Dashboard
async function fetchReports() {
    if (!currentToken) return;
    
    try {
        const res = await fetch(`${GATEWAY_URL}/api/reports`, {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        
        if (res.status === 401) {
            logoutBtn.click();
            return;
        }
        
        if (res.ok) {
            const reports = await res.json();
            renderReportsTable(reports);
            calculateStats(reports);
        }
    } catch (err) {
        console.error("Error fetching reports", err);
    }
}

function renderReportsTable(reports) {
    if (reports.length === 0) {
        submissionsTbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center text-muted">No evaluations submitted yet. Create one above!</td>
            </tr>
        `;
        return;
    }
    
    submissionsTbody.innerHTML = reports.map(r => {
        const shortCommit = r.commitId ? r.commitId.substring(0, 7) : 'n/a';
        const fileCount = r.changedFiles ? r.changedFiles.length : 0;
        
        let riskBadge = '<span class="text-muted">-</span>';
        if (r.status === 'COMPLETED' && r.riskLevel) {
            const riskClass = r.riskLevel.toLowerCase();
            riskBadge = `<span class="risk-level-badge ${riskClass}">${r.riskLevel}</span>`;
        }
        
        let statusBadge = `<span class="status-badge pending">PENDING</span>`;
        if (r.status === 'COMPLETED') {
            statusBadge = `<span class="status-badge completed">COMPLETED</span>`;
        } else if (r.status === 'FAILED') {
            statusBadge = `<span class="status-badge failed">FAILED</span>`;
        }
        
        const sourceSvc = r.sourceService || 'unknown';
        const metricsStr = r.status === 'COMPLETED' 
            ? `LOC: ${r.totalLocDelta} | Comp: ${r.cyclomaticComplexity}` 
            : '<span class="text-muted">calculating...</span>';

        return `
            <tr>
                <td>
                    <strong>${extractRepoName(r.repoUrl)}</strong>
                    <span class="meta-sub-info">${r.branch}</span>
                </td>
                <td><span class="commit-sha" title="${r.commitId}">${shortCommit}</span></td>
                <td><span class="service-node source">${sourceSvc}</span></td>
                <td>${metricsStr}</td>
                <td>${riskBadge}</td>
                <td>${statusBadge}</td>
                <td>
                    <button class="btn secondary" onclick="viewReportDetails('${r.id}')">View Details</button>
                </td>
            </tr>
        `;
    }).join('');
}

function extractRepoName(url) {
    if (!url) return 'unknown';
    const parts = url.split('/');
    return parts[parts.length - 1] || url;
}

function calculateStats(reports) {
    const total = reports.length;
    document.getElementById('stat-total').textContent = total;
    
    const completed = reports.filter(r => r.status === 'COMPLETED');
    const highRisk = completed.filter(r => r.riskLevel === 'High').length;
    document.getElementById('stat-high-risk').textContent = highRisk;
    
    if (completed.length > 0) {
        const avgLoc = Math.round(completed.reduce((acc, r) => acc + (r.totalLocDelta || 0), 0) / completed.length);
        document.getElementById('stat-avg-loc').textContent = avgLoc;
        
        const avgConf = Math.round((completed.reduce((acc, r) => acc + (r.confidence || 0), 0) / completed.length) * 100);
        document.getElementById('stat-avg-confidence').textContent = `${avgConf}%`;
    } else {
        document.getElementById('stat-avg-loc').textContent = '-';
        document.getElementById('stat-avg-confidence').textContent = '-';
    }
}

// View Details Modal
async function viewReportDetails(reportId) {
    try {
        const res = await fetch(`${GATEWAY_URL}/api/reports/${reportId}`, {
            headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        
        if (res.ok) {
            const r = await res.json();
            
            // Populate Modal Content
            document.getElementById('report-title').textContent = `Change Risk Assessment: ${extractRepoName(r.repoUrl)}`;
            document.getElementById('report-meta-sub').innerHTML = `
                Branch: <strong>${r.branch}</strong> | 
                Commit: <span class="commit-sha">${r.commitId}</span> | 
                Status: <strong>${r.status}</strong>
            `;
            
            // Source service name
            document.getElementById('report-source-service').textContent = r.sourceService || 'unknown';
            
            // Impacted services list
            const impactedList = document.getElementById('report-impacted-services-list');
            if (r.impactedServices && r.impactedServices.length > 0) {
                impactedList.innerHTML = r.impactedServices.map(svc => `
                    <span class="service-node impacted">${svc}</span>
                `).join('');
            } else {
                impactedList.innerHTML = `<span class="no-impact text-muted">No downstream service dependencies affected</span>`;
            }
            
            // Metrics
            document.getElementById('report-loc').textContent = r.totalLocDelta !== undefined ? r.totalLocDelta : '-';
            document.getElementById('report-complexity').textContent = r.cyclomaticComplexity !== undefined ? r.cyclomaticComplexity : '-';
            document.getElementById('report-churn').textContent = r.fileChurnRate !== undefined ? `${(r.fileChurnRate * 100).toFixed(0)}%` : '-';
            
            // Risk Pred & Confidence
            const riskBadge = document.getElementById('report-risk-badge');
            riskBadge.className = 'risk-level-badge large'; // clear
            
            if (r.status === 'COMPLETED') {
                riskBadge.classList.add(r.riskLevel.toLowerCase());
                riskBadge.textContent = r.riskLevel;
                
                const confidencePct = Math.round((r.confidence || 0) * 100);
                document.getElementById('report-confidence-val').textContent = `${confidencePct}%`;
                document.getElementById('report-confidence-progress').style.width = `${confidencePct}%`;
                
                // Drivers / Contributors list
                const driversList = document.getElementById('report-drivers-list');
                if (r.contributors && r.contributors.length > 0) {
                    driversList.innerHTML = r.contributors.map(c => `<li>${c}</li>`).join('');
                } else {
                    driversList.innerHTML = `<li>Standard metrics</li>`;
                }
                
                // Recommendations
                const recsList = document.getElementById('report-recs-list');
                if (r.recommendations && r.recommendations.length > 0) {
                    recsList.innerHTML = r.recommendations.map(rec => `<li>${rec}</li>`).join('');
                } else {
                    recsList.innerHTML = `<li>No recommendations generated.</li>`;
                }
            } else {
                riskBadge.textContent = 'PENDING';
                document.getElementById('report-confidence-val').textContent = '0%';
                document.getElementById('report-confidence-progress').style.width = '0%';
                document.getElementById('report-drivers-list').innerHTML = `<li><span class="text-muted">Awaiting ML scoring pipeline completion...</span></li>`;
                document.getElementById('report-recs-list').innerHTML = `<li><span class="text-muted">Analyzing metrics and generating pipeline feedback...</span></li>`;
            }
            
            // Changed Files List
            const filesList = document.getElementById('report-files-list');
            if (r.changedFiles && r.changedFiles.length > 0) {
                filesList.innerHTML = r.changedFiles.map(f => `<li>${f}</li>`).join('');
            } else {
                filesList.innerHTML = `<li>No modified files found.</li>`;
            }
            
            // Display Modal
            reportModal.classList.remove('hidden');
        }
    } catch (err) {
        console.error("Error viewing report details", err);
    }
}
window.viewReportDetails = viewReportDetails; // Expose to global scope for HTML onclick
