package com.cs386p.mapforphotographers.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
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
    private val viewModel: ProfileViewModel by viewModels()
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
    open fun hideKeyboard() {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textProfile
//        notificationsViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        val adapter = PhotoMetaAdapter(viewModelPhoto)
        val rv = binding.photosRV
        //val itemDecor = DividerItemDecoration(rv.context, StaggeredGridLayoutManager.VERTICAL)
        //rv.addItemDecoration(itemDecor)
        rv.adapter = adapter
        rv.layoutManager = GridLayoutManager(rv.context, 3)
//        // Swipe left to delete
//        initTouchHelper().attachToRecyclerView(rv)
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

        viewModelPhoto.observerPhotoCount().observe(viewLifecycleOwner) {
            binding.textPhotosCount.text = it.toString()
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
                    val snack = Snackbar.make(it,"Please provide a display name!",Snackbar.LENGTH_LONG)
                    snack.show()
//                    val toast = Toast.makeText(context, "Please provide a display name!", Toast.LENGTH_SHORT)
//                    toast.show()
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
            }
        }

        viewModel.updateUser()
        viewModelPhoto.fetchPhotoMeta()
//        updatePhotoCount()
        return root
    }
    private val importPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            //result.data?.toString()?.let { Log.d("xxx", it) }
            if (result.data != null && result.data?.data != null) {
                doOnePhoto(binding.root.context, result.data!!.data!!)
            }
            //result.data...
            //viewModel.pictureSuccess()
        } else {
            //viewModel.pictureFailure()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}