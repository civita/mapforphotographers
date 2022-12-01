package com.cs386p.mapforphotographers.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.cs386p.mapforphotographers.PhotoViewModel
import com.cs386p.mapforphotographers.databinding.FragmentDashboardBinding
import com.cs386p.mapforphotographers.view.PhotoMetaAdapter
import com.google.android.material.internal.ViewUtils.hideKeyboard

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModelPhoto: PhotoViewModel by activityViewModels()


    // An Android nightmare
    // https://stackoverflow.com/a/70562398
    // https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    // https://stackoverflow.com/questions/7789514/how-to-get-activitys-windowtoken-without-view
    private fun hideKeyboard() {
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
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

//        val textView: TextView = binding.textDashboard
//        dashboardViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        val adapter = PhotoMetaAdapter(viewModelPhoto)
        val rv = binding.dashboardPhotosRV
        //val itemDecor = DividerItemDecoration(rv.context, StaggeredGridLayoutManager.VERTICAL)
        //rv.addItemDecoration(itemDecor)
        rv.adapter = adapter
        rv.layoutManager = GridLayoutManager(rv.context, 3)
        viewModelPhoto.observeLivePhotoMeta().observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        binding.actionSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    hideKeyboard()
                    viewModelPhoto.setSearchTerm("""""")
                } else {
                    viewModelPhoto.setSearchTerm(s.toString())
                }
            }
        })
        viewModelPhoto.searchTermRefresh()
        return root
    }

    override fun onResume() {
        viewModelPhoto.searchTermRefresh()
        super.onResume()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}