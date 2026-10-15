import { useEffect, useState, type FormEvent } from 'react'

type ApiStatus = 'checking' | 'available' | 'unavailable'
type Action = 'idle' | 'submitting'
interface CanonicalResume {
  name: string
  contact: { email: string; phone: string; location: string }
  summary: string
  skills: string[]
  experience: { company: string; title: string; start_date: string; end_date: string; bullets: string[] }[]
  projects: { name: string; description: string; technologies: string[]; start_date: string; end_date: string; bullets: string[] }[]
  education: { institution: string; location: string; degree: string; start_date: string; end_date: string; details: string[] }[]
  certifications?: { name: string; date: string }[]
}
interface ResumeResponse { resume_id: string; raw_text: string; parsed_resume: CanonicalResume }
interface JobResponse { job_id: string; title: string | null }
interface TailorRun {
  run_id: string
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED'
  tailored_resume: CanonicalResume | null
  change_summary: string[]
  missing_requirements: string[]
  error_message: string | null
}

async function responseError(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json()
    if (body && typeof body === 'object' && 'error' in body && typeof body.error === 'string') return body.error
  } catch { /* A proxy error might return HTML. */ }
  return `Request failed (HTTP ${response.status}). Please try again.`
}
const errorMessage = (error: unknown) => error instanceof Error ? error.message : 'Something went wrong. Please try again.'

