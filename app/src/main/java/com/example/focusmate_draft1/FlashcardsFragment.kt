package com.example.focusmate_draft1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FlashcardsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var flashcardAdapter: FlashcardAdapter
    private lateinit var database: FlashcardDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_flashcards, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        val fabAddCard = view.findViewById<FloatingActionButton>(R.id.fab_add_card)

        database = FlashcardDatabase(requireContext())

        setupRecyclerView()
        loadFlashcards()

        fabAddCard.setOnClickListener {
            openAddCardActivity()
        }

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        flashcardAdapter = FlashcardAdapter(emptyList()) { flashcard, action ->
            handleCardAction(flashcard, action)
        }
        recyclerView.adapter = flashcardAdapter
    }

    private fun loadFlashcards() {
        val flashcards = database.getAllCards()
        flashcardAdapter.updateData(flashcards)
    }

    private fun handleCardAction(flashcard: Flashcard, action: String) {
        when (action) {
            "delete" -> showDeleteConfirmationDialog(flashcard)
            "edit" -> openEditCardActivity(flashcard)
        }
    }

    private fun showDeleteConfirmationDialog(flashcard: Flashcard) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Flashcard")
            .setMessage("Are you sure you want to delete this flashcard?")
            .setPositiveButton("Delete") { _, _ ->
                database.deleteCard(flashcard.question)
                loadFlashcards()
                Toast.makeText(requireContext(), "Flashcard deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAddCardActivity() {
        val intent = android.content.Intent(requireContext(), AddCardActivity::class.java)
        startActivity(intent)
    }

    private fun openEditCardActivity(flashcard: Flashcard) {
        val intent = android.content.Intent(requireContext(), AddCardActivity::class.java)
        intent.putExtra("question", flashcard.question)
        intent.putExtra("answer", flashcard.answer)
        startActivity(intent)
    }
}
