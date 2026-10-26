
const API = {
  changes: "http://localhost:8082/api/changes/submit",
  reports: "http://localhost:8083/api/reports",
  summary: "http://localhost:8083/api/reports/summary",
  health: "http://localhost:8083/api/reports/health"
};

const $ = s => document.querySelector(s);
const $$ = s => [...document.querySelectorAll(s)];

let cachedReports = [];

function showView(name) {
  $$(".view").forEach(v => v.classList.toggle("active", v.id === `view-${name}`));
  $$(".nav-item").forEach(b => b.classList.toggle("active", b.dataset.view === name));
  const title = $("#page-title");
  if (title) title.textContent = ({overview:"Overview",submit:"Analyze a change",reports:"Reports history"})[name] || "Overview";
  if (name === "reports") loadReports();
  if (name === "overview") loadSummary();
}

$$("[data-view]").forEach(b => b.addEventListener("click", () => showView(b.dataset.view)));
$$("[data-go]").forEach(b => b.addEventListener("click", () => showView(b.dataset.go)));

function toast(message) {
  const t = $("#toast");
  if (!t) return;
  t.textContent = message;
  t.classList.remove("hidden");
  setTimeout(() => t.classList.add("hidden"), 3200);
}

async function request(url, options = {}) {
  const response = await fetch(url, options);
  const text = await response.text();
  let data;

  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = text;
  }

  if (!response.ok) {
    const message = data && typeof data === "object"
      ? data.message || data.error || data.detail
      : "";
    throw new Error(message || `${response.status} ${response.statusText}`);
  }

  return data;
}

async function checkConnection() {
  const label = $("#connection-label");
  if (!label) return;

  try {
    await request(API.health);
    label.textContent = "Report service connected";
  } catch {
    label.textContent = "Backend not reachable";
  }
}

function reportObject(item) {
  return item?.report || item?.data || item || {};
}

function riskValue(r) {
  const prediction = r.riskPrediction || r.risk_prediction || {};
  return String(
    r.risk ||
    r.riskLevel ||
    r.predictedRisk ||
    prediction.risk ||
    prediction.riskLevel ||
    "UNKNOWN"
  ).toUpperCase();
}

function renderMetrics(reports) {
  const risks = reports.map(item => riskValue(reportObject(item)));

  $("#metric-total").textContent = reports.length;
  $("#metric-low").textContent = risks.filter(r => r === "LOW").length;
  $("#metric-medium").textContent = risks.filter(r => r === "MEDIUM").length;
  $("#metric-high").textContent = risks.filter(r => r === "HIGH").length;
}

async function loadSummary() {
  try {
    const summary = await request(API.summary);

    $("#metric-total").textContent = summary.totalReports ?? summary.total ?? "—";
    $("#metric-low").textContent = summary.lowRisk ?? "—";
    $("#metric-medium").textContent = summary.mediumRisk ?? "—";
    $("#metric-high").textContent = summary.highRisk ?? "—";
  } catch {
    try {
      const data = await request(API.reports);
      cachedReports = Array.isArray(data) ? data : (data.reports || []);
      renderMetrics(cachedReports);
    } catch {
      ["#metric-total","#metric-low","#metric-medium","#metric-high"].forEach(s => {
        const el = $(s);
        if (el) el.textContent = "—";
      });
    }
  }
}

function esc(value) {
  return String(value ?? "").replace(/[&<>"']/g, c => ({
    "&":"&amp;",
    "<":"&lt;",
    ">":"&gt;",
    '"':"&quot;",
    "'":"&#39;"
  })[c]);
}

function getRiskGuidance(risk) {
  const guidance = {
    LOW: {
      title: "Lower predicted risk",
      explanation: "The model classified this change as low risk based on the information provided. This does not guarantee the change is safe.",
      action: "Run the relevant automated tests and complete your normal code review."
    },
    MEDIUM: {
      title: "Moderate predicted risk",
      explanation: "The model identified a moderate level of risk. Review the change carefully before merging.",
      action: "Review affected components, test edge cases, and check the impact on dependent services."
    },
    HIGH: {
      title: "Higher predicted risk",
      explanation: "The model classified this change as high risk. Additional review is recommended before release.",
      action: "Request a thorough code review, expand test coverage, and consider a staged rollout."
    }
  };

  return guidance[risk] || {
    title: "Risk not available",
    explanation: "A risk category could not be identified for this report.",
    action: "Check the analysis result and service logs before relying on this report."
  };
}

