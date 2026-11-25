import { useEffect, useRef } from 'react'
import type { Chat } from '../types'
import Logo from './Logo'

interface Props {
  chat: Chat
  typing: boolean
}

export default function ChatView({ chat, typing }: Props) {
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    endRef.current?.scrollIntoView({ block: 'end', behavior: 'smooth' })
  }, [chat.messages.length, chat.id, typing])

  return (
    <div className="messages" role="log" aria-live="polite">
      <div className="messages-inner">
        {chat.messages.map((m) => (
          <div key={m.id} className={`message ${m.role}`}>
            {m.role === 'assistant' && (
              <div className="avatar">
                <Logo size={26} />
              </div>
            )}
            <div className="bubble">
              {m.files && m.files.length > 0 && (
                <ul className="file-chips in-message">
                  {m.files.map((name, i) => (
                    <li key={`${name}-${i}`} className="chip">
                      <span className="chip-pdf">PDF</span>
                      <span className="chip-name" title={name}>{name}</span>
                    </li>
                  ))}
                </ul>
              )}
              {m.text && <p>{m.text}</p>}
            </div>
          </div>
        ))}

        {typing && (
          <div className="message assistant">
            <div className="avatar">
              <Logo size={26} />
            </div>
            <div className="bubble typing" aria-label="Γράφει…">
              <span /><span /><span />
            </div>
          </div>
        )}
        <div ref={endRef} />
      </div>
    </div>
  )
}
