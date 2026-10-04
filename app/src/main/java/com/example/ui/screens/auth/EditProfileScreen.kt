package com.example.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.data.models.AlgeriaWilayas
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    user: UserEntity,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var email by remember { mutableStateOf(user.email) }
    var wilaya by remember { mutableStateOf(user.wilaya) }
    var commune by remember { mutableStateOf(user.commune) }
    var bio by remember { mutableStateOf(user.bio) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    var isWilayaExpanded by remember { mutableStateOf(false) }
    var isCommuneExpanded by remember { mutableStateOf(false) }

    val selectedWilayaObj = remember(wilaya) {
        AlgeriaWilayas.list.find {
            "${it.code} - ${it.nameAr}".equals(wilaya.trim(), ignoreCase = true) ||
            it.nameAr.equals(wilaya.trim(), ignoreCase = true) ||
            wilaya.trim().startsWith("${it.code} ")
        }
    }
    val currentCommunes = selectedWilayaObj?.communes ?: emptyList()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("تعديل الملف الشخصي", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("عدّل بياناتك الشخصية ومعلومات التواصل.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 13.sp)
            OutlinedTextField(name, { name = it; error = null }, Modifier.fillMaxWidth(), label = { Text("الاسم الكامل") }, singleLine = true, enabled = !saving)
            OutlinedTextField(phone, { phone = it; error = null }, Modifier.fillMaxWidth(), label = { Text("رقم الهاتف") }, singleLine = true, enabled = !saving)
            OutlinedTextField(email, { email = it; error = null }, Modifier.fillMaxWidth(), label = { Text("البريد الإلكتروني") }, singleLine = true, enabled = !saving)

            // Wilaya: Read-only dropdown list of 69 wilayas
            ExposedDropdownMenuBox(
                expanded = isWilayaExpanded,
                onExpandedChange = { if (!saving) isWilayaExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = wilaya,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text("الولاية (اختر من القائمة - 69 ولاية)") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isWilayaExpanded) },
                    singleLine = true,
                    enabled = !saving
                )
                ExposedDropdownMenu(
                    expanded = isWilayaExpanded,
                    onDismissRequest = { isWilayaExpanded = false },
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    AlgeriaWilayas.list.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${item.code} - ${item.nameAr}", fontWeight = FontWeight.SemiBold)
                                    Text(item.nameFr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                wilaya = "${item.code} - ${item.nameAr}"
                                if (item.communes.isNotEmpty()) {
                                    commune = item.communes.first()
                                }
                                isWilayaExpanded = false
                            }
                        )
                    }
                }
            }

            // Commune: Read-only dropdown list
            if (currentCommunes.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = isCommuneExpanded,
                    onExpandedChange = { if (!saving) isCommuneExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = commune,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("البلدية (اختر من القائمة)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCommuneExpanded) },
                        singleLine = true,
                        enabled = !saving
                    )
                    ExposedDropdownMenu(
                        expanded = isCommuneExpanded,
                        onDismissRequest = { isCommuneExpanded = false },
                        modifier = Modifier.heightIn(max = 220.dp)
                    ) {
                        currentCommunes.forEach { commName ->
                            DropdownMenuItem(
                                text = { Text(commName) },
                                onClick = {
                                    commune = commName
                                    isCommuneExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(commune, { commune = it; error = null }, Modifier.fillMaxWidth(), label = { Text("البلدية") }, singleLine = true, enabled = !saving)
            }

            OutlinedTextField(bio, { bio = it; error = null }, Modifier.fillMaxWidth(), label = { Text("نبذة عنك") }, minLines = 3, enabled = !saving)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center) }
            Button(
                onClick = {
                    saving = true
                    viewModel.updateCurrentUserProfile(name, phone, email, wilaya, commune, bio,
                        onSuccess = { saving = false; onSaved() },
                        onError = { saving = false; error = it })
                },
                modifier = Modifier.fillMaxWidth(), enabled = !saving
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.padding(2.dp), color = MaterialTheme.colorScheme.onPrimary)
                else { Icon(Icons.Default.Save, null); Text(" حفظ التعديلات") }
            }
        }
    }
}
