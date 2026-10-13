import { render, screen } from '@testing-library/react'
import { afterEach, expect, test, vi } from 'vitest'
import App from './App'

afterEach(() => vi.unstubAllGlobals())

test('shows loading then API availability', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ status: 'UP' }) }))
  render(<App />)
  expect(screen.getByRole('status')).toHaveTextContent('Checking')
  expect(await screen.findByText(/API: Available/)).toBeInTheDocument()
})

test('shows a useful error when the API is unavailable', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')))
  render(<App />)
  expect(await screen.findByText(/API: Unavailable/)).toBeInTheDocument()
})
