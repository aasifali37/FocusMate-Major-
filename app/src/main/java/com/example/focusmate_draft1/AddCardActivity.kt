package com.example.focusmate_draft1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddCardActivity : AppCompatActivity() {

    private lateinit var editQuestion: EditText
    private lateinit var editAnswer: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var database: FlashcardDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_card)

        editQuestion = findViewById(R.id.editQuestion)
        editAnswer = findViewById(R.id.editAnswer)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        database = FlashcardDatabase(this)

        // Check if editing an existing card
        val existingQuestion = intent.getStringExtra("question")
        val existingAnswer = intent.getStringExtra("answer")

        if (existingQuestion != null && existingAnswer != null) {
            editQuestion.setText(existingQuestion)
            editAnswer.setText(existingAnswer)
        }

        btnSave.setOnClickListener {
            saveFlashcard()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveFlashcard() {
        val question = editQuestion.text.toString().trim()
        val answer = editAnswer.text.toString().trim()

        if (question.isEmpty() || answer.isEmpty()) {
            Toast.makeText(this, "Both fields are required!", Toast.LENGTH_SHORT).show()
            return
        }

        if (intent.hasExtra("question")) {
            database.updateCard(Flashcard(question, answer))
            Toast.makeText(this, "Flashcard updated!", Toast.LENGTH_SHORT).show()
        } else {
            database.addCard(Flashcard(question, answer))
            Toast.makeText(this, "Flashcard added!", Toast.LENGTH_SHORT).show()
        }

        finish() // Close the activity
    }
}
