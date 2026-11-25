import { useMemo, useState } from 'react'
import type { Chat } from '../types'
import Logo from './Logo'

interface Props {
  chats: Chat[]
  activeId: string | null
  collapsed: boolean
  onToggle: () => void
  onNewChat: () => void
  onOpenChat: (id: string) => void
}

export function SidebarToggleIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="3" y="4" width="18" height="16" rx="3" />
      <path d="M9 4v16" />
    </svg>
  )
}

export default function Sidebar({ chats, activeId, collapsed, onToggle, onNewChat, onOpenChat }: Props) {
  const [query, setQuery] = useState('')

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase()
    return q ? chats.filter((c) => c.title.toLowerCase().includes(q)) : chats
  }, [chats, query])

  return (
    <aside className="sidebar" aria-hidden={collapsed} inert={collapsed}>
      <div className="sidebar-inner">
        <div className="brand">
          <Logo />
          <span className="brand-name">Seekker AI</span>
          <button
            type="button"
            className="icon-button collapse"
            onClick={onToggle}
            aria-label="Απόκρυψη πλαϊνής μπάρας"
            title="Απόκρυψη πλαϊνής μπάρας"
          >
            <SidebarToggleIcon />
          </button>
        </div>

        <div className="side-section">
          <button type="button" className="new-chat" onClick={onNewChat}>
            <span className="plus" aria-hidden="true">+</span>
            Νέα συνομιλία
          </button>

          <div className="search">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.5-3.5" />
            </svg>
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Αναζήτηση συνομιλιών"
              aria-label="Αναζήτηση συνομιλιών"
            />
          </div>
        </div>

        <div className="history-wrap">
          <div className="history-label">Ιστορικό</div>
          <nav className="history" aria-label="Ιστορικό συνομιλιών">
            {chats.length === 0 && <p className="empty">Δεν υπάρχουν συνομιλίες ακόμα.</p>}
            {chats.length > 0 && visible.length === 0 && <p className="empty">Κανένα αποτέλεσμα.</p>}
            {visible.map((chat) => (
              <button
                key={chat.id}
                type="button"
                className={`history-item${chat.id === activeId ? ' active' : ''}`}
                onClick={() => onOpenChat(chat.id)}
                title={chat.title}
              >
                {chat.title}
              </button>
            ))}
          </nav>
        </div>
      </div>
    </aside>
  )
}
