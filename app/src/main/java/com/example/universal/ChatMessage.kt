package com.example.universal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

enum class MessageType { USER, AGENT, SYSTEM, ERROR, THINKING, USER_INPUT }

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis(),
    val code: String? = null,
    val thinking: String? = null,  // reasoning before final response
    val isExpanded: Boolean = false  // UI state for expand/collapse
)

class ChatAdapter(
    private val messages: MutableList<ChatMessage> = mutableListOf(),
    private val onCodeTap: ((String) -> Unit)? = null,
    private val onCopyCode: ((String) -> Unit)? = null,
    private val onExpandToggle: ((String, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_USER   = 0
        private const val VIEW_AGENT  = 1
        private const val VIEW_THINKING = 2
        private const val VIEW_SYSTEM = 3
        private const val VIEW_ERROR  = 4
    }

    override fun getItemViewType(position: Int) = when (messages[position].type) {
        MessageType.USER   -> VIEW_USER
        MessageType.AGENT  -> VIEW_AGENT
        MessageType.THINKING -> VIEW_THINKING
        MessageType.SYSTEM -> VIEW_SYSTEM
        MessageType.ERROR  -> VIEW_ERROR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_USER   -> UserViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false))
            VIEW_ERROR  -> ErrorViewHolder(inflater.inflate(R.layout.item_chat_error, parent, false))
            VIEW_THINKING -> ThinkingViewHolder(inflater.inflate(R.layout.item_chat_thinking, parent, false))
            VIEW_SYSTEM -> SystemViewHolder(inflater.inflate(R.layout.item_chat_system, parent, false))
            else        -> AgentViewHolder(inflater.inflate(R.layout.item_chat_agent, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val timeStr = fmt.format(java.util.Date(msg.timestamp))
        
        // Relative time
        val relativeTime = getRelativeTime(msg.timestamp)
        
        when (holder) {
            is UserViewHolder   -> holder.bind(msg, timeStr)
            is AgentViewHolder  -> holder.bind(msg, relativeTime, onCodeTap, onCopyCode) { expanded ->
                onExpandToggle?.invoke(msg.id, expanded)
            }
            is ThinkingViewHolder -> holder.bind()
            is SystemViewHolder -> holder.bind(msg)
            is ErrorViewHolder  -> holder.bind(msg)
        }
    }

    private fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            else -> java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(timestamp))
        }
    }

    override fun getItemCount() = messages.size

    // ── DiffUtil for efficient updates ───────────────────���───────────────────
    fun submitList(newMessages: List<ChatMessage>) {
        val diffCallback = ChatDiffCallback(messages.toList(), newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
    }

    // ── Legacy API ────────────────────────────────────────────────────────
    fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun updateLastMessage(text: String) {
        if (messages.isNotEmpty()) {
            val last = messages.last()
            messages[messages.size - 1] = last.copy(text = text)
            notifyItemChanged(messages.size - 1)
        }
    }

    fun updateThinking(newThinking: String, expanded: Boolean = true) {
        if (messages.isNotEmpty()) {
            val last = messages.last()
            if (last.type == MessageType.AGENT) {
                messages[messages.size - 1] = last.copy(thinking = newThinking, isExpanded = expanded)
                notifyItemChanged(messages.size - 1)
            }
        }
    }

    // Add thinking (inline in agent bubble, not separate bubble)
    fun showThinking(thinking: String) {
        if (messages.isNotEmpty()) {
            val last = messages.last()
            if (last.type == MessageType.AGENT) {
                messages[messages.size - 1] = last.copy(thinking = thinking, isExpanded = false)
                notifyItemChanged(messages.size - 1)
            }
        }
    }

    fun clear() {
        messages.clear()
        notifyDataSetChanged()
    }

    fun getMessages(): List<ChatMessage> = messages.toList()

    // ── DiffUtil Callback ─────────────────────────────────────────────────
    class ChatDiffCallback(
        private val oldList: List<ChatMessage>,
        private val newList: List<ChatMessage>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos].id == newList[newPos].id
        override fun areContentsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos] == newList[newPos]
    }

    // ── View Holders ──────────────────────────────────────────────────────

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.chatText)
        private val time: TextView = view.findViewById(R.id.chatTime)
        fun bind(msg: ChatMessage, timeStr: String) {
            text.text = msg.text
            time.text = timeStr
        }
    }

    class AgentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView  = view.findViewById(R.id.chatText)
        private val time: TextView  = view.findViewById(R.id.chatTime)
        private val code: TextView? = view.findViewById(R.id.chatCode)
        private val codeContainer: View? = view.findViewById(R.id.codeContainer)
        private val thinkingContainer: View? = view.findViewById(R.id.thinkingContainer)
        private val thinkingText: TextView? = view.findViewById(R.id.thinkingText)
        private val thinkingCard: View? = view.findViewById(R.id.thinkingCard)
        private val thinkingExpandIcon: View? = view.findViewById(R.id.thinkingExpandIcon)
        private val expandHint: TextView? = view.findViewById(R.id.expandHint)
        private val btnCopyCode: View? = view.findViewById(R.id.btnCopyCode)

        fun bind(
            msg: ChatMessage,
            relativeTime: String,
            onCodeTap: ((String) -> Unit)?,
            onCopyCode: ((String) -> Unit)?,
            onExpandToggle: ((Boolean) -> Unit)?
        ) {
            text.text = msg.text
            time.text = relativeTime

            // Thinking section
            if (!msg.thinking.isNullOrEmpty()) {
                thinkingContainer?.visibility = View.VISIBLE
                thinkingText?.text = msg.thinking.take(500) + if (msg.thinking.length > 500) "..." else ""
                expandHint?.visibility = View.VISIBLE
                
                // Toggle expand state
                thinkingCard?.setOnClickListener {
                    val newExpanded = !msg.isExpanded
                    onExpandToggle?.invoke(newExpanded)
                }
                
                // Update expand icon rotation
                val rotation = if (msg.isExpanded) 180f else 0f
                thinkingExpandIcon?.rotation = rotation
                
                // Show/hide thinking text based on expanded state
                thinkingText?.visibility = if (msg.isExpanded) View.VISIBLE else View.GONE
            } else {
                thinkingContainer?.visibility = View.GONE
                expandHint?.visibility = View.GONE
            }

            // Code block
            if (!msg.code.isNullOrEmpty()) {
                codeContainer?.visibility = View.VISIBLE
                code?.text = msg.code.take(300) + if ((msg.code.length) > 300) "\n..." else ""
                code?.setOnClickListener { onCodeTap?.invoke(msg.code) }
                btnCopyCode?.setOnClickListener { onCopyCode?.invoke(msg.code) }
            } else {
                codeContainer?.visibility = View.GONE
            }
        }
    }

    class ThinkingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Pulse animation handled in code via ObjectAnimator
        fun bind() {
            // Animation started in onBindViewHolder
        }
    }

    class SystemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.chatText)
        fun bind(msg: ChatMessage) { text.text = msg.text }
    }

    class UserInputViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val question: TextView = view.findViewById(R.id.questionText)
        private val sendBtn: View = view.findViewById(R.id.sendAnswer)
        fun bind(msg: ChatMessage, onReply: (String) -> Unit) {
            question.text = msg.text
            sendBtn.setOnClickListener {
                val input = view.findViewById<EditText>(R.id.userInput)
                val reply = input.text.toString()
                if (reply.isNotBlank()) onReply(reply)
            }
        }
    }

    class ErrorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.chatText)
        fun bind(msg: ChatMessage) { text.text = msg.text }
    }
}