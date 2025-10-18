package com.example.focusmate_draft1.ui // Correct package name is crucial!

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.focusmate_draft1.R

class ProfileFragment : Fragment() {

    private lateinit var userNameTextView: TextView
    private lateinit var userDescriptionTextView: TextView
    private lateinit var profilePictureImageView: ImageView
    private lateinit var editProfileButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        return view
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userNameTextView = view.findViewById(R.id.userNameTextView)
        userDescriptionTextView = view.findViewById(R.id.userDescriptionTextView)
        profilePictureImageView = view.findViewById(R.id.profilePictureImageView)
        editProfileButton = view.findViewById(R.id.editProfileButton)

        userNameTextView.text = "Asif Ali"
        userDescriptionTextView.text = "A self-driven BCA student from GGSIPU with a strong command of C, C++, Java, and Python, I possess a solid foundation in OOP, SQL, DBMS, Computer Networks, and AI/ML. My passion for Information Security and Software Development is complemented by hands-on experience in Python-based CLI and GUI tools, and Java applications, including recent AI/ML experience at Metafiser."
        profilePictureImageView.setImageResource(R.drawable.ic_profile)

        editProfileButton.setOnClickListener {
            // Handle edit profile button click
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // No need to nullify views individually here.
    }
}
