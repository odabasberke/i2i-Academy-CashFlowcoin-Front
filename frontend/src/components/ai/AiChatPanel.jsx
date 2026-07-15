import { useState, useRef, useEffect } from 'react'
import { askAi } from '../../api/aiApi'
import Spinner from '../common/Spinner'

const INTRO_MESSAGE = {
  role: 'assistant',
  content: 'Ask me about your portfolio, holdings, or recent trades.',
}

export default function AiChatPanel() {
  const [messages, setMessages] = useState([INTRO_MESSAGE])
  const [question, setQuestion] = useState('')
  const [isSending, setIsSending] = useState(false)
  const scrollRef = useRef(null)

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, isSending])

  async function handleSubmit(event) {
    event.preventDefault()
    const trimmed = question.trim()
    if (!trimmed || isSending) {
      return
    }

    // Conversation history lives client-side only - the backend answers
    // each question independently, rebuilding fresh portfolio/price
    // context every time rather than remembering earlier turns.
    setMessages((current) => [...current, { role: 'user', content: trimmed }])
    setQuestion('')
    setIsSending(true)

    try {
      const response = await askAi(trimmed)
      setMessages((current) => [...current, { role: 'assistant', content: response.answer }])
    } catch {
      // The interceptor already toasted the specific error - this bubble
      // just keeps the conversation coherent instead of leaving it hanging.
      setMessages((current) => [
        ...current,
        { role: 'assistant', content: "I couldn't reach the AI service just now. Please try again.", isError: true },
      ])
    } finally {
      setIsSending(false)
    }
  }

  return (
    <section className="flex h-full flex-col rounded-xl border border-hairline bg-panel">
      <div className="border-b border-hairline px-4 py-3">
        <h2 className="font-display text-sm font-semibold uppercase tracking-wide text-text-muted">
          AI Insights
        </h2>
      </div>

      <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto px-4 py-4">
        {messages.map((message, i) => (
          <ChatBubble key={i} message={message} />
        ))}
        {isSending && <TypingIndicator />}
      </div>

      <form onSubmit={handleSubmit} className="flex gap-2 border-t border-hairline p-3">
        <input
          type="text"
          value={question}
          onChange={(event) => setQuestion(event.target.value)}
          placeholder="How is my portfolio doing?"
          className="flex-1 rounded-md border border-hairline bg-ink px-3 py-2 text-sm outline-none focus:border-signal"
        />
        <button
          type="submit"
          disabled={isSending || !question.trim()}
          className="flex items-center gap-2 rounded-md bg-signal px-4 py-2 text-sm font-semibold text-ink transition hover:bg-signal/90 disabled:cursor-not-allowed disabled:opacity-40"
        >
          {isSending ? <Spinner className="h-4 w-4" /> : 'Ask'}
        </button>
      </form>
    </section>
  )
}

function ChatBubble({ message }) {
  const isUser = message.role === 'user'
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[85%] rounded-lg px-3 py-2 text-sm leading-relaxed whitespace-pre-wrap ${
          isUser ? 'bg-signal/15 text-text' : message.isError ? 'bg-sell/10 text-sell' : 'bg-panel-raised text-text'
        }`}
      >
        {message.content}
      </div>
    </div>
  )
}

function TypingIndicator() {
  return (
    <div className="flex justify-start">
      <div className="flex items-center gap-1.5 rounded-lg bg-panel-raised px-3 py-2.5">
        <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-text-muted [animation-delay:-0.3s]" />
        <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-text-muted [animation-delay:-0.15s]" />
        <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-text-muted" />
      </div>
    </div>
  )
}
