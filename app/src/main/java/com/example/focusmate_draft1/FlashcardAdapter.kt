package com.example.focusmate_draft1

import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class FlashcardAdapter(
    private var flashcards: List<Flashcard>,
    private val onAction: (Flashcard, String) -> Unit
) : RecyclerView.Adapter<FlashcardAdapter.FlashcardViewHolder>() {

    inner class FlashcardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val questionText: TextView = view.findViewById(R.id.textQuestion)
        val answerText: TextView = view.findViewById(R.id.textAnswer)
        val cardView: CardView = view as CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlashcardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flashcard, parent, false)
        return FlashcardViewHolder(view)
    }

    override fun onBindViewHolder(holder: FlashcardViewHolder, position: Int) {
        val flashcard = flashcards[position]
        holder.questionText.text = flashcard.question
        holder.answerText.text = "Tap to Reveal"
        holder.answerText.visibility = View.GONE

        // Toggle answer visibility on click
        holder.cardView.setOnClickListener {
            if (holder.answerText.visibility == View.VISIBLE) {
                holder.answerText.visibility = View.GONE
            } else {
                holder.answerText.text = flashcard.answer
                holder.answerText.visibility = View.VISIBLE
            }
        }

        // Show popup menu on long press
        holder.cardView.setOnLongClickListener { view ->
            showPopupMenu(view, flashcard)
            true
        }
    }

    override fun getItemCount(): Int = flashcards.size

    fun updateData(newFlashcards: List<Flashcard>) {
        flashcards = newFlashcards
        notifyDataSetChanged()
    }

    // Function to show popup menu
    private fun showPopupMenu(view: View, flashcard: Flashcard) {
        val popup = PopupMenu(view.context, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.flashcard_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_edit -> {
                    onAction(flashcard, "edit")
                    true
                }
                R.id.action_delete -> {
                    onAction(flashcard, "delete")
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
