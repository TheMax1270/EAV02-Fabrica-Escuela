import { useCallback, useState } from 'react'
import { useParams } from 'react-router-dom'
import SiteHeader from '../components/SiteHeader'
import ConversationList from '../components/ConversationList'
import MessageThread from '../components/MessageThread'

export default function MessagesPage() {
  const { userId } = useParams()
  const [activity, setActivity] = useState(0)
  const handleActivity = useCallback(() => setActivity((value) => value + 1), [])

  return (
    <div className="app-shell">
      <SiteHeader />
      <main className="messages-main">
        <div className={`messages-layout ${userId ? 'has-thread' : ''}`}>
          <aside className="messages-panel conversation-panel" aria-labelledby="conversations-title">
            <p className="eyebrow">MENSAJES PRIVADOS</p>
            <h1 id="conversations-title">Conversaciones</h1>
            <ConversationList activeId={userId} refreshKey={activity} />
          </aside>
          <div className="messages-panel thread-panel">
            {userId
              ? <MessageThread key={userId} participantId={userId} onActivity={handleActivity} />
              : <p className="thread-placeholder">Selecciona una conversación o escribe desde el perfil de un desarrollador.</p>}
          </div>
        </div>
      </main>
    </div>
  )
}
