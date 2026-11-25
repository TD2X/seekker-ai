export type Role = 'user' | 'assistant'

export interface Message {
  id: string
  role: Role
  text: string
  /** Names of the PDFs attached to a user message (UI only for now: nothing is uploaded yet). */
  files?: string[]
}

export interface Chat {
  id: string
  title: string
  createdAt: number
  messages: Message[]
}
