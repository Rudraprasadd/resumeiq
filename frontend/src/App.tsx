import {
  Activity,
  ArrowRight,
  BarChart3,
  CheckCircle2,
  FileText,
  Gauge,
  History,
  Lock,
  LogOut,
  Sparkles,
  Upload,
  UserRound,
  Wallet
} from "lucide-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { api } from "./api";
import type { LoginPayload, RegisterPayload } from "./api";
import { clearAuth, loadAuth, saveAuth, type AuthState } from "./auth";

type AuthMode = "login" | "register";

type DraftAnalysis = {
  resumeName: string;
  jobTitle: string;
  companyName: string;
  jobDescription: string;
};

const initialDraft: DraftAnalysis = {
  resumeName: "",
  jobTitle: "",
  companyName: "",
  jobDescription: ""
};

const metrics = [
  { label: "Match score", value: "82", suffix: "%", icon: Gauge },
  { label: "ATS readiness", value: "91", suffix: "%", icon: BarChart3 },
  { label: "Missing keywords", value: "7", suffix: "", icon: Activity }
];

const history = [
  { role: "Senior Backend Engineer", company: "FinEdge", score: 88, date: "Today" },
  { role: "Full Stack Developer", company: "CloudWorks", score: 76, date: "Yesterday" },
  { role: "Java Spring Engineer", company: "Nexora", score: 83, date: "May 20" }
];

