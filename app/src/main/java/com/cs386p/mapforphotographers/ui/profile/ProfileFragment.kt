package com.cs386p.mapforphotographers.ui.profile

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.cs386p.mapforphotographers.AuthInit
import com.cs386p.mapforphotographers.databinding.FragmentProfileBinding
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
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
            binding.textWelcome.visibility = View.GONE
            binding.textEditUsername.visibility = View.VISIBLE
        }

        binding.textEditUsername.setOnClickListener {
            if (!binding.textEditUsername.text.isNullOrBlank()) {
                AuthInit.setDisplayName(binding.textEditUsername.text.toString(), viewModel)
                binding.textWelcome.visibility = View.VISIBLE
                binding.textEditUsername.visibility = View.GONE
            } else {
                val toast = Toast.makeText(context, "Please provide a display name!", Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        return root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}