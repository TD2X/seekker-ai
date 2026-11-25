/**
 * PDF upload service.
 *
 * TODO(backend): for now the upload is SIMULATED so the UI can show a progress bar.
 * When the API has its upload endpoint, replace the body of `uploadPdf` with an
 * XMLHttpRequest (xhr.upload.onprogress gives the real percentage) and keep the same signature.
 */
export interface UploadHandle {
  cancel: () => void
}

export function uploadPdf(
  file: File,
  onProgress: (percent: number) => void,
  onDone: () => void,
): UploadHandle {
  // pretend speed: ~2 MB/s, but never faster than 1.5 s or slower than 6 s
  const duration = Math.min(6000, Math.max(1500, (file.size / (2 * 1024 * 1024)) * 1000))
  const start = performance.now()

  const timer = window.setInterval(() => {
    const t = Math.min(1, (performance.now() - start) / duration)
    const eased = 1 - Math.pow(1 - t, 2)
    onProgress(Math.round(eased * 100))
    if (t >= 1) {
      window.clearInterval(timer)
      onDone()
    }
  }, 80)

  return { cancel: () => window.clearInterval(timer) }
}

export function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
