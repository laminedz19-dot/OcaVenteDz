package com.example.ui.screens.categories

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.CategoryGridCard
import com.example.ui.components.OcaSubTopBar
import com.example.ui.model.Category

@Composable
fun CategoriesScreen(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            OcaSubTopBar(
                title = "جميع التصنيفات",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryGridCard(
                    category = category,
                    onClick = { onCategoryClick(category) }
                )
            }
        }
    }
}
