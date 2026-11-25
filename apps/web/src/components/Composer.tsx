import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent, KeyboardEvent } from 'react'
import { formatSize, uploadPdf } from '../services/upload'
import type { UploadHandle } from '../services/upload'

interface Props {
  placeholder: string
  onSend: (text: string, fileNames: string[]) => void
}

interface Attachment {
  id: string
  name: string
  size: number
  progress: number
  status: 'uploading' | 'done'
}

let counter = 0

export default function Composer({ placeholder, onSend }: Props) {
  const [text, setText] = useState('')
  const [items, setItems] = useState<Attachment[]>([])
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const handles = useRef(new Map<string, UploadHandle>())

  const uploading = items.some((i) => i.status === 'uploading')
  const ready = items.filter((i) => i.status === 'done')
  const canSend = !uploading && (text.trim().length > 0 || ready.length > 0)

  // grow the textarea with its content (up to the CSS max-height)
  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${el.scrollHeight}px`
  }, [text])

  // stop running uploads if the composer disappears
  useEffect(() => {
    const map = handles.current
    return () => map.forEach((h) => h.cancel())
  }, [])

  function patch(id: string, change: Partial<Attachment>) {
    setItems((prev) => prev.map((i) => (i.id === id ? { ...i, ...change } : i)))
  }

  function submit() {
    if (!canSend) return
    onSend(text, ready.map((i) => i.name))
    setText('')
    setItems([])
    handles.current.clear()
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    // Enter sends, Shift+Enter makes a new line; ignore Enter used to confirm an IME composition
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault()
      submit()
    }
  }

  function handleFiles(e: ChangeEvent<HTMLInputElement>) {
    const picked = Array.from(e.target.files ?? []).filter(
      (f) => f.type === 'application/pdf' || f.name.toLowerCase().endsWith('.pdf'),
    )
    e.target.value = '' // allow picking the same file again

    const added: Attachment[] = picked.map((f) => ({
      id: `att-${++counter}`,
      name: f.name,
      size: f.size,
      progress: 0,
      status: 'uploading',
    }))
    setItems((prev) => [...prev, ...added])

    picked.forEach((file, idx) => {
      const id = added[idx].id
      const handle = uploadPdf(
        file,
        (p) => patch(id, { progress: p }),
        () => {
          patch(id, { progress: 100, status: 'done' })
          handles.current.delete(id)
        },
      )
      handles.current.set(id, handle)
    })
  }

  function removeItem(id: string) {
    handles.current.get(id)?.cancel()
    handles.current.delete(id)
    setItems((prev) => prev.filter((i) => i.id !== id))
  }

  return (
    <div className="composer">
      {items.length > 0 && (
        <ul className="attachments" aria-label="Επιλεγμένα PDF">
          {items.map((it) => (
            <li key={it.id} className={`attachment ${it.status}`}>
              <div className="att-icon" aria-hidden="true">PDF</div>
              <div className="att-body">
                <div className="att-top">
                  <span className="att-name" title={it.name}>{it.name}</span>
                  <span className="att-status">
                    {it.status === 'done' ? '✓ Ανέβηκε' : `${it.progress}%`}
                  </span>
                </div>
                <div className="att-sub">{formatSize(it.size)}</div>
                <div
                  className="progress"
                  role="progressbar"
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={it.progress}
                  aria-label={`Μεταφόρτωση ${it.name}`}
                >
                  <div className="progress-fill" style={{ width: `${it.progress}%` }} />
                </div>
              </div>
              <button
                type="button"
                className="att-remove"
                onClick={() => removeItem(it.id)}
                aria-label={`Αφαίρεση ${it.name}`}
              >
                ×
              </button>
            </li>
          ))}
        </ul>
      )}

      <textarea
        ref={textareaRef}
        value={text}
        onChange={(e) => setText(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        rows={1}
        aria-label="Μήνυμα"
      />

      <div className="composer-actions">
        <button
          type="button"
          className="icon-button attach"
          onClick={() => fileInputRef.current?.click()}
          aria-label="Προσθήκη PDF"
          title="Προσθήκη PDF"
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="application/pdf,.pdf"
          multiple
          hidden
          onChange={handleFiles}
        />

        <button
          type="button"
          className="send-button"
          onClick={submit}
          disabled={!canSend}
          aria-label="Αποστολή"
          title={uploading ? 'Περίμενε να ολοκληρωθεί η μεταφόρτωση' : 'Αποστολή'}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M12 19V5M5 12l7-7 7 7" />
          </svg>
        </button>
      </div>
    </div>
  )
}
