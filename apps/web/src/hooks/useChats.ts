import { useCallback, useEffect, useRef, useState } from 'react'
import type { Chat, Message } from '../types'

const KEY = 'seekker.chats.v1'

// Temporary answer: the search backend (API + workers) is not connected to this UI yet.
const PLACEHOLDER_REPLY =
  'Η αναζήτηση δεν είναι ακόμα συνδεδεμένη με το backend. Αυτή είναι προσωρινή απάντηση του περιβάλλοντος.'

function uid(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function load(): Chat[] {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as Chat[]) : []
  } catch {
    return []
  }
}

function makeTitle(text: string, files: string[]): string {
  const base = text.trim() || files[0] || 'Νέα συνομιλία'
  return base.length > 40 ? `${base.slice(0, 40)}…` : base
}

export function useChats() {
  const [chats, setChats] = useState<Chat[]>(load)
  // null = "new chat" screen: the chat is created when the first message is sent
  const [activeId, setActiveId] = useState<string | null>(null)
  // id of the chat that is "typing" an answer right now
  const [typingId, setTypingId] = useState<string | null>(null)
  const timers = useRef<number[]>([])

  useEffect(() => {
    try {
      localStorage.setItem(KEY, JSON.stringify(chats))
    } catch {
      // ignore: history just won't survive a reload
    }
  }, [chats])

  useEffect(() => {
    const list = timers.current
    return () => list.forEach((t) => window.clearTimeout(t))
  }, [])

  const activeChat = chats.find((c) => c.id === activeId) ?? null

  const startNewChat = useCallback(() => setActiveId(null), [])
  const openChat = useCallback((id: string) => setActiveId(id), [])

  const send = useCallback(
    (text: string, files: string[]) => {
      const userMessage: Message = { id: uid(), role: 'user', text: text.trim(), files }
      let chatId: string

      if (activeChat) {
        chatId = activeChat.id
        setChats((prev) =>
          prev.map((c) => (c.id === chatId ? { ...c, messages: [...c.messages, userMessage] } : c)),
        )
      } else {
        chatId = uid()
        const chat: Chat = {
          id: chatId,
          title: makeTitle(text, files),
          createdAt: Date.now(),
          messages: [userMessage],
        }
        setChats((prev) => [chat, ...prev])
        setActiveId(chatId)
      }

      // show "typing…" for a moment, then the placeholder answer
      setTypingId(chatId)
      const t = window.setTimeout(() => {
        const reply: Message = { id: uid(), role: 'assistant', text: PLACEHOLDER_REPLY }
        setChats((prev) =>
          prev.map((c) => (c.id === chatId ? { ...c, messages: [...c.messages, reply] } : c)),
        )
        setTypingId((cur) => (cur === chatId ? null : cur))
      }, 1100)
      timers.current.push(t)
    },
    [activeChat],
  )

  return { chats, activeChat, activeId, typingId, startNewChat, openChat, send }
}
