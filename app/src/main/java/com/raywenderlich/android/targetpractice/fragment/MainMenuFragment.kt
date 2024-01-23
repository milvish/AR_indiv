package com.raywenderlich.android.targetpractice.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.raywenderlich.android.targetpractice.R

class MainMenuFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_main_menu, container, false)
        val buttonStartLesson = view.findViewById<Button>(R.id.buttonStartLesson)
        buttonStartLesson.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_lessonFragment)
        }
        // Inflate the layout for this fragment
        return view
    }


}