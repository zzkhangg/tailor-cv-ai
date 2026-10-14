import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import App from './App'

const runId = '123e4567-e89b-42d3-a456-426614174000'
const health = { ok: true, json: async () => ({ status: 'UP' }) }
const parsed = {
  name: 'Jane Doe', contact: { email: 'jane@example.com', phone: '', location: 'Brisbane' },
  summary: 'Software student', skills: ['Python'], experience: [], projects: [], education: [],
}

beforeEach(() => {
  window.history.replaceState({}, '', '/')
  vi.stubGlobal('scrollTo', vi.fn())
})
afterEach(() => { cleanup(); vi.unstubAllGlobals() })

test('shows API availability and keeps tailoring disabled initially', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(health))
  render(<App />)
  expect(screen.getByRole('status')).toHaveTextContent('Checking')
  expect(await screen.findByText('Service available')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Tailor my resume' })).toBeDisabled()
})

test('shows useful unavailable state and disables upload', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))
  render(<App />)
  expect(await screen.findByText('Service unavailable')).toBeInTheDocument()
  expect(screen.getByRole('alert')).toHaveTextContent('Start the local stack')
  expect(screen.getByRole('button', { name: 'Upload and parse' })).toBeDisabled()
})

test('completes upload, job save, and tailoring navigation', async () => {
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(health)
    .mockResolvedValueOnce({ ok: true, json: async () => ({ resume_id: 'resume-123', raw_text: 'Jane Doe\nSkills: Python', parsed_resume: parsed }) })
    .mockResolvedValueOnce({ ok: true, json: async () => ({ job_id: 'job-456', title: 'Engineer' }) })
    .mockResolvedValueOnce({ ok: true, json: async () => ({ run_id: runId, status: 'SUCCEEDED' }) })
    .mockResolvedValueOnce({ ok: true, json: async () => ({ run_id: runId, status: 'SUCCEEDED', tailored_resume: parsed, change_summary: ['Reordered skills.'], missing_requirements: [], error_message: null }) })
    .mockResolvedValueOnce({ ok: true, text: async () => '<html><body>Jane Doe</body></html>' })
  vi.stubGlobal('fetch', fetchMock)
  render(<App />)
  await screen.findByText('Service available')

  fireEvent.change(screen.getByLabelText('Resume PDF'), { target: { files: [new File(['%PDF-1.4'], 'sample.pdf', { type: 'application/pdf' })] } })
  fireEvent.click(screen.getByRole('button', { name: 'Upload and parse' }))
  expect(await screen.findByText('Jane Doe')).toBeInTheDocument()
  expect(screen.getByText('Check extracted PDF text')).toBeInTheDocument()

  fireEvent.change(screen.getByLabelText(/Job title/), { target: { value: 'Engineer' } })
  fireEvent.change(screen.getByLabelText('Job description'), { target: { value: 'Build reliable web services for a small product team.' } })
  fireEvent.click(screen.getByRole('button', { name: 'Save job description' }))
  expect(await screen.findByText('Role saved')).toBeInTheDocument()
  const tailor = screen.getByRole('button', { name: 'Tailor my resume' })
  expect(tailor).toBeEnabled()
  fireEvent.click(tailor)

  expect(await screen.findByRole('heading', { name: 'Resume preview' })).toBeInTheDocument()
  expect(window.location.pathname).toBe(`/result/${runId}`)
  expect(fetchMock).toHaveBeenCalledWith('/api/v1/tailor', expect.objectContaining({ method: 'POST' }))
})

test('validates job length without calling the jobs endpoint', async () => {
  const fetchMock = vi.fn().mockResolvedValue(health)
  vi.stubGlobal('fetch', fetchMock)
  render(<App />)
  await screen.findByText('Service available')
  fireEvent.change(screen.getByLabelText('Job description'), { target: { value: 'Too short' } })
  fireEvent.click(screen.getByRole('button', { name: 'Save job description' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('at least 30 characters')
  expect(fetchMock).toHaveBeenCalledTimes(1)
})

test('loads a result directly with preview, summaries, and download', async () => {
  window.history.replaceState({}, '', `/result/${runId}`)
  const fetchMock = vi.fn()
    .mockResolvedValueOnce({ ok: true, json: async () => ({ run_id: runId, status: 'SUCCEEDED', tailored_resume: parsed, change_summary: ['Reordered skills.'], missing_requirements: ['Kubernetes'], error_message: null }) })
    .mockResolvedValueOnce({ ok: true, text: async () => '<html><body>Jane Doe</body></html>' })
  vi.stubGlobal('fetch', fetchMock)
  render(<App />)
  expect(await screen.findByText('Reordered skills.')).toBeInTheDocument()
  expect(screen.getByText('Kubernetes')).toBeInTheDocument()
  expect(screen.getByTitle('Tailored resume preview')).toHaveAttribute('srcdoc', expect.stringContaining('Jane Doe'))
  expect(screen.getAllByRole('link', { name: 'Download PDF' })[0]).toHaveAttribute('href', `/api/v1/tailor/${runId}/pdf`)
})

test('handles invalid IDs and failed result requests', async () => {
  window.history.replaceState({}, '', '/result/not-a-uuid')
  const fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
  const { unmount } = render(<App />)
  expect(screen.getByRole('alert')).toHaveTextContent('valid run ID')
  expect(fetchMock).not.toHaveBeenCalled()
  unmount()

  window.history.replaceState({}, '', `/result/${runId}`)
  fetchMock.mockResolvedValueOnce({ ok: false, status: 404, json: async () => ({ error: 'Tailoring run not found.' }) })
  render(<App />)
  expect(await screen.findByRole('alert')).toHaveTextContent('Tailoring run not found.')
  await waitFor(() => expect(screen.getByRole('button', { name: 'Try again' })).toBeEnabled())
})