function formatDate(value) {
  if (!value) return "Not available";

  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    const date = new Date(year, month - 1, day, hour, minute, second);
    return Number.isNaN(date.getTime()) ? value.join("-") : date.toLocaleString();
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
}

function renderReports() {
  const search = $("#report-search");
  const query = search ? search.value.trim().toLowerCase() : "";

  const filtered = cachedReports.filter(item =>
    JSON.stringify(reportObject(item)).toLowerCase().includes(query)
  );

  const body = $("#reports-body");
  if (!body) return;

  if (!filtered.length) {
    body.innerHTML = `<tr><td colspan="5" class="empty-cell">${
      cachedReports.length
        ? "No reports match your search."
        : "No reports found yet. Submit your first change to create a report."
    }</td></tr>`;
  } else {
    body.innerHTML = filtered.map((item, i) => {
      const r = reportObject(item);
      const id = item.id || r.id || `report-${i}`;
      const risk = riskValue(r);
      const repo = r.repoUrl || r.repositoryUrl || "Repository not provided";
      const branch = r.branch || "—";
      const status = r.status || "Saved";
      const riskClass = ["LOW","MEDIUM","HIGH"].includes(risk) ? risk.toLowerCase() : "unknown";

      return `<tr>
        <td><strong>${esc(repo)}</strong><small>${esc(r.proposedChange || r.commitId || id)}</small></td>
        <td>${esc(branch)}</td>
        <td><span class="risk-pill ${riskClass}">${esc(risk)}</span></td>
        <td><span class="status-pill">${esc(status)}</span></td>
        <td><button type="button" class="text-btn detail-btn" data-index="${i}">Details →</button></td>
      </tr>`;
    }).join("");
  }

  const footer = $("#reports-footer");
  if (footer) {
    footer.textContent = `${filtered.length} report${filtered.length === 1 ? "" : "s"} shown · loaded from Report Service`;
  }

  $$(".detail-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const item = filtered[Number(btn.dataset.index)];
      if (!item) return;

      const r = reportObject(item);
      const detail = $("#report-detail");
      if (!detail) return;

      const risk = riskValue(r);
      const riskClass = ["LOW","MEDIUM","HIGH"].includes(risk) ? risk.toLowerCase() : "unknown";
      const guidance = getRiskGuidance(risk);
      const prediction = r.riskPrediction || r.risk_prediction || {};
      const score = r.riskScore ?? r.risk_score ?? prediction.risk_score ?? prediction.riskScore;
      const files = Array.isArray(r.changedFiles)
        ? r.changedFiles
        : Array.isArray(r.changed_files) ? r.changed_files : [];
      const submitted = formatDate(r.submittedAt || r.submitted_at);
      
      const submission = r.submission && typeof r.submission === "object" ? r.submission : {};
      const added = r.linesAdded ?? r.lines_added ?? submission.linesAdded ?? submission.lines_added;
      const deleted = r.linesDeleted ?? r.lines_deleted ?? submission.linesDeleted ?? submission.lines_deleted;
      const complexity = r.complexity ?? submission.complexity;


      detail.innerHTML = `
        <div class="detail-header">
          <div>
            <div class="eyebrow">ANALYSIS REPORT</div>
            <h3>${esc(r.proposedChange || "Software change analysis")}</h3>
          </div>
          <span class="risk-pill ${riskClass}">${esc(risk)} RISK</span>
        </div>

        <p class="detail-description">${esc(guidance.title)}. ${esc(guidance.explanation)}</p>

        <div class="detail-metrics">
          <div><small>Risk score</small><strong>${score === undefined ? "Not available" : esc(score)}</strong></div>
          <div><small>Branch</small><strong>${esc(r.branch || "—")}</strong></div>
          <div><small>Status</small><strong>${esc(r.status || "Saved")}</strong></div>
        </div>

        <div class="detail-section">
          <h4>Recommended next step</h4>
          <p>${esc(guidance.action)}</p>
        </div>

        <div class="detail-section">
          <h4>Repository</h4>
          <p>${esc(r.repoUrl || r.repositoryUrl || "Not provided")}</p>
          <p>Commit: ${esc(r.commitId || "Not provided")}</p>
        </div>

        <div class="detail-section">
          <h4>Change size and complexity</h4>
          <p>Lines added: ${added === undefined ? "Not available" : esc(added)}</p>
          <p>Lines deleted: ${deleted === undefined ? "Not available" : esc(deleted)}</p>
          <p>Complexity: ${complexity === undefined ? "Not available" : `${esc(complexity)} / 5`}</p>
        </div>

        <div class="detail-section">
          <h4>Changed files (${files.length})</h4>
          ${files.length ? `<ul>${files.map(f => `<li>${esc(f)}</li>`).join("")}</ul>` : "<p>No changed-file details available.</p>"}
        </div>

        <div class="detail-section">
          <h4>Submitted</h4>
          <p>${esc(submitted)}</p>
        </div>

        <p class="detail-note">Risk predictions are estimates. Review the code and tests before making release decisions.</p>
      `;

      detail.classList.remove("hidden");
      detail.scrollIntoView({behavior:"smooth",block:"nearest"});
    });
  });
}

