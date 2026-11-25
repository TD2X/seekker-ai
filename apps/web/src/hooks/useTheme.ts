import { useCallback, useEffect, useState } from 'react'

export type Theme = 'light' | 'dark'

const KEY = 'seekker.theme'

function initialTheme(): Theme {
  // index.html already applied the right theme to <html>; read it back so React agrees with it.
  const applied = document.documentElement.getAttribute('data-theme')
  return applied === 'dark' ? 'dark' : 'light'
}

export function useTheme() {
  const [theme, setTheme] = useState<Theme>(initialTheme)

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    try {
      localStorage.setItem(KEY, theme)
    } catch {
      // storage can be unavailable (private mode): the theme just won't be remembered
    }
  }, [theme])

  const toggle = useCallback(() => setTheme((t) => (t === 'light' ? 'dark' : 'light')), [])

  return { theme, toggle }
}
