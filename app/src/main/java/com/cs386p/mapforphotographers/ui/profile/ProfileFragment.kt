package com.cs386p.mapforphotographers.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.cs386p.mapforphotographers.AuthInit
import com.cs386p.mapforphotographers.PhotoViewModel
import com.cs386p.mapforphotographers.PhotoViewModel.Companion.doOnePhoto
import com.cs386p.mapforphotographers.databinding.FragmentProfileBinding
import com.cs386p.mapforphotographers.view.PhotoMetaAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import java.io.File

class ProfileFragment : Fragment() {
    companion object {
        private val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        fun localPhotoFile(pictureName : String): File {
            // Create the File where the photo should go
            val localPhotoFile = File(storageDir, "${pictureName}.jpg")
            Log.d("MainActivity", "photo path ${localPhotoFile.absolutePath}")
            return localPhotoFile
        }
    }
    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by activityViewModels()
    private val viewModelPhoto: PhotoViewModel by viewModels()

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
    // https://stackoverflow.com/a/70562398
    // https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    // https://stackoverflow.com/questions/7789514/how-to-get-activitys-windowtoken-without-view
    fun hideKeyboard() {
        if (activity != null){
            // Hide soft keyboard
            val imm =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
        }
    }

    private fun updatePhotoCount() {
        val user = FirebaseAuth.getInstance().currentUser
        if(user != null) {
            viewModelPhoto.fetchPhotoCount(user.uid)
        }
    }

    private fun updatePhotoLikedCount() {
        val user = FirebaseAuth.getInstance().currentUser
        if(user != null) {
            viewModelPhoto.fetchPhotoLikedCount(user.uid)
        }
    }

    override fun onResume() {
        Log.d("xxx", "resume!!")
        viewModelPhoto.fetchPhotoMeta()
        updatePhotoLikedCount()
        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val adapter = PhotoMetaAdapter(viewModelPhoto)
        val rv = binding.photosRV
        rv.adapter = adapter
        rv.layoutManager = GridLayoutManager(rv.context, 3)
        viewModelPhoto.observePhotoMeta().observe(viewLifecycleOwner) {
            adapter.submitList(it)
            updatePhotoCount()
        }

        binding.buttonLogin.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if(user == null) {
                AuthInit(viewModel, signInLauncher)
            } else {
                viewModel.signOut()
                viewModelPhoto.signOut()
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

        viewModelPhoto.observePhotoCount().observe(viewLifecycleOwner) {
            binding.textPhotosCount.text = it.toString()
        }

        viewModelPhoto.observerPhotoLikedCount().observe(viewLifecycleOwner) {
            binding.textPhotosLiked.text = it.toString()
        }

        viewModelPhoto.observeIsViewingLiked().observe(viewLifecycleOwner) {
            if(it) {
                val snack = Snackbar.make(binding.root,"You're viewing photos you liked ❤", Snackbar.LENGTH_SHORT)
                snack.show()
                viewModelPhoto.fetchPhotoMeta()
                binding.layoutLiked.setBackgroundColor(Color.parseColor("#ebb6b0"))
            } else {
                viewModelPhoto.fetchPhotoMeta()
                binding.layoutLiked.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        binding.layoutLiked.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if(user != null) {
                viewModelPhoto.toggleIsViewingLiked()
            }
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
                    val snack = Snackbar.make(it,"Please provide a display name!",Snackbar.LENGTH_SHORT)
                    snack.show()
                }
            }
        }

        binding.buttonUpload.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            if(user != null) {
                // upload a photo
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI).also {
                    importPhotoLauncher.launch(it)
                }
            } else {
                val snack = Snackbar.make(it,"Please login first!", Snackbar.LENGTH_SHORT)
                snack.show()
            }
        }

        viewModel.updateUser()
        viewModelPhoto.fetchPhotoMeta()
        return root
    }
    private val importPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data != null && result.data?.data != null) {
                doOnePhoto(binding.root.context, result.data!!.data!!)
                viewModelPhoto.updateIsViewingLiked(false)
            }
        } else {
            val snack = Snackbar.make(binding.root,"Error picking up a photo!", Snackbar.LENGTH_SHORT)
            snack.show()
            // Do nothing
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}