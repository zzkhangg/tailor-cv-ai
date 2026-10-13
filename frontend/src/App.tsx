import { useEffect, useState, type FormEvent } from 'react'

type Status = 'checking' | 'available' | 'unavailable'
type Action = 'idle' | 'submitting'

interface ParsedResume {
  name: string
  contact: { email: string; phone: string; location: string }
  summary: string
  skills: string[]
  experience: { company: string; title: string; start_date: string; end_date: string; bullets: string[] }[]
}
interface ResumeResponse { resume_id: string; parsed_resume: ParsedResume }
interface JobResponse { job_id: string; title: string | null }

async function responseError(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json()
    if (body && typeof body === 'object' && 'error' in body && typeof body.error === 'string') return body.error
  } catch { /* A proxy error might return HTML. */ }
  return `Request failed (HTTP ${response.status}). Please try again.`
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.'
}

export default function App() {
  const [status, setStatus] = useState<Status>('checking')
  const [file, setFile] = useState<File | null>(null)
  const [resumeAction, setResumeAction] = useState<Action>('idle')
  const [resumeError, setResumeError] = useState('')
  const [resume, setResume] = useState<ResumeResponse | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [jobAction, setJobAction] = useState<Action>('idle')
  const [jobError, setJobError] = useState('')
  const [job, setJob] = useState<JobResponse | null>(null)

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
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      setResumeError('Choose a PDF file.'); return
    }
    if (file.size > 5 * 1024 * 1024) { setResumeError('PDF must be 5 MB or smaller.'); return }
    setResumeAction('submitting')
    setResumeError('')
    setResume(null)
    try {
      const form = new FormData()
      form.append('file', file)
      const response = await fetch('/api/v1/resumes', { method: 'POST', body: form })
      if (!response.ok) throw new Error(await responseError(response))
      setResume(await response.json() as ResumeResponse)
    } catch (error) { setResumeError(errorMessage(error)) }
    finally { setResumeAction('idle') }
  }

  async function createJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (description.trim().length < 30) {
      setJobError('Enter at least 30 characters for the job description.'); return
    }
    setJobAction('submitting')
    setJobError('')
    setJob(null)
    try {
      const response = await fetch('/api/v1/jobs', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: title.trim(), description: description.trim() }),
      })
      if (!response.ok) throw new Error(await responseError(response))
      setJob(await response.json() as JobResponse)
    } catch (error) { setJobError(errorMessage(error)) }
    finally { setJobAction('idle') }
  }

  return <main className="min-h-screen bg-slate-50 px-4 py-10 text-slate-900 sm:px-6">
    <div className="mx-auto max-w-4xl space-y-8">
      <header>
        <p className="text-sm font-semibold uppercase tracking-widest text-indigo-700">TailorCV · Phase 2</p>
        <h1 className="mt-2 text-4xl font-bold tracking-tight">Your resume workspace</h1>
        <p className="mt-3 text-slate-600">Upload a text-based PDF and save a job description. Resume tailoring is coming in the next phase.</p>
        <p className={`mt-5 inline-block rounded-full px-4 py-2 text-sm font-medium ${status === 'available' ? 'bg-emerald-100 text-emerald-800' : status === 'unavailable' ? 'bg-rose-100 text-rose-800' : 'bg-slate-200 text-slate-700'}`} role="status">
          API: {status === 'checking' ? 'Checking...' : status === 'available' ? 'Available' : 'Unavailable. Start the stack with ./start.sh.'}
        </p>
      </header>

      <div className="grid gap-6 md:grid-cols-2">
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold">1. Upload your resume</h2>
          <p className="mt-2 text-sm text-slate-600">Text-based PDF only, up to 5 MB. Scanned PDFs are not supported.</p>
          <form onSubmit={uploadResume} className="mt-5 space-y-4">
            <label htmlFor="resume-file" className="block text-sm font-medium">Resume PDF</label>
            <input id="resume-file" type="file" accept="application/pdf,.pdf" onChange={event => { setFile(event.target.files?.[0] ?? null); setResumeError('') }} className="block w-full rounded-lg border border-slate-300 p-3 text-sm file:mr-3 file:rounded-md file:border-0 file:bg-indigo-50 file:px-3 file:py-2 file:font-semibold file:text-indigo-700" />
            <button type="submit" disabled={status !== 'available' || resumeAction === 'submitting'} className="rounded-lg bg-indigo-700 px-5 py-3 font-semibold text-white hover:bg-indigo-800 disabled:cursor-not-allowed disabled:opacity-50">
              {resumeAction === 'submitting' ? 'Uploading...' : 'Upload and parse PDF'}
            </button>
          </form>
          {resumeError && <p role="alert" className="mt-4 rounded-lg bg-rose-50 p-3 text-sm text-rose-800">{resumeError}</p>}
          {resume && <div className="mt-6 border-t border-slate-200 pt-5">
            <p className="font-semibold text-emerald-700">Resume saved and parsed</p>
            <p className="mt-1 break-all text-xs text-slate-500">Resume ID: {resume.resume_id}</p>
            <dl className="mt-4 space-y-3 text-sm">
              <div><dt className="font-semibold">Name</dt><dd>{resume.parsed_resume.name || 'Not found'}</dd></div>
              <div><dt className="font-semibold">Email</dt><dd>{resume.parsed_resume.contact.email || 'Not found'}</dd></div>
              <div><dt className="font-semibold">Summary</dt><dd className="whitespace-pre-wrap">{resume.parsed_resume.summary || 'Not found'}</dd></div>
              <div><dt className="font-semibold">Skills</dt><dd>{resume.parsed_resume.skills.length ? resume.parsed_resume.skills.join(', ') : 'Not found'}</dd></div>
            </dl>
          </div>}
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold">2. Save a job description</h2>
          <p className="mt-2 text-sm text-slate-600">Paste the role details. At least 30 characters are required.</p>
          <form onSubmit={createJob} className="mt-5 space-y-4">
            <div><label htmlFor="job-title" className="block text-sm font-medium">Job title (optional)</label><input id="job-title" value={title} onChange={event => setTitle(event.target.value)} maxLength={200} placeholder="Software Engineer" className="mt-2 w-full rounded-lg border border-slate-300 p-3" /></div>
            <div><label htmlFor="job-description" className="block text-sm font-medium">Job description</label><textarea id="job-description" value={description} onChange={event => { setDescription(event.target.value); setJobError('') }} maxLength={20000} rows={7} placeholder="Describe the responsibilities and requirements..." className="mt-2 w-full rounded-lg border border-slate-300 p-3" /></div>
            <button type="submit" disabled={status !== 'available' || jobAction === 'submitting'} className="rounded-lg bg-indigo-700 px-5 py-3 font-semibold text-white hover:bg-indigo-800 disabled:cursor-not-allowed disabled:opacity-50">
              {jobAction === 'submitting' ? 'Saving...' : 'Save job description'}
            </button>
          </form>
          {jobError && <p role="alert" className="mt-4 rounded-lg bg-rose-50 p-3 text-sm text-rose-800">{jobError}</p>}
          {job && <div className="mt-6 border-t border-slate-200 pt-5">
            <p className="font-semibold text-emerald-700">Job description saved</p>
            <p className="mt-1 break-all text-xs text-slate-500">Job ID: {job.job_id}</p>
            {job.title && <p className="mt-3 text-sm">{job.title}</p>}
          </div>}
        </section>
      </div>
      <p className="text-sm text-slate-500">Tailoring, preview and PDF download are planned for later phases. Use a fictional resume for demos; this single-user app has no sign-in yet.</p>
    </div>
  </main>
}
