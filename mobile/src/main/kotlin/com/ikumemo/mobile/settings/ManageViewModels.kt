package com.ikumemo.mobile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikumemo.mobile.data.ProductRepository
import com.ikumemo.mobile.data.StoreRepository
import com.ikumemo.mobile.data.db.ProductEntity
import com.ikumemo.mobile.data.db.StoreEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val STOP_TIMEOUT_MILLIS = 5_000L

sealed interface StoreManageEvent {
    /** 使わないお店にする（候補から消し、過去の記録のお店名は残す、R-16）。 */
    data class Archive(
        val storeId: Long,
    ) : StoreManageEvent

    /** [from] を [into] へまとめる（同じお店が 2 つできたとき、R-16）。 */
    data class Merge(
        val from: Long,
        val into: Long,
    ) : StoreManageEvent
}

/** お店の管理（PLAN §3）。使っているお店を最近使った順に出す。 */
class StoreManageViewModel(
    private val repository: StoreRepository,
) : ViewModel() {
    val stores: StateFlow<List<StoreEntity>> =
        repository
            .observeActiveStores()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val writeLock = Mutex()

    fun onEvent(event: StoreManageEvent) {
        viewModelScope.launch {
            writeLock.withLock {
                when (event) {
                    is StoreManageEvent.Archive -> repository.archive(event.storeId)
                    is StoreManageEvent.Merge -> repository.merge(event.from, event.into)
                }
            }
        }
    }
}

/** 候補に出すかどうかの切り替え（F-23 の非表示）。 */
data class SetProductVisible(
    val productId: Long,
    val visible: Boolean,
)

/** よく買う物の管理（PLAN §3）。よく買う順に並べ、候補（いつもの・入力中の候補）に出すかを切り替える。 */
class ProductManageViewModel(
    private val repository: ProductRepository,
) : ViewModel() {
    val products: StateFlow<List<ProductEntity>> =
        repository
            .observeAllProducts()
            .map { products ->
                products.sortedWith(
                    compareByDescending<ProductEntity> { it.purchaseCount }
                        .thenByDescending { it.lastPurchasedAt }
                        .thenBy { it.normalizedName },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    fun onEvent(event: SetProductVisible) {
        viewModelScope.launch { repository.setHidden(event.productId, hidden = !event.visible) }
    }
}
