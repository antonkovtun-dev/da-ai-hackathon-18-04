import { useEffect, useRef } from 'react'
import { sendHeartbeat } from '../api/presence'

function getTabId(): string {
  let id = sessionStorage.getItem('presence_tab_id')
  if (!id) {
    id = crypto.randomUUID()
    sessionStorage.setItem('presence_tab_id', id)
  }
  return id
}

export function usePresenceHeartbeat() {
  const tabId = useRef(getTabId())

  useEffect(() => {
    const beat = () => sendHeartbeat(tabId.current, document.hasFocus()).catch(() => {})

    beat()

    const interval = setInterval(beat, 30_000)

    const onVisibility = () => beat()
    document.addEventListener('visibilitychange', onVisibility)

    return () => {
      clearInterval(interval)
      document.removeEventListener('visibilitychange', onVisibility)
    }
  }, [])
}
