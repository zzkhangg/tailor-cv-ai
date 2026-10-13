import { useEffect, useState } from 'react'

type Status = 'checking' | 'available' | 'unavailable'

export default function App() {
  const [status, setStatus] = useState<Status>('checking')

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

  return <main className="min-h-screen bg-slate-50 px-6 py-16 text-slate-900">
    <div className="mx-auto max-w-2xl rounded-2xl bg-white p-8 shadow-sm">
      <h1 className="text-3xl font-bold">TailorCV</h1>
      <p className="mt-4 text-slate-600">Resume tailoring is being built. Phase 1 establishes the services and their connection.</p>
      <p className="mt-8 rounded-lg bg-slate-100 p-4" role="status">
        API: {status === 'checking' ? 'Checking…' : status === 'available' ? 'Available' : 'Unavailable. Check that resume-service is running.'}
      </p>
    </div>
  </main>
}