export function App() {
  const [auth, setAuth] = useState<AuthState | null>(() => loadAuth());
  const [mode, setMode] = useState<AuthMode>("login");
  const [authError, setAuthError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [health, setHealth] = useState<"checking" | "up" | "down">("checking");
  const [draft, setDraft] = useState<DraftAnalysis>(initialDraft);

  useEffect(() => {
    api
      .health()
      .then((result) => setHealth(result.status === "UP" ? "up" : "down"))
      .catch(() => setHealth("down"));
  }, []);

  const completion = useMemo(() => {
    const fields = [
      draft.resumeName,
      draft.jobTitle,
      draft.companyName,
      draft.jobDescription
    ];
    return Math.round((fields.filter(Boolean).length / fields.length) * 100);
  }, [draft]);

  async function handleAuthSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAuthError("");
    setIsSubmitting(true);

    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "");
    const password = String(form.get("password") ?? "");

    try {
      const response =
        mode === "login"
          ? await api.login({ email, password } satisfies LoginPayload)
          : await api.register({
              fullName: String(form.get("fullName") ?? ""),
              email,
              password
            } satisfies RegisterPayload);
      setAuth(saveAuth(response));
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : "Authentication failed");
    } finally {
      setIsSubmitting(false);
    }
  }

  function signOut() {
    clearAuth();
    setAuth(null);
  }

  if (!auth) {
    return (
      <main className="auth-page">
        <section className="auth-panel">
          <div className="brand-row">
            <div className="brand-mark">
              <Sparkles size={22} />
            </div>
            <div>
              <strong>ResumeIQ</strong>
              <span>AI resume analysis</span>
            </div>
          </div>

          <div className="auth-copy">
            <h1>Turn one resume into the right resume.</h1>
            <p>
              Compare your resume with a job description, surface ATS gaps, and prepare
              sharper answers before applying.
            </p>
          </div>

          <div className="status-strip">
            <span className={`status-dot ${health}`} />
            Backend {health === "checking" ? "checking" : health === "up" ? "online" : "offline"}
          </div>
        </section>

        <section className="auth-card">
          <div className="segmented" aria-label="Authentication mode">
            <button
              className={mode === "login" ? "active" : ""}
              type="button"
              onClick={() => setMode("login")}
            >
              Login
            </button>
            <button
              className={mode === "register" ? "active" : ""}
              type="button"
              onClick={() => setMode("register")}
            >
              Register
            </button>
          </div>

          <form className="auth-form" onSubmit={handleAuthSubmit}>
            {mode === "register" && (
              <label>
                Full name
                <input name="fullName" autoComplete="name" minLength={2} required />
              </label>
            )}
            <label>
              Email
              <input name="email" type="email" autoComplete="email" required />
            </label>
            <label>
              Password
              <input
                name="password"
                type="password"
                autoComplete={mode === "login" ? "current-password" : "new-password"}
                minLength={mode === "register" ? 8 : undefined}
                required
              />
            </label>

            {authError && <p className="form-error">{authError}</p>}

            <button className="primary-button" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Please wait" : mode === "login" ? "Login" : "Create account"}
              <ArrowRight size={18} />
            </button>
          </form>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand-row">
          <div className="brand-mark">
            <Sparkles size={20} />
          </div>
          <div>
            <strong>ResumeIQ</strong>
            <span>{auth.user.plan} plan</span>
          </div>
        </div>

        <nav className="nav-list">
          <a className="active" href="#analysis">
            <FileText size={18} /> Analysis
          </a>
          <a href="#history">
            <History size={18} /> History
          </a>
          <a href="#billing">
            <Wallet size={18} /> Billing
          </a>
          <a href="#security">
            <Lock size={18} /> Security
          </a>
        </nav>

        <button className="ghost-button" type="button" onClick={signOut}>
          <LogOut size={18} /> Sign out
        </button>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">Dashboard</p>
            <h1>Resume analysis workspace</h1>
          </div>
          <div className="profile-pill">
            <UserRound size={18} />
            <span>{auth.user.fullName}</span>
          </div>
        </header>

        <section className="metric-grid" aria-label="Analysis metrics">
          {metrics.map((metric) => {
            const Icon = metric.icon;
            return (
              <article className="metric-card" key={metric.label}>
                <Icon size={18} />
                <span>{metric.label}</span>
                <strong>
                  {metric.value}
                  {metric.suffix}
                </strong>
              </article>
            );
          })}
        </section>

        <section className="content-grid" id="analysis">
          <article className="tool-panel">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Analyze</p>
                <h2>Resume and job description</h2>
              </div>
              <span className="progress-badge">{completion}% ready</span>
            </div>

            <div className="upload-box">
              <Upload size={22} />
              <div>
                <strong>{draft.resumeName || "Upload resume PDF"}</strong>
                <span>PDF up to 5 MB</span>
              </div>
              <input
                type="file"
                accept="application/pdf"
                aria-label="Upload resume PDF"
                onChange={(event) =>
                  setDraft((current) => ({
                    ...current,
                    resumeName: event.target.files?.[0]?.name ?? ""
                  }))
                }
              />
            </div>

            <div className="field-row">
              <label>
                Job title
                <input
                  value={draft.jobTitle}
                  onChange={(event) =>
                    setDraft((current) => ({ ...current, jobTitle: event.target.value }))
                  }
                />
              </label>
              <label>
                Company
                <input
                  value={draft.companyName}
                  onChange={(event) =>
                    setDraft((current) => ({ ...current, companyName: event.target.value }))
                  }
                />
              </label>
            </div>

            <label>
              Job description
              <textarea
                value={draft.jobDescription}
                onChange={(event) =>
                  setDraft((current) => ({ ...current, jobDescription: event.target.value }))
                }
                rows={10}
              />
            </label>

            <button className="primary-button" type="button" disabled>
              Run analysis
              <Sparkles size={18} />
            </button>
          </article>

          <article className="insight-panel">
            <div className="section-heading">
              <div>
                <p className="eyebrow">Preview</p>
                <h2>Expected output</h2>
              </div>
            </div>
            <ul className="check-list">
              <li>
                <CheckCircle2 size={18} />
                Match score and ATS readiness
              </li>
              <li>
                <CheckCircle2 size={18} />
                Missing keywords and role fit
              </li>
              <li>
                <CheckCircle2 size={18} />
                Resume rewrite suggestions
              </li>
              <li>
                <CheckCircle2 size={18} />
                Cover letter and interview questions
              </li>
            </ul>
            <div className="note-box">
              Backend resume and analysis endpoints are ready to connect once implemented.
            </div>
          </article>
        </section>

        <section className="history-panel" id="history">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Recent</p>
              <h2>Analysis history</h2>
            </div>
          </div>
          <div className="history-list">
            {history.map((item) => (
              <div className="history-row" key={`${item.role}-${item.company}`}>
                <div>
                  <strong>{item.role}</strong>
                  <span>{item.company}</span>
                </div>
                <span>{item.score}%</span>
                <time>{item.date}</time>
              </div>
            ))}
          </div>
        </section>
      </section>
    </main>
  );
}
