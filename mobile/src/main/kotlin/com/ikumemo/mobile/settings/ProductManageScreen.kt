package com.ikumemo.mobile.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ikumemo.mobile.AppViewModelFactory
import com.ikumemo.mobile.R
import com.ikumemo.mobile.data.db.ProductEntity

/** よく買う物の管理（PLAN §3）。品目ごとに「候補に出す」を切り替える（F-23）。 */
@Composable
fun ProductManageScreen(
    onBack: () -> Unit,
    viewModel: ProductManageViewModel = viewModel(factory = AppViewModelFactory.Factory),
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    SettingsScaffold(title = R.string.settings_products, onBack = onBack) {
        ProductManageContent(products, viewModel::onEvent)
    }
}

@Composable
fun ProductManageContent(
    products: List<ProductEntity>,
    onEvent: (SetProductVisible) -> Unit,
) {
    if (products.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.products_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                stringResource(R.string.products_detail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        items(products, key = { it.id }) { product ->
            val visible = !product.hidden
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(value = visible, role = Role.Switch) {
                            onEvent(SetProductVisible(product.id, it))
                        }.padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag(ManageTestTags.product(product.id)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(product.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.products_count, product.purchaseCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = visible, onCheckedChange = null)
            }
            HorizontalDivider()
        }
    }
}
