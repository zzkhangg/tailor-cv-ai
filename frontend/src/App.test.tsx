import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import App from './App'

afterEach(() => { cleanup(); vi.unstubAllGlobals() })

const health = { ok: true, json: async () => ({ status: 'UP' }) }

test('shows loading then API availability', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(health))
  render(<App />)
  expect(screen.getByRole('status')).toHaveTextContent('Checking')
  expect(await screen.findByText(/API: Available/)).toBeInTheDocument()
})

test('shows a useful error when the API is unavailable', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))
  render(<App />)
  expect(await screen.findByText(/API: Unavailable/)).toBeInTheDocument()
  expect(screen.getByRole('button', { name: /Upload and parse PDF/ })).toBeDisabled()
})

test('uploads a PDF and displays parsed resume data', async () => {
  const fetchMock = vi.fn().mockResolvedValueOnce(health).mockResolvedValueOnce({
    ok: true, json: async () => ({ resume_id: 'resume-123', parsed_resume: {
      name: 'Jane Doe', contact: { email: 'jane@example.com', phone: '', location: '' },
      summary: 'Software student', skills: ['Python'], experience: [],
    } }),
  })
  vi.stubGlobal('fetch', fetchMock)
  render(<App />)
  await screen.findByText(/API: Available/)
  fireEvent.change(screen.getByLabelText('Resume PDF'), {
    target: { files: [new File(['%PDF-1.4'], 'sample.pdf', { type: 'application/pdf' })] },
  })
  fireEvent.click(screen.getByRole('button', { name: /Upload and parse PDF/ }))
  expect(await screen.findByText('Resume saved and parsed')).toBeInTheDocument()
  expect(screen.getByText('Jane Doe')).toBeInTheDocument()
  expect(screen.getByText('resume-123', { exact: false })).toBeInTheDocument()
  expect(fetchMock).toHaveBeenLastCalledWith('/api/v1/resumes', expect.objectContaining({ method: 'POST', body: expect.any(FormData) }))
})

test('creates a job and shows its ID', async () => {
  const fetchMock = vi.fn().mockResolvedValueOnce(health).mockResolvedValueOnce({
    ok: true, json: async () => ({ job_id: 'job-456', title: 'Engineer' }),
  })
  vi.stubGlobal('fetch', fetchMock)
  render(<App />)
  await screen.findByText(/API: Available/)
  fireEvent.change(screen.getByLabelText('Job title (optional)'), { target: { value: 'Engineer' } })
  fireEvent.change(screen.getByLabelText('Job description'), { target: { value: 'Build reliable web services for a small team.' } })
  fireEvent.click(screen.getByRole('button', { name: /Save job description/ }))
  expect(await screen.findByText('Job description saved')).toBeInTheDocument()
  expect(screen.getByText('job-456', { exact: false })).toBeInTheDocument()
  expect(fetchMock).toHaveBeenLastCalledWith('/api/v1/jobs', expect.objectContaining({ method: 'POST' }))
})

test('reports backend upload errors', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(health).mockResolvedValueOnce({
    ok: false, status: 422, json: async () => ({ error: 'No extractable text found.' }),
  }))
  render(<App />)
  await screen.findByText(/API: Available/)
  fireEvent.change(screen.getByLabelText('Resume PDF'), {
    target: { files: [new File(['%PDF-1.4'], 'scan.pdf', { type: 'application/pdf' })] },
  })
  fireEvent.click(screen.getByRole('button', { name: /Upload and parse PDF/ }))
  expect(await screen.findByRole('alert')).toHaveTextContent('No extractable text found.')
})
