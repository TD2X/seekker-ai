import { useCallback, useState } from 'react'
import Background from './components/Background'
import Composer from './components/Composer'
import ChatView from './components/ChatView'
import Sidebar, { SidebarToggleIcon } from './components/Sidebar'
import ThemeToggle from './components/ThemeToggle'
import { useChats } from './hooks/useChats'
import { useTheme } from './hooks/useTheme'

const SIDEBAR_KEY = 'seekker.sidebar.collapsed'

export default function App() {
  const { theme, toggle } = useTheme()
  const { chats, activeChat, activeId, typingId, startNewChat, openChat, send } = useChats()

  const [collapsed, setCollapsed] = useState<boolean>(() => {
    try {
      return localStorage.getItem(SIDEBAR_KEY) === '1'
    } catch {
      return false
    }
  })

  const toggleSidebar = useCallback(() => {
    setCollapsed((c) => {
      try {
        localStorage.setItem(SIDEBAR_KEY, c ? '0' : '1')
      } catch {
        // ignore
      }
      return !c
    })
  }, [])

  return (
    <>
    <Background />
    <div className={`app${collapsed ? ' collapsed' : ''}`}>
      <Sidebar
        chats={chats}
        activeId={activeId}
        collapsed={collapsed}
        onToggle={toggleSidebar}
        onNewChat={startNewChat}
        onOpenChat={openChat}
      />

      <main className="main">
        <header className="topbar">
          <div className="topbar-left">
            {collapsed && (
              <button
                type="button"
                className="icon-button reveal"
                onClick={toggleSidebar}
                aria-label="Εμφάνιση πλαϊνής μπάρας"
                title="Εμφάνιση πλαϊνής μπάρας"
              >
                <SidebarToggleIcon />
              </button>
            )}
            {activeChat && <span className="topbar-title">{activeChat.title}</span>}
          </div>
          <ThemeToggle theme={theme} onToggle={toggle} />
        </header>

        {activeChat ? (
          <>
            <ChatView chat={activeChat} typing={typingId === activeChat.id} />
            <div className="composer-dock">
              <Composer key={activeChat.id} placeholder="Κάνε μια ερώτηση για τα PDF σου" onSend={send} />
            </div>
          </>
        ) : (
          <div className="home">
            <h1>Τι θέλεις να βρούμε στα PDF σου;</h1>
            <p className="home-sub">Ανέβασε έγγραφα και ρώτα με φυσική γλώσσα.</p>
            <Composer key="home" placeholder="Κάνε μια ερώτηση ή πρόσθεσε ένα PDF" onSend={send} />
          </div>
        )}
      </main>
    </div>
    </>
  )
}