function usePathname() {
  const [pathname, setPathname] = useState(window.location.pathname)
  useEffect(() => {
    const update = () => setPathname(window.location.pathname)
    window.addEventListener('popstate', update)
    return () => window.removeEventListener('popstate', update)
  }, [])
  function navigate(path: string) {
    window.history.pushState({}, '', path)
    setPathname(path)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  return { pathname, navigate }
}

function Brand() {
  return <a className="brand" href="/" aria-label="TailorCV home"><span className="brand-mark" aria-hidden="true">T</span><span>TailorCV</span></a>
}

function HomePage({ navigate }: { navigate: (path: string) => void }) {
  const [status, setStatus] = useState<ApiStatus>('checking')
  const [file, setFile] = useState<File | null>(null)
  const [resumeAction, setResumeAction] = useState<Action>('idle')
  const [resumeError, setResumeError] = useState('')
  const [resume, setResume] = useState<ResumeResponse | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [jobAction, setJobAction] = useState<Action>('idle')
  const [jobError, setJobError] = useState('')
  const [job, setJob] = useState<JobResponse | null>(null)
  const [tailorAction, setTailorAction] = useState<Action>('idle')
  const [tailorError, setTailorError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    fetch('/api/v1/health', { signal: controller.signal })
      .then(async response => {
        if (!response.ok || (await response.json()).status !== 'UP') throw new Error('API unavailable')
        setStatus('available')
      })
      .catch(() => { if (!controller.signal.aborted) setStatus('unavailable') })
    return () => controller.abort()
  }, [])

  async function uploadResume(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) { setResumeError('Choose a PDF file first.'); return }
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) { setResumeError('Choose a PDF file.'); return }
    if (file.size > 5 * 1024 * 1024) { setResumeError('PDF must be 5 MB or smaller.'); return }
    setResumeAction('submitting'); setResumeError(''); setResume(null); setTailorError('')
    try {
      const form = new FormData(); form.append('file', file)
      const response = await fetch('/api/v1/resumes', { method: 'POST', body: form })
      if (!response.ok) throw new Error(await responseError(response))
      setResume(await response.json() as ResumeResponse)
    } catch (error) { setResumeError(errorMessage(error)) }
    finally { setResumeAction('idle') }
  }

  async function createJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (description.trim().length < 30) { setJobError('Enter at least 30 characters for the job description.'); return }
    setJobAction('submitting'); setJobError(''); setJob(null); setTailorError('')
    try {
      const response = await fetch('/api/v1/jobs', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ title: title.trim(), description: description.trim() }) })
      if (!response.ok) throw new Error(await responseError(response))
      setJob(await response.json() as JobResponse)
    } catch (error) { setJobError(errorMessage(error)) }
    finally { setJobAction('idle') }
  }

  async function tailorResume() {
    if (!resume || !job) return
    setTailorAction('submitting'); setTailorError('')
    try {
      const response = await fetch('/api/v1/tailor', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ resume_id: resume.resume_id, job_id: job.job_id }) })
      if (!response.ok) throw new Error(await responseError(response))
      const run = await response.json() as TailorRun
      navigate(`/result/${run.run_id}`)
    } catch (error) { setTailorError(errorMessage(error)) }
    finally { setTailorAction('idle') }
  }

  const busy = resumeAction === 'submitting' || jobAction === 'submitting' || tailorAction === 'submitting'
  return <>
    <header className="site-header"><Brand /><div className={`api-state ${status}`} role="status"><span />{status === 'checking' ? 'Checking service' : status === 'available' ? 'Service available' : 'Service unavailable'}</div></header>
    <main className="home-shell">
      <section className="intro"><p className="context-line">A focused workspace for one application</p><h1>Shape your experience for the role in front of you.</h1><p>Upload a text-based resume, add the job description, and review a grounded draft before downloading it.</p></section>
      {status === 'unavailable' && <div className="notice error" role="alert"><strong>The service is not running.</strong><span>Start the local stack with <code>./start.sh</code>, then reload this page.</span></div>}
      <div className="workbench">
        <div className="workflow" aria-label="Tailoring workflow">
          <section className={`step ${resume ? 'complete' : ''}`}>
            <div className="step-number" aria-hidden="true">1</div><div className="step-body">
              <div className="step-heading"><div><h2>Add your resume</h2><p>Text-based PDF, up to 5 MB. Scans need OCR and are not supported.</p></div>{resume && <span className="done">Ready</span>}</div>
              <form onSubmit={uploadResume} className="form-stack"><label htmlFor="resume-file">Resume PDF</label><div className="file-control"><input id="resume-file" type="file" accept="application/pdf,.pdf" disabled={busy} onChange={event => { setFile(event.target.files?.[0] ?? null); setResumeError('') }} /><span>{file?.name ?? 'Choose a PDF'}</span></div><button className="secondary-button" type="submit" disabled={status !== 'available' || busy}>{resumeAction === 'submitting' ? 'Reading resume…' : resume ? 'Replace resume' : 'Upload and parse'}</button></form>
              {resumeError && <p role="alert" className="field-error">{resumeError}</p>}
            </div>
          </section>
          <section className={`step ${job ? 'complete' : ''}`}>
            <div className="step-number" aria-hidden="true">2</div><div className="step-body">
              <div className="step-heading"><div><h2>Add the role</h2><p>Paste the responsibilities and requirements you want to target.</p></div>{job && <span className="done">Ready</span>}</div>
              <form onSubmit={createJob} className="form-stack"><div><label htmlFor="job-title">Job title <span>(optional)</span></label><input id="job-title" value={title} disabled={busy} onChange={event => { setTitle(event.target.value); setJob(null) }} maxLength={200} placeholder="Software Engineer" /></div><div><div className="label-row"><label htmlFor="job-description">Job description</label><span>{description.length.toLocaleString()} / 20,000</span></div><textarea id="job-description" value={description} disabled={busy} onChange={event => { setDescription(event.target.value); setJobError(''); setJob(null) }} maxLength={20000} rows={9} placeholder="Paste the role description here…" /></div><button className="secondary-button" type="submit" disabled={status !== 'available' || busy}>{jobAction === 'submitting' ? 'Saving role…' : job ? 'Role saved' : 'Save job description'}</button></form>
              {jobError && <p role="alert" className="field-error">{jobError}</p>}
            </div>
          </section>
          <section className="generate-step"><div><h2>Create the tailored draft</h2><p>The AI may only select or reorder skills and experience evidence already in your resume.</p></div><button className="primary-button" type="button" onClick={tailorResume} disabled={status !== 'available' || !resume || !job || busy}>{tailorAction === 'submitting' ? 'Tailoring your resume…' : 'Tailor my resume'}</button>{tailorError && <p role="alert" className="field-error wide">{tailorError}</p>}</section>
        </div>
        <aside className="source-sheet" aria-label="Parsed resume snapshot"><div className="sheet-tab">Source snapshot</div>{resume ? <><h2>{resume.parsed_resume.name || 'Name not found'}</h2><p className="sheet-contact">{resume.parsed_resume.contact.email || 'Email not found'}</p><div className="sheet-rule" /><h3>Summary</h3><p>{resume.parsed_resume.summary || 'No summary was found.'}</p><h3>Skills</h3><p>{resume.parsed_resume.skills.length ? resume.parsed_resume.skills.join(' · ') : 'No skills were found.'}</p><h3>Experience</h3><p>{resume.parsed_resume.experience.length ? `${resume.parsed_resume.experience.length} role${resume.parsed_resume.experience.length === 1 ? '' : 's'} found` : 'No experience entries were found.'}</p><details><summary>Check extracted PDF text</summary><pre>{resume.raw_text}</pre></details></> : <div className="sheet-empty"><div className="paper-lines" aria-hidden="true"><i /><i /><i /><i /><i /></div><h2>Your parsed resume will appear here.</h2><p>Use it to check that headings and text were extracted correctly before tailoring.</p></div>}</aside>
      </div>
      <p className="privacy-note">Use fictional data for demos. This classroom application has no sign-in, and tailoring sends the selected content to the configured AI provider.</p>
    </main>
  </>
}

