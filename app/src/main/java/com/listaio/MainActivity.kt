package com.listaio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.listaio.di.ServiceLocator
import com.listaio.ui.ListaioApp
import com.listaio.ui.list.ShoppingListViewModel
import com.listaio.ui.list.ShoppingListViewModelFactory
import com.listaio.ui.onboarding.OnboardingScreen
import com.listaio.ui.theme.ListaioTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("listaio_prefs", MODE_PRIVATE)

        setContent {
            ListaioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var listId by rememberSaveable { mutableStateOf(prefs.getString("listId", null)) }

                    if (listId == null) {
                        OnboardingScreen(
                            onListIdSet = { id ->
                                prefs.edit().putString("listId", id).apply()
                                listId = id
                            },
                        )
                    } else {
                        val viewModel = viewModel<ShoppingListViewModel>(
                            factory = ShoppingListViewModelFactory(
                                ServiceLocator.provideListRepository(listId!!),
                            ),
                        )
                        ListaioApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
