const API = { changes: "http://localhost:8082/api/changes/submit", reports: "http://localhost:8083/api/reports", summary: "http://localhost:8083/api/reports/summary", health: "http://localhost:8083/api/reports/health" };
const $ = (s) => document.querySelector(s);
const $$ = (s) => [...document.querySelectorAll(s)];
let cachedReports = [];
function showView(name) {
  $$(".view").forEach(v => v.classList.toggle("active", v.id === `view-${name}`));
  $$(".nav-item").forEach(b => b.classList.toggle("active", b.dataset.view === name));
  $("#page-title").textContent = ({overview:"Overview",submit:"Analyze a change",reports:"Reports history"})[name] || "Overview";
  if (name === "reports") loadReports();
  if (name === "overview") loadSummary();
}
$$("[data-view]").forEach(b => b.addEventListener("click", () => showView(b.dataset.view)));
$$("[data-go]").forEach(b => b.addEventListener("click", () => showView(b.dataset.go)));
function toast(message) { const t=$("#toast"); t.textContent=message; t.classList.remove("hidden"); setTimeout(()=>t.classList.add("hidden"),3200); }
async function request(url, options={}) {
  const r = await fetch(url, options);
  const text = await r.text();
  let data; try { data = text ? JSON.parse(text) : {}; } catch { data = text; }
  if (!r.ok) throw new Error((data && (data.message || data.error || data.detail)) || `${r.status} ${r.statusText}`);
  return data;
}
async function checkConnection() {
  try { await request(API.health); $("#connection-label").textContent = "Report service connected"; }
  catch { $("#connection-label").textContent = "Backend not reachable"; }
}
function reportObject(item) { return item?.report || item?.data || item || {}; }
function riskValue(r) { return String(r.risk || r.riskLevel || r.predictedRisk || "UNKNOWN").toUpperCase(); }
function renderMetrics(reports) {
  const risks=reports.map(x=>riskValue(reportObject(x)));
  $("#metric-total").textContent=reports.length;
  $("#metric-low").textContent=risks.filter(x=>x==="LOW").length;
  $("#metric-medium").textContent=risks.filter(x=>x==="MEDIUM").length;
  $("#metric-high").textContent=risks.filter(x=>x==="HIGH").length;
}
async function loadSummary() {
  try {
    const summary=await request(API.summary);
    $("#metric-total").textContent=summary.totalReports ?? summary.total ?? "—";
    $("#metric-low").textContent=summary.lowRisk ?? "—";
    $("#metric-medium").textContent=summary.mediumRisk ?? "—";
    $("#metric-high").textContent=summary.highRisk ?? "—";
  } catch {
    try { const data=await request(API.reports); cachedReports=Array.isArray(data)?data:(data.reports||[]); renderMetrics(cachedReports); }
    catch { ["#metric-total","#metric-low","#metric-medium","#metric-high"].forEach(s=>$(s).textContent="—"); }
  }
}
function esc(value) { return String(value ?? "").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c])); }
function renderReports() {
  const risk=riskValue(r);
  const riskGuidance={
    LOW:{
        title:"Lower predicted risk",
        explanation:"The model classified this change as low risk based on the information provided. This does not guarantee the change is safe.",
        action:"Run the relevant automated tests and complete your normal code review."
    },
    MEDIUM:{
        title:"Moderate predicted risk",
        explanation:"The model identified a moderate level of risk. Review the change carefully before merging.",
        action:"Review affected components, test edge cases, and check the impact on dependent services."
    },
    HIGH:{
        title:"Higher predicted risk",
        explanation:"The model classified this change as high risk. Additional review is recommended before release.",
        action:"Request a thorough code review, expand test coverage, and consider a staged rollout."
    }
};
const guidance=riskGuidance[risk]||{
    title:"Risk not available",
    explanation:"A risk category could not be identified for this report.",
    action:"Check the analysis result and service logs before relying on this report."
};
  const q=$("#report-search").value.trim().toLowerCase();
  const filtered=cachedReports.filter(item=>JSON.stringify(reportObject(item)).toLowerCase().includes(q));
  const body=$("#reports-body");
  if (!filtered.length) { body.innerHTML=`<tr><td colspan="5" class="empty-cell">${cachedReports.length?"No reports match your search.":"No reports found yet. Submit your first change to create a report."}</td></tr>`; }
  else body.innerHTML=filtered.map((item,i)=>{
    const r=reportObject(item), id=item.id||r.id||`report-${i}`, risk=riskValue(r), repo=r.repoUrl||r.repositoryUrl||"Repository not provided", branch=r.branch||"—", status=r.status||"Saved";
    const riskClass=["LOW","MEDIUM","HIGH"].includes(risk)?risk.toLowerCase():"unknown";
    return `<tr><td><strong>${esc(repo)}</strong><small>${esc(r.proposedChange||r.commitId||id)}</small></td><td>${esc(branch)}</td><td><span class="risk-pill ${riskClass}">${esc(risk)}</span></td><td><span class="status-pill">${esc(status)}</span></td><td><button class="text-btn detail-btn" data-index="${i}">Details →</button></td></tr>`;
  }).join("");
  $("#reports-footer").textContent=`${filtered.length} report${filtered.length===1?"":"s"} shown · loaded from Report Service`;
  
$$(".detail-btn").forEach(btn=>btn.addEventListener("click",()=>{
    const item=filtered[Number(btn.dataset.index)];
    const r=reportObject(item);
    const detail=$("#report-detail");
    const risk=riskValue(r);
    const riskClass=["LOW","MEDIUM","HIGH"].includes(risk)?risk.toLowerCase():"unknown";
    const score=r.riskScore??r.risk_score??r.riskPrediction?.risk_score;
    const files=Array.isArray(r.changedFiles)?r.changedFiles:[];
    const date=Array.isArray(r.submittedAt)?r.submittedAt.join("-"):(r.submittedAt||r.submitted_at||"Not available");
    detail.innerHTML=`
      <div class="detail-header">
        <div><div class="eyebrow">ANALYSIS REPORT</div><h3>${esc(r.proposedChange||"Software change analysis")}</h3></div>
        <span class="risk-pill ${riskClass}">${esc(risk)} RISK</span>
      </div>
      <p class="detail-description">${esc(guidance.title)}. ${esc(guidance.explanation)}</p>
<div class="detail-section"><h4>Recommended next step</h4><p>${esc(guidance.action)}</p></div>
      <div class="detail-metrics">
        <div><small>Risk score</small><strong>${score===undefined?"Not available":esc(score)}</strong></div>
        <div><small>Branch</small><strong>${esc(r.branch||"—")}</strong></div>
        <div><small>Status</small><strong>${esc(r.status||"Saved")}</strong></div>
      </div>
      <div class="detail-section"><h4>Repository</h4><p>${esc(r.repoUrl||r.repositoryUrl||"Not provided")}</p><p>Commit: ${esc(r.commitId||"Not provided")}</p></div>
      <div class="detail-section"><h4>Changed files (${files.length})</h4>
        ${files.length?`<ul>${files.map(f=>`<li>${esc(f)}</li>`).join("")}</ul>`:"<p>No changed-file details available.</p>"}
      </div>
      <div class="detail-section"><h4>Submitted</h4><p>${esc(date)}</p></div>
      <p class="detail-note">Risk predictions are estimates. Review the code and tests before making release decisions.</p>`;
    detail.classList.remove("hidden");
    detail.scrollIntoView({behavior:"smooth",block:"nearest"});
}));

}
async function loadReports() {
  $("#reports-body").innerHTML='<tr><td colspan="5" class="empty-cell">Loading reports…</td></tr>';
  try {
    const data=await request(API.reports);
    cachedReports=Array.isArray(data)?data:(data.reports||[]);
    renderReports(); renderMetrics(cachedReports);
  } catch(e) {
    $("#reports-body").innerHTML=`<tr><td colspan="5" class="empty-cell">Couldn't load reports. ${esc(e.message)}<br>Make sure Report Service is running on port 8083.</td></tr>`;
    $("#reports-footer").textContent="Connection failed";
  }
}
$("#report-search").addEventListener("input",renderReports);
$("#refresh-reports").addEventListener("click",loadReports);
$("#change-form").addEventListener("submit",async e=>{
  e.preventDefault();
  const form=e.currentTarget, fd=new FormData(form), msg=$("#submit-message"), btn=$("#submit-btn");
  const files=String(fd.get("changedFiles")||"").split(/\r?\n/).map(s=>s.trim()).filter(Boolean);
  const payload={repoUrl:String(fd.get("repoUrl")).trim(),commitId:String(fd.get("commitId")).trim(),branch:String(fd.get("branch")).trim(),proposedChange:String(fd.get("proposedChange")).trim(),changedFiles:files,linesAdded:Number(fd.get("linesAdded")),linesDeleted:Number(fd.get("linesDeleted")),complexity:Number(fd.get("complexity"))};
  if(!files.length){msg.textContent="Please enter at least one changed file path.";msg.className="message error";return;}
  btn.disabled=true;btn.textContent="Analyzing…";msg.className="message hidden";
  try {
    const result=await request(API.changes,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)});
    msg.textContent="Change submitted successfully. Response:\n"+JSON.stringify(result,null,2)+"\n\nOpen Reports history to check whether the report has been saved.";
    msg.className="message success"; toast("Change submitted successfully");
    await loadSummary();
  } catch(err) {
    msg.textContent=`We couldn't submit this change: ${err.message}\n\nCheck that Change Analysis Service is running on port 8082. If the browser reports a CORS error, the backend needs to allow this frontend origin.`;
    msg.className="message error";
  } finally { btn.disabled=false;btn.innerHTML='Analyze change <span>→</span>'; }
});
checkConnection(); loadSummary();
