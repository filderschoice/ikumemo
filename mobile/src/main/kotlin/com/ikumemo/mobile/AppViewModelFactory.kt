package com.ikumemo.mobile

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ikumemo.core.suggest.Suggestions
import com.ikumemo.mobile.calendar.CalendarViewModel
import com.ikumemo.mobile.reminder.ReminderViewModel
import com.ikumemo.mobile.settings.AppearanceViewModel
import com.ikumemo.mobile.settings.OperationViewModel
import com.ikumemo.mobile.settings.ProductManageViewModel
import com.ikumemo.mobile.settings.ReminderSettingsViewModel
import com.ikumemo.mobile.settings.StoreManageViewModel
import com.ikumemo.mobile.shoppinglist.ShoppingListViewModel
import com.ikumemo.mobile.voice.VoiceInputViewModel

/** 画面の ViewModel を [AppContainer] から作る（手動 DI、PLAN §4.1）。 */
object AppViewModelFactory {
    val Factory: ViewModelProvider.Factory =
        viewModelFactory {
            initializer {
                val container = container()
                ShoppingListViewModel(
                    container.shoppingRepository,
                    container.storeRepository,
                    Suggestions(container.normalizer),
                    container.operationPrefs.settings,
                )
            }
            initializer {
                val container = container()
                CalendarViewModel(container.recordRepository, container.storeRepository, container.clock)
            }
            initializer {
                val container = container()
                ReminderViewModel(container.reminderRepository, container.reminderPrefs, container.clock)
            }
            initializer { AppearanceViewModel(container().appearancePrefs) }
            initializer { OperationViewModel(container().operationPrefs) }
            initializer { StoreManageViewModel(container().storeRepository) }
            initializer { ProductManageViewModel(container().shoppingRepository.products) }
            initializer {
                val container = container()
                ReminderSettingsViewModel(container.reminderPrefs) { container.reminderScheduler.reschedule() }
            }
            initializer { VoiceInputViewModel(container().shoppingRepository, container().speechEngineFactory()) }
        }

    private fun CreationExtras.container(): AppContainer =
        (requireNotNull(this[APPLICATION_KEY]) as IkumemoApplication).container
}
