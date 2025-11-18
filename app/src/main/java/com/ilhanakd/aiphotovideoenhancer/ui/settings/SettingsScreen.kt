package com.ilhanakd.aiphotovideoenhancer.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ilhanakd.aiphotovideoenhancer.R
import com.ilhanakd.aiphotovideoenhancer.data.preferences.PreferenceRepository
import com.ilhanakd.aiphotovideoenhancer.domain.model.AppThemeOption
import com.ilhanakd.aiphotovideoenhancer.domain.model.LocaleOption

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context.applicationContext))
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val showAbout = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(text = stringResource(id = R.string.language))
            LanguageOption(title = stringResource(id = R.string.system_default), selected = language == LocaleOption.SYSTEM) {
                viewModel.setLanguage(LocaleOption.SYSTEM)
            }
            LanguageOption(title = stringResource(id = R.string.english), selected = language == LocaleOption.ENGLISH) {
                viewModel.setLanguage(LocaleOption.ENGLISH)
            }
            LanguageOption(title = stringResource(id = R.string.turkish), selected = language == LocaleOption.TURKISH) {
                viewModel.setLanguage(LocaleOption.TURKISH)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = stringResource(id = R.string.theme))
            ThemeOption(title = stringResource(id = R.string.system_default), selected = theme == AppThemeOption.SYSTEM) {
                viewModel.setTheme(AppThemeOption.SYSTEM)
            }
            ThemeOption(title = stringResource(id = R.string.light), selected = theme == AppThemeOption.LIGHT) {
                viewModel.setTheme(AppThemeOption.LIGHT)
            }
            ThemeOption(title = stringResource(id = R.string.dark), selected = theme == AppThemeOption.DARK) {
                viewModel.setTheme(AppThemeOption.DARK)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showAbout.value = true }) {
                Text(text = stringResource(id = R.string.about))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(id = R.string.privacy_policy_body))
        }
    }

    if (showAbout.value) {
        AlertDialog(
            onDismissRequest = { showAbout.value = false },
            confirmButton = { Button(onClick = { showAbout.value = false }) { Text(text = stringResource(id = R.string.ok)) } },
            title = { Text(text = stringResource(id = R.string.about)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.about_body
                    )
                )
            }
        )
    }
}

@Composable
private fun LanguageOption(title: String, selected: Boolean, onSelect: () -> Unit) {
    androidx.compose.material3.Row(modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = title, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ThemeOption(title: String, selected: Boolean, onSelect: () -> Unit) {
    androidx.compose.material3.Row(modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = title, modifier = Modifier.padding(start = 8.dp))
    }
}

class SettingsViewModelFactory(private val context: android.content.Context) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        val repository = PreferenceRepository(context)
        return SettingsViewModel(repository) as T
    }
}
