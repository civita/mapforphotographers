package com.cs386p.mapforphotographers.ui.profile

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.cs386p.mapforphotographers.AuthInit
import com.cs386p.mapforphotographers.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.updateUser()
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            Log.d("MainActivity", "sign in failed ${result}")
        }
    }

    // An Android nightmare
    // https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    // https://stackoverflow.com/questions/7789514/how-to-get-activitys-windowtoken-without-view
    open fun hideKeyboard() {
        if (activity != null){
            // Hide soft keyboard
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textProfile
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        binding.buttonLogin.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if(user == null) {
                AuthInit(viewModel, signInLauncher)
            } else {
                viewModel.signOut()
                binding.textWelcome.visibility = View.VISIBLE
                binding.textEditUsername.visibility = View.GONE
            }
        }

        viewModel.observeLoginButtonText().observe(viewLifecycleOwner) {
            binding.buttonLogin.text = it
        }

        viewModel.observeDisplayName().observe(viewLifecycleOwner) {
            binding.textWelcome.text = "Hi, " + it
            binding.textEditUsername.setText(it)
        }

        binding.textWelcome.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if(user != null) {
                binding.textWelcome.visibility = View.GONE
                binding.textEditUsername.visibility = View.VISIBLE
            }
        }

        binding.textEditUsername.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if(user != null) {
                if (!binding.textEditUsername.text.isNullOrBlank()) {
                    AuthInit.setDisplayName(binding.textEditUsername.text.toString(), viewModel)
                    binding.textWelcome.visibility = View.VISIBLE
                    binding.textEditUsername.visibility = View.GONE
                    hideKeyboard()
                } else {
                    val toast = Toast.makeText(context, "Please provide a display name!", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }

        return root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}