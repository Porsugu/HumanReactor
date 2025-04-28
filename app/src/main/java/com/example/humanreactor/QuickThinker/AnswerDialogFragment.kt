package com.example.humanreactor.QuickThinker

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.example.humanreactor.R
import org.w3c.dom.Text

class AnswerDialogFragment : DialogFragment() {

    private var onNextQuestionListener : OnNextQuestionListener?= null

    // Interface for callback to activity
    interface OnNextQuestionListener {
        fun onNextQuestion()
    }

    override fun onAttach(context : Context){
        super.onAttach(context)
            // verify the host activity implements the callback interface
        try{
            onNextQuestionListener = context as OnNextQuestionListener
        } catch (e : ClassCastException){
            throw ClassCastException("$context must implement OnNextQuestionListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.quick_thinker_answer_dialog_fragment, container, false)

        // initialize the things that i need here that are from new instance
        val user_choice = arguments?.getInt(ARG_USER_CHOICE)
        val correct_choice = arguments?.getInt(ARG_CORRECT_CHOICE)
        val time_used = arguments?.getDouble(ARG_TIME_USED)
        val explanation = arguments?.getString(ARG_EXPLANATION)
        val humour = arguments?.getString(ARG_HUMOUR)

        // check if the answer is corect or not
        if(user_choice != correct_choice){
            view.findViewById<TextView>(R.id.dialog_topic).text = "Oppsie Wrong"
        }
        view.findViewById<TextView>(R.id.sentence_of_humour).text = humour
        view.findViewById<TextView>(R.id.dialog_correct_answer).text = correct_choice.toString()
        view.findViewById<TextView>(R.id.quick_thinker_explanation).text = explanation
        view.findViewById<TextView>(R.id.dialog_time_used).text = String.format("%.2f sec", time_used)
        val dialog_nextButton = view.findViewById<ConstraintLayout>(R.id.dialog_next_BTN)

        // setting next button onClickListener
        dialog_nextButton.setOnClickListener {

            Log.d("Dialog", "Next question is clicked")
            dismiss()
            onNextQuestionListener?.onNextQuestion()    // call the callback method
        }


        return view
    }

//    override fun onCreate (savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//    }

    override fun onResume() {
        super.onResume()

        //set the size of the dialog
        dialog?.window?.setLayout(
            resources.getDimensionPixelSize(R.dimen.card_width),
            resources.getDimensionPixelSize(R.dimen.card_hight),

            )
    }

    // companion objects to get the stuffs from the activity which called it out
    companion object {
        private const val ARG_USER_CHOICE = "userChoice"
        private const val ARG_CORRECT_CHOICE = "correctChoice"
        private const val ARG_TIME_USED = "timeUsed"
        private const val ARG_EXPLANATION = "explanation"
        private const val ARG_HUMOUR = "sentence_of_humour"

        fun newInstance(userChoice : Int, correctChoice : Int, timeUsedSeconds:Double, explanation:String,sentence_of_humour:String ): AnswerDialogFragment {
            val fragment = AnswerDialogFragment()
            val args = Bundle().apply{
                putInt(ARG_USER_CHOICE, userChoice)
                putInt(ARG_CORRECT_CHOICE, correctChoice)
                putDouble(ARG_TIME_USED, timeUsedSeconds)
                putString(ARG_EXPLANATION, explanation)
                putString(ARG_HUMOUR, sentence_of_humour)
            }

            fragment.arguments = args
            return fragment

        }

    }
}