package com.ikumemo.mobile.data

import com.ikumemo.core.suggest.ProductSummary
import com.ikumemo.core.text.ProductNameNormalizer
import com.ikumemo.mobile.data.db.IkumemoDatabase
import com.ikumemo.mobile.data.db.ProductEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

/** 品目（候補の元）の読み書き。 */
class ProductRepository(
    db: IkumemoDatabase,
    private val normalizer: ProductNameNormalizer,
    private val clock: Clock,
) {
    private val productDao = db.productDao()

    suspend fun findByName(name: String): ProductEntity? = productDao.findByNormalizedName(normalizer.normalize(name))

    /** 正規化した品名が同じ品目を返し、無ければ作る。初めて入れた表記をその品物の代表名にする（PLAN §4.4）。 */
    suspend fun findOrCreate(displayName: String): ProductEntity {
        val normalized = normalizer.normalize(displayName)
        productDao.findByNormalizedName(normalized)?.let { return it }
        val product = ProductEntity(name = displayName, normalizedName = normalized, createdAt = clock.instant())
        return product.copy(id = productDao.insert(product))
    }

    /** 品目の表示名（音声の分割で品名の中の「と」を守るのに使う）。 */
    suspend fun names(): List<String> = productDao.getAllNames()

    /** よく買う物の管理で並べる全部の品目（候補に出さない物も含む）。 */
    fun observeAllProducts(): Flow<List<ProductEntity>> = productDao.observeAll()

    /** 候補に出さない／出す（F-23）。記録やメモには影響しない。 */
    suspend fun setHidden(
        productId: Long,
        hidden: Boolean,
    ) {
        productDao.getById(productId)?.let { productDao.update(it.copy(hidden = hidden)) }
    }

    /** 候補の元になる品目（F-20・F-21）。 */
    fun observeSummaries(): Flow<List<ProductSummary>> =
        productDao.observeAll().map { products ->
            products.map {
                ProductSummary(it.id, it.name, it.normalizedName, it.purchaseCount, it.lastPurchasedAt, it.hidden)
            }
        }
}