async function loadReports() {
  const body = $("#reports-body");
  const footer = $("#reports-footer");

  if (body) body.innerHTML = '<tr><td colspan="5" class="empty-cell">Loading reports…</td></tr>';

  try {
    const data = await request(API.reports);
    cachedReports = Array.isArray(data) ? data : (data.reports || []);
    renderReports();
    renderMetrics(cachedReports);
  } catch (e) {
    if (body) {
      body.innerHTML = `<tr><td colspan="5" class="empty-cell">Couldn't load reports. ${esc(e.message)}<br>Make sure Report Service is running on port 8083.</td></tr>`;
    }
    if (footer) footer.textContent = "Connection failed";
  }
}

const reportSearch = $("#report-search");
if (reportSearch) reportSearch.addEventListener("input", renderReports);

const refreshButton = $("#refresh-reports");
if (refreshButton) refreshButton.addEventListener("click", loadReports);

const changeForm = $("#change-form");

if (changeForm) {
  changeForm.addEventListener("submit", async e => {
    e.preventDefault();

    const form = e.currentTarget;
    const fd = new FormData(form);
    const msg = $("#submit-message");
    const btn = $("#submit-btn");

    const files = String(fd.get("changedFiles") || "")
      .split(/\r?\n/)
      .map(s => s.trim())
      .filter(Boolean);

    const payload = {
      repoUrl: String(fd.get("repoUrl") || "").trim(),
      commitId: String(fd.get("commitId") || "").trim(),
      branch: String(fd.get("branch") || "").trim(),
      proposedChange: String(fd.get("proposedChange") || "").trim(),
      changedFiles: files,
      linesAdded: Number(fd.get("linesAdded")),
      linesDeleted: Number(fd.get("linesDeleted")),
      complexity: Number(fd.get("complexity"))
    };

    if (!files.length) {
      if (msg) {
        msg.textContent = "Please enter at least one changed file path.";
        msg.className = "message error";
      }
      return;
    }

    if (btn) {
      btn.disabled = true;
      btn.textContent = "Analyzing…";
    }
    if (msg) msg.className = "message hidden";

    try {
      const result = await request(API.changes, {
        method: "POST",
        headers: {"Content-Type":"application/json"},
        body: JSON.stringify(payload)
      });

      if (msg) {
        msg.textContent = "Change submitted successfully. Response:\n" +
          JSON.stringify(result, null, 2) +
          "\n\nOpen Reports history to check whether the report has been saved.";
        msg.className = "message success";
      }

      toast("Change submitted successfully");
      await loadSummary();
    } catch (err) {
      if (msg) {
        msg.textContent = `We couldn't submit this change: ${err.message}\n\nCheck that Change Analysis Service is running on port 8082. If the browser reports a CORS error, the backend needs to allow this frontend origin.`;
        msg.className = "message error";
      }
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = 'Analyze change <span>→</span>';
      }
    }
  });
}

checkConnection();
loadSummary();
