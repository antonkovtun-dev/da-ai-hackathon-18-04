import type { Message } from './messages'

export const uploadAttachment = (roomId: string, file: File, comment?: string): Promise<Message> => {
  const form = new FormData()
  form.append('file', file)
  if (comment) form.append('comment', comment)
  return fetch(`/api/rooms/${roomId}/attachments`, {
    method: 'POST',
    credentials: 'include',
    body: form,
  }).then(res => {
    if (!res.ok) throw Object.assign(new Error(res.statusText), { status: res.status })
    return res.json()
  })
}