function ResultPage({ runId, navigate }: { runId: string; navigate: (path: string) => void }) {
  const validId = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(runId)
  const [run, setRun] = useState<TailorRun | null>(null)
  const [error, setError] = useState(validId ? '' : 'This result link does not contain a valid run ID.')
  const [loading, setLoading] = useState(validId)
  const [previewHtml, setPreviewHtml] = useState('')
  const [previewError, setPreviewError] = useState('')
  async function loadResult() {
    if (!validId) return
    setLoading(true); setError(''); setPreviewError(''); setPreviewHtml('')
    try {
      const response = await fetch(`/api/v1/tailor/${runId}`)
      if (!response.ok) throw new Error(await responseError(response))
      const data = await response.json() as TailorRun; setRun(data)
      if (data.status === 'SUCCEEDED') {
        const preview = await fetch(`/api/v1/tailor/${runId}/preview`)
        if (!preview.ok) setPreviewError(await responseError(preview)); else setPreviewHtml(await preview.text())
      }
    } catch (reason) { setError(errorMessage(reason)); setRun(null) }
    finally { setLoading(false) }
  }
  useEffect(() => { void loadResult() }, [runId])
  const succeeded = run?.status === 'SUCCEEDED'
  return <><header className="site-header"><Brand /><button className="text-button" onClick={() => navigate('/')}>Start again</button></header><main className="result-shell">
    <div className="result-heading"><div><p className="context-line">Your tailored draft</p><h1>Review every change before you use it.</h1></div>{succeeded && <a className="primary-button download" href={`/api/v1/tailor/${runId}/pdf`} download>Download PDF</a>}</div>
    {loading && <div className="loading-panel" role="status"><span /><p>Loading your tailored resume…</p></div>}
    {error && <div className="notice error" role="alert"><strong>We couldn’t open this result.</strong><span>{error}</span><button className="secondary-button" onClick={loadResult} disabled={!validId}>Try again</button></div>}
    {run && run.status !== 'SUCCEEDED' && <div className="notice error" role="alert"><strong>{run.status === 'FAILED' ? 'Tailoring did not complete.' : 'This draft is still being prepared.'}</strong><span>{run.error_message || 'Return to the workspace and try the tailoring step again.'}</span></div>}
    {succeeded && <div className="result-grid"><section className="preview-frame" aria-labelledby="preview-title"><div className="preview-bar"><h2 id="preview-title">Resume preview</h2><span>A4 · single column</span></div>{previewHtml ? <iframe title="Tailored resume preview" srcDoc={previewHtml} /> : previewError ? <div className="preview-error" role="alert"><strong>Preview unavailable</strong><p>{previewError}</p><button className="secondary-button" onClick={loadResult}>Reload preview</button></div> : <div className="loading-panel" role="status"><span /><p>Rendering preview…</p></div>}</section><aside className="review-panel">
      <section><h2>What changed</h2>{run.change_summary.length ? <ul className="review-list changes">{run.change_summary.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}</ul> : <p className="empty-copy">No material changes were reported.</p>}</section>
      <section><h2>Requirements not found</h2><p className="section-help">These job requirements were not verified in your source resume.</p>{run.missing_requirements.length ? <ul className="review-list missing">{run.missing_requirements.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}</ul> : <p className="empty-copy success">No unverified requirements were reported.</p>}</section>
      <div className="review-warning"><strong>Review before sending</strong><p>This is an AI-generated draft, not an ATS guarantee. Confirm every statement and date against your original resume.</p></div><a className="primary-button download mobile-download" href={`/api/v1/tailor/${runId}/pdf`} download>Download PDF</a>
    </aside></div>}
  </main></>
}

export default function App() {
  const { pathname, navigate } = usePathname()
  const match = pathname.match(/^\/result\/([^/]+)\/?$/)
  return match ? <ResultPage runId={decodeURIComponent(match[1])} navigate={navigate} /> : <HomePage navigate={navigate} />
}
