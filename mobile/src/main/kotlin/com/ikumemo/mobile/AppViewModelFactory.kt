package com.ikumemo.mobile

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ikumemo.mobile.shoppinglist.ShoppingListViewModel
import com.ikumemo.mobile.voice.VoiceInputViewModel

/** 画面の ViewModel を [AppContainer] から作る（手動 DI、PLAN §4.1）。 */
object AppViewModelFactory {
    val Factory: ViewModelProvider.Factory =
        viewModelFactory {
            initializer { ShoppingListViewModel(container().shoppingRepository) }
            initializer { VoiceInputViewModel(container().shoppingRepository, container().speechEngineFactory()) }
        }

    private fun CreationExtras.container(): AppContainer =
        (requireNotNull(this[APPLICATION_KEY]) as IkumemoApplication).container
}
