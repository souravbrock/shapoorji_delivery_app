package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Order
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.auth.AdminAuthGuard
import com.example.ui.auth.WelcomeScreen
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.GoogleAccountDialog
import com.example.ui.customer.CartScreen
import com.example.ui.customer.CheckoutScreen
import com.example.ui.customer.CustomerCatalogScreen
import com.example.ui.customer.FavoritesScreen
import com.example.ui.customer.InvoiceScreen
import com.example.ui.customer.OrderTrackingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GroceryViewModel

sealed interface AppScreen {
    data object Catalog : AppScreen
    data object Cart : AppScreen
    data object Checkout : AppScreen
    data class OrderTracking(val orderId: Long? = null) : AppScreen
    data class Invoice(val order: Order) : AppScreen
    data object Favorites : AppScreen
    data object Admin : AppScreen
}

class MainActivity : ComponentActivity() {

    private val viewModel: GroceryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShapoorjiDeliveryApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ShapoorjiDeliveryApp(viewModel: GroceryViewModel) {
    val context = LocalContext.current
    var backstack by remember { mutableStateOf<List<AppScreen>>(listOf(AppScreen.Catalog)) }
    val currentScreen = backstack.lastOrNull() ?: AppScreen.Catalog

    val user by viewModel.currentUser.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val websiteAuthState by viewModel.websiteAuthState.collectAsState()
    val emailOtpState by viewModel.emailOtpState.collectAsState()

    var showGoogleAccountDialog by remember { mutableStateOf(false) }

    // Toast listener
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.toastMessage.value = null
        }
    }

    // Unauthenticated State Gate: If customer is not logged in, render Welcome Screen
    if (!user.isGoogleSignedIn) {
        WelcomeScreen(
            onGoogleSignIn = { name, email, phone, tower, flat ->
                viewModel.registerOrUpdateCustomer(name, email, phone, tower, flat)
                backstack = listOf(AppScreen.Catalog)
            },
            websiteAuthState = websiteAuthState,
            emailOtpState = emailOtpState,
            onWebsiteLogin = { email, password -> viewModel.websiteLogin(email, password) },
            onRequestSignupOtp = { email -> viewModel.requestSignupOtp(email) },
            onConfirmSignupOtp = { name, email, password, phone, tower, flat, code ->
                viewModel.selectedTower.value = tower
                viewModel.flatInput.value = flat
                viewModel.confirmSignupOtp(name, email, password, phone, code)
            }
        )
        return
    }

    // Sync admin mode toggle with screen
    LaunchedEffect(isAdminMode) {
        if (isAdminMode && currentScreen !is AppScreen.Admin) {
            backstack = backstack + AppScreen.Admin
        } else if (!isAdminMode && currentScreen is AppScreen.Admin) {
            if (backstack.size > 1) {
                backstack = backstack.filterNot { it is AppScreen.Admin }
            } else {
                backstack = listOf(AppScreen.Catalog)
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        backstack = backstack + screen
    }

    fun navigateBack() {
        if (backstack.size > 1) {
            backstack = backstack.dropLast(1)
        }
    }

    BackHandler(enabled = backstack.size > 1) {
        navigateBack()
    }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            is AppScreen.Catalog -> {
                CustomerCatalogScreen(
                    viewModel = viewModel,
                    onNavigateToCart = { navigateTo(AppScreen.Cart) },
                    onNavigateToOrders = { navigateTo(AppScreen.OrderTracking()) },
                    onNavigateToFavorites = { navigateTo(AppScreen.Favorites) },
                    onOpenGoogleAccountDialog = { showGoogleAccountDialog = true }
                )
            }
            is AppScreen.Cart -> {
                CartScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigateBack() },
                    onProceedToCheckout = { navigateTo(AppScreen.Checkout) }
                )
            }
            is AppScreen.Checkout -> {
                CheckoutScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigateBack() },
                    onOrderPlaced = { order ->
                        // Replace checkout with tracking screen
                        backstack = listOf(AppScreen.Catalog, AppScreen.OrderTracking(order.id))
                    }
                )
            }
            is AppScreen.OrderTracking -> {
                OrderTrackingScreen(
                    viewModel = viewModel,
                    initialOrderId = screen.orderId,
                    onNavigateBack = {
                        if (backstack.size > 1) navigateBack() else backstack = listOf(AppScreen.Catalog)
                    },
                    onViewInvoice = { order ->
                        navigateTo(AppScreen.Invoice(order))
                    }
                )
            }
            is AppScreen.Invoice -> {
                InvoiceScreen(
                    order = screen.order,
                    onNavigateBack = { navigateBack() }
                )
            }
            is AppScreen.Favorites -> {
                FavoritesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigateBack() }
                )
            }
            is AppScreen.Admin -> {
                // Authentication Logic Guard: strictly verifies if current user is souravbrock@gmail.com
                // before rendering the AdminDashboardScreen component
                AdminAuthGuard(
                    currentUser = user,
                    onNavigateBackToCustomer = {
                        viewModel.setAdminMode(false)
                        navigateBack()
                    }
                ) {
                    AdminDashboardScreen(
                        viewModel = viewModel,
                        onNavigateBackToCustomer = {
                            viewModel.setAdminMode(false)
                            navigateBack()
                        },
                        onViewOrderInvoice = { order ->
                            navigateTo(AppScreen.Invoice(order))
                        }
                    )
                }
            }
        }
    }

    // Google Sign-in / Registration Modal
    if (showGoogleAccountDialog) {
        GoogleAccountDialog(
            currentUser = user,
            onDismiss = { showGoogleAccountDialog = false },
            onSaveProfile = { name, email, phone, tower, flat ->
                viewModel.registerOrUpdateCustomer(name, email, phone, tower, flat)
            },
            onSignOut = {
                viewModel.signOut()
                backstack = listOf(AppScreen.Catalog)
                showGoogleAccountDialog = false
            },
            onCheckForUpdates = {
                showGoogleAccountDialog = false
                viewModel.checkForAppUpdates()
            },
            websiteAuthState = websiteAuthState,
            onWebsiteLogin = { email, password -> viewModel.websiteLogin(email, password) },
            emailOtpState = emailOtpState,
            onRequestSignupOtp = { email -> viewModel.requestSignupOtp(email) },
            onConfirmSignupOtp = { name, email, password, phone, code ->
                viewModel.confirmSignupOtp(name, email, password, phone, code)
            },
            onWebsiteLogout = { viewModel.websiteLogout() }
        )
    }

    // App In-Place Update & Upgrade Dialog
    if (showUpdateDialog) {
        AppUpdateDialog(
            status = updateStatus,
            onDismiss = { viewModel.dismissUpdateDialog() },
            onCheckForUpdates = { viewModel.checkForAppUpdates() },
            onInstallUpdate = {
                Toast.makeText(context, "Initiating in-place upgrade package installer...", Toast.LENGTH_SHORT).show()
            },
            onRequestInstallPermission = {
                viewModel.appUpdateManager.openInstallPermissionSettings(context)
            },
            canInstallPackages = viewModel.appUpdateManager.canRequestPackageInstalls()
        )
    }
}
