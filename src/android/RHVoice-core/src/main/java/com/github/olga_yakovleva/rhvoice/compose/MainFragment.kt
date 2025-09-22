package com.github.olga_yakovleva.rhvoice.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.github.olga_yakovleva.rhvoice.compose.theme.JetpackComposeMLKitTutorialTheme

interface MainFragmentListener {
    fun onSettingsClick()
}

class MainFragment : Fragment() {
    private var listener: MainFragmentListener? = null
    
    companion object {
        fun newInstance(listener: MainFragmentListener): MainFragment {
            return MainFragment().apply {
                this.listener = listener
            }
        }
    }
    
    fun setListener(listener: MainFragmentListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                JetpackComposeMLKitTutorialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(listener ?: object : MainFragmentListener {
                            override fun onSettingsClick() {
                                // Default implementation - do nothing if no listener is set
                            }
                        })
                    }
                }
            }
        }
    }


}