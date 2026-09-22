package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.local.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.ShapoorjiGeo
import com.example.data.model.UserProfile
import com.example.data.notification.NotificationDispatcher
import com.example.data.notification.SmtpResult
import com.example.data.notification.TelegramDispatchReport
import com.example.data.repository.GroceryRepository
import com.example.data.repository.SheetImportResult
import com.example.data.sync.CentralCatalogSyncManager
import com.example.data.sync.CentralSyncResult
import com.example.data.sync.GitHubPublishResult
import com.example.data.update.AppUpdateManager
import com.example.data.update.UpdateStatus
import com.example.data.website.EmailOtpState
import com.example.data.website.EmailOtpVerifier
import com.example.data.website.WebsiteAuthState
import com.example.data.website.WebsiteBackend
import com.example.data.website.WebsiteOrderSummary
import com.example.data.website.WebsiteUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroceryViewModel(application: Application) : AndroidViewModel(application) {

    val authManager = AuthManager.getInstance(application)
    val appUpdateManager = AppUpdateManager(application)
    val updateStatus: StateFlow<UpdateStatus> = appUpdateManager.updateStatus
    val showUpdateDialog = MutableStateFlow(false)
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val dispatcher = NotificationDispatcher(application, database.notificationLogDao())
    val repository = GroceryRepository(
        productDao = database.productDao(),
        orderDao = database.orderDao(),
        reviewDao = database.reviewDao(),
        favoriteDao = database.favoriteDao(),
        notificationLogDao = database.notificationLogDao(),
        cartDao = database.cartDao(),
        dispatcher = dispatcher
    )

    // Central Catalog Sync & GitHub Publishing Engine
    val centralSyncManager = CentralCatalogSyncManager(application, repository.productDaoInstance)
    val isSyncingCentral = MutableStateFlow(false)
    val isPublishingToGitHub = MutableStateFlow(false)
    val centralSyncResult = MutableStateFlow<CentralSyncResult?>(null)
    val gitHubPublishResult = MutableStateFlow<GitHubPublishResult?>(null)

    val centralSyncUrl = MutableStateFlow(centralSyncManager.centralSyncUrl)
    val lastSyncSummary = MutableStateFlow(centralSyncManager.lastSyncSummary)
    val isAutoSyncEnabled = MutableStateFlow(centralSyncManager.isAutoSyncEnabled)

    // Store Account (website) state. Declared before init: the init block
    // restores the session on launch, so these must already be initialized.
    val websiteBackend = WebsiteBackend(application)
    private val _websiteAuthState =
        MutableStateFlow<WebsiteAuthState>(WebsiteAuthState.SignedOut)
    val websiteAuthState: StateFlow<WebsiteAuthState> = _websiteAuthState.asStateFlow()
    private val _websiteOrders = MutableStateFlow<List<WebsiteOrderSummary>>(emptyList())
    val websiteOrders: StateFlow<List<WebsiteOrderSummary>> = _websiteOrders.asStateFlow()
    val isWebsiteSignedIn: Boolean
        get() = _websiteAuthState.value is WebsiteAuthState.SignedIn

    init {
        viewModelScope.launch {
            repository.syncOfficialCatalog()
            // Auto-sync with the live website catalog (spdelivery.reddevils.co.in)
            // on launch; falls back to the configured central URL otherwise.
            // Local Room database is retained if the network is unreachable.
            if (centralSyncManager.isAutoSyncEnabled) {
                try {
                    val websiteResult = centralSyncManager.fetchAndSyncFromWebsite()
                    if (websiteResult.success &&
                        (websiteResult.updatedCount > 0 || websiteResult.addedCount > 0)
                    ) {
                        centralSyncResult.value = websiteResult
                        refreshCentralSyncState()
                    } else if (centralSyncManager.centralSyncUrl.isNotBlank()) {
                        val result = centralSyncManager.fetchAndSyncCatalog()
                        if (result.success) {
                            centralSyncResult.value = result
                            refreshCentralSyncState()
                        }
                    }
                } catch (_: Exception) {
                    // Retain local offline Room database if remote is not reachable
                }
            }
        }
        // Restore durable Store Account session (profile + order history).
        restoreWebsiteSession()
    }

    /** Manual pull of the live website catalog (spdelivery.reddevils.co.in). */
    fun syncWithWebsite(onComplete: ((CentralSyncResult) -> Unit)? = null) {
        viewModelScope.launch {
            isSyncingCentral.value = true
            try {
                val result = centralSyncManager.fetchAndSyncFromWebsite()
                centralSyncResult.value = result
                refreshCentralSyncState()
                onComplete?.invoke(result)
            } catch (e: Exception) {
                val failure = CentralSyncResult(success = false, message = e.message ?: "Sync failed")
                centralSyncResult.value = failure
                onComplete?.invoke(failure)
            } finally {
                isSyncingCentral.value = false
            }
        }
    }

    // User Profile / Customer Registration powered by AuthManager
    val currentUser: StateFlow<UserProfile> = authManager.currentUser

    // Admin Access Control: strictly restricted to souravbrock@gmail.com
    val isCurrentUserAdmin: StateFlow<Boolean> = currentUser.map { user ->
        authManager.isAuthorizedAdmin(user)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Mode: Customer vs Admin
    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    // Search and Category
    val selectedCategory = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")

    // Raw Products
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Products
    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        selectedCategory,
        searchQuery
    ) { products, category, query ->
        products.filter { product ->
            val matchesCategory = when (category) {
                "All" -> true
                "Daily Essentials" -> product.isDailyEssential
                else -> product.category.equals(category, ignoreCase = true)
            }
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shopping Cart (Room-backed persistence)
    val cartEntities: StateFlow<List<com.example.data.model.CartItemEntity>> = repository.allCartEntities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItem>> = combine(allProducts, cartEntities) { products, entities ->
        val productMap = products.associateBy { it.id }
        entities.mapNotNull { entity ->
            productMap[entity.productId]?.let { prod ->
                CartItem(
                    product = prod,
                    quantity = entity.quantity,
                    portionLabel = entity.portionLabel
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Quick map of productId -> quantity for badges and catalog buttons
    val cartMap: StateFlow<Map<Long, Double>> = cartEntities.map { list ->
        list.associate { it.productId to it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val cartSubtotal: StateFlow<Double> = cartItems.map { items ->
        items.sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val deliveryFee: StateFlow<Double> = cartSubtotal.map { subtotal ->
        // Free delivery inside Shapoorji on orders above ₹199, else nominal ₹20
        if (subtotal >= 199.0 || subtotal == 0.0) 0.0 else 20.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartTotal: StateFlow<Double> = combine(cartSubtotal, deliveryFee) { sub, fee ->
        if (sub > 0) sub + fee else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Favorites
    val favoriteIds: StateFlow<List<Long>> = repository.favoriteProductIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Orders
    val allOrders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer's Orders
    val customerOrders: StateFlow<List<Order>> = combine(allOrders, currentUser) { orders, user ->
        orders.filter { it.customerEmail.equals(user.email, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notification Logs
    val notificationLogs = repository.allNotificationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shapoorji Location Verification State
    val selectedTower = MutableStateFlow(authManager.currentUser.value.tower.ifBlank { "Shukhobrishti Phase 1 - Tower A1" })
    val flatInput = MutableStateFlow(authManager.currentUser.value.flatNumber)
    val deliveryNotesInput = MutableStateFlow("Please leave at door / ring bell")
    val currentLatitude = MutableStateFlow(22.5695)
    val currentLongitude = MutableStateFlow(88.5195)
    val isLocationInsideShapoorji = MutableStateFlow(true)
    val locationVerificationMessage = MutableStateFlow("✓ Verified: Inside Shapoorji Shukhobrishti Delivery Zone")

    // UI Feedback
    val toastMessage = MutableStateFlow<String?>(null)

    fun toggleAdminMode() {
        val user = currentUser.value
        if (!authManager.isAuthorizedAdmin(user)) {
            _isAdminMode.value = false
            toastMessage.value = "Access Restricted: Device account '${user.email}' does not have administrator privileges. Admin access is reserved for ${AuthManager.ADMIN_EMAIL}."
            return
        }
        _isAdminMode.value = !_isAdminMode.value
    }

    fun setAdminMode(enabled: Boolean) {
        val user = currentUser.value
        if (enabled && !authManager.isAuthorizedAdmin(user)) {
            _isAdminMode.value = false
            toastMessage.value = "Access Restricted: Device account '${user.email}' does not have administrator privileges. Admin access is reserved for ${AuthManager.ADMIN_EMAIL}."
            return
        }
        _isAdminMode.value = enabled
    }

    // Customer Account Registration / Sign-In with Mail and Phone
    fun registerOrUpdateCustomer(
        name: String,
        email: String,
        phone: String,
        tower: String = selectedTower.value,
        flat: String = flatInput.value
    ) {
        val cleanTower = tower.trim().ifBlank { selectedTower.value }
        val cleanFlat = flat.trim().ifBlank { flatInput.value }

        val updatedUser = authManager.signInWithGoogle(
            name = name,
            email = email,
            phone = phone,
            tower = cleanTower,
            flat = cleanFlat
        )

        selectedTower.value = cleanTower
        flatInput.value = cleanFlat

        // If the new user is not admin, immediately revoke admin mode
        if (!authManager.isAuthorizedAdmin(updatedUser)) {
            _isAdminMode.value = false
        }

        toastMessage.value = "Signed in as ${updatedUser.name} (${updatedUser.email})"
    }

    fun signInWithGoogle(
        name: String,
        email: String,
        phone: String = currentUser.value.phone,
        tower: String = currentUser.value.tower,
        flat: String = currentUser.value.flatNumber
    ) {
        registerOrUpdateCustomer(
            name = name,
            email = email,
            phone = phone,
            tower = tower,
            flat = flat
        )
    }

    fun quickSignIn(email: String): Boolean {
        val user = authManager.quickSignIn(email) ?: return false
        selectedTower.value = user.tower
        flatInput.value = user.flatNumber
        if (!authManager.isAuthorizedAdmin(user)) {
            _isAdminMode.value = false
        }
        toastMessage.value = "Signed in as ${user.name} (${user.email})"
        return true
    }

    fun getRegisteredUsers(): List<UserProfile> = authManager.getAllRegisteredUsers()

    fun getLastRegisteredUser(): UserProfile? = authManager.getLastRegisteredUser()

    fun isEmailRegistered(email: String): Boolean = authManager.isEmailRegistered(email)

    fun signOut() {
        authManager.signOut()
        _isAdminMode.value = false
        toastMessage.value = "Signed out of Google account"
    }

    // Store Account (website) — durable identity shared with
    // spdelivery.reddevils.co.in. Survives reinstalls: profile + order
    // history are restored from the server after re-login.
    // (State is declared above init; functions live here.)
    /** Mirror the website profile into the local profile (keeps gating + admin checks working). */
    private fun applyWebsiteUser(user: WebsiteUser, silent: Boolean = false) {
        val tower = selectedTower.value.ifBlank { currentUser.value.tower }
        val flat = flatInput.value.ifBlank { currentUser.value.flatNumber }
        authManager.signInWithGoogle(
            name = user.fullName.ifBlank { currentUser.value.name },
            email = user.email,
            phone = user.phone.ifBlank { currentUser.value.phone },
            tower = tower,
            flat = flat
        )
        _websiteAuthState.value = WebsiteAuthState.SignedIn(user)
        if (!silent) toastMessage.value = "Signed in to Store Account (${user.email})"
    }

    fun restoreWebsiteSession() {
        viewModelScope.launch {
            _websiteAuthState.value = WebsiteAuthState.Loading
            try {
                val sessionUser = websiteBackend.fetchSession()
                if (sessionUser != null) {
                    applyWebsiteUser(sessionUser, silent = true)
                    _websiteOrders.value = websiteBackend.fetchOrders()
                } else {
                    _websiteAuthState.value = WebsiteAuthState.SignedOut
                }
            } catch (_: Exception) {
                _websiteAuthState.value = WebsiteAuthState.SignedOut
            }
        }
    }

    fun websiteLogin(email: String, password: String) {
        viewModelScope.launch {
            _websiteAuthState.value = WebsiteAuthState.Loading
            val result = websiteBackend.login(email, password)
            if (result.isSuccess) {
                applyWebsiteUser(result.getOrNull()!!)
                _websiteOrders.value = websiteBackend.fetchOrders()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Sign-in failed"
                _websiteAuthState.value = WebsiteAuthState.Error(msg)
                toastMessage.value = msg
            }
        }
    }

    fun websiteSignup(name: String, email: String, password: String, phone: String) {
        if (!otpVerifier.isVerified(email)) {
            _emailOtpState.value =
                EmailOtpState.Failed("Verify your email address first — request a code below")
            toastMessage.value = "Verify your email address first"
            return
        }
        viewModelScope.launch {
            _websiteAuthState.value = WebsiteAuthState.Loading
            val result = websiteBackend.signup(email, password)
            if (result.isSuccess) {
                // Push profile details to the website account (best-effort).
                try {
                    websiteBackend.updateProfile(
                        fullName = name,
                        phone = phone,
                        address = "${flatInput.value}, ${selectedTower.value}".trim().trim(',')
                    )
                } catch (_: Exception) {
                }
                val user = result.getOrNull()!!.copy(
                    fullName = name.ifBlank { result.getOrNull()!!.fullName },
                    phone = phone.ifBlank { result.getOrNull()!!.phone }
                )
                applyWebsiteUser(user)
                _websiteOrders.value = websiteBackend.fetchOrders()
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Account creation failed"
                _websiteAuthState.value = WebsiteAuthState.Error(msg)
                toastMessage.value = msg
            }
        }
    }

    fun websiteLogout() {
        viewModelScope.launch {
            try {
                websiteBackend.logout()
            } catch (_: Exception) {
            } finally {
                _websiteAuthState.value = WebsiteAuthState.SignedOut
                _websiteOrders.value = emptyList()
                toastMessage.value = "Signed out of Store Account"
            }
        }
    }

    fun refreshWebsiteOrders() {
        if (!isWebsiteSignedIn) return
        viewModelScope.launch {
            try {
                _websiteOrders.value = websiteBackend.fetchOrders()
            } catch (_: Exception) {
            }
        }
    }

    // Email verification for new Store Accounts (OTP via store SMTP).
    val otpVerifier = EmailOtpVerifier(getApplication())
    private val _emailOtpState =
        MutableStateFlow<EmailOtpState>(EmailOtpState.Idle)
    val emailOtpState: StateFlow<EmailOtpState> = _emailOtpState.asStateFlow()

    fun requestSignupOtp(email: String) {
        viewModelScope.launch {
            _emailOtpState.value = EmailOtpState.Sending
            val result = otpVerifier.sendCode(email)
            _emailOtpState.value = if (result.isSuccess) {
                EmailOtpState.CodeSent(
                    email = email.trim(),
                    resendAtMillis = System.currentTimeMillis() + 60_000L
                )
            } else {
                EmailOtpState.Failed(result.exceptionOrNull()?.message ?: "Could not send code")
            }
        }
    }

    fun confirmSignupOtp(
        name: String,
        email: String,
        password: String,
        phone: String,
        code: String
    ) {
        val check = otpVerifier.confirmCode(email, code)
        if (check.isFailure) {
            _emailOtpState.value =
                EmailOtpState.Failed(check.exceptionOrNull()?.message ?: "Wrong code")
            return
        }
        _emailOtpState.value = EmailOtpState.Verified(email.trim())
        websiteSignup(name, email, password, phone)
    }

    fun resetEmailOtp() {
        _emailOtpState.value = EmailOtpState.Idle
    }

    // Cart operations (Room-backed)
    fun addToCart(productId: Long, quantity: Double = 1.0, portionLabel: String = "") {
        viewModelScope.launch {
            repository.addToCart(productId, quantity, portionLabel)
        }
    }

    fun setPortionInCart(productId: Long, fraction: Double, portionLabel: String) {
        viewModelScope.launch {
            repository.setPortionInCart(productId, fraction, portionLabel)
        }
    }

    fun setCartPortion(productId: Long, fraction: Double, portionLabel: String) {
        setPortionInCart(productId, fraction, portionLabel)
    }

    fun removeFromCart(productId: Long, fractionStep: Double = 1.0) {
        viewModelScope.launch {
            repository.removeFromCart(productId, fractionStep)
        }
    }

    fun deleteFromCart(productId: Long) {
        viewModelScope.launch {
            repository.deleteCartItem(productId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    // Favorites
    fun toggleFavorite(productId: Long) {
        viewModelScope.launch {
            val isFav = favoriteIds.value.contains(productId)
            repository.toggleFavorite(productId, isFav)
        }
    }

    // Location Verification for Shapoorji
    fun updateCoordinates(lat: Double, lng: Double) {
        currentLatitude.value = lat
        currentLongitude.value = lng
        val inside = ShapoorjiGeo.isInsideShapoorji(lat, lng)
        isLocationInsideShapoorji.value = inside
        if (inside) {
            val dist = ShapoorjiGeo.distanceInMeters(lat, lng, ShapoorjiGeo.CENTER_LATITUDE, ShapoorjiGeo.CENTER_LONGITUDE).toInt()
            locationVerificationMessage.value = "✓ Verified Inside Shapoorji (Offset: ${dist}m from Shukhobrishti center)"
        } else {
            locationVerificationMessage.value = "❌ Outside Delivery Zone! Shapoorji Delivery only serves within Shapoorji Shukhobrishti."
        }
    }

    fun setLocationToPreset(isInside: Boolean) {
        if (isInside) {
            updateCoordinates(22.5695, 88.5195)
        } else {
            // Outside Shapoorji: e.g. Sector V / Salt Lake (22.5868, 88.4355)
            updateCoordinates(22.5868, 88.4355)
        }
    }

    // Order Placement (requires Store Account sign-in so the order is also
    // recorded on spdelivery.reddevils.co.in and survives reinstalls).
    fun placeOrder(onSuccess: (Order) -> Unit, onError: (String) -> Unit) {
        if (!isLocationInsideShapoorji.value) {
            onError("Orders cannot be placed outside Shapoorji! Please verify your location inside Shapoorji Shukhobrishti.")
            return
        }

        if (!isWebsiteSignedIn) {
            onError("Please sign in with your Store Account first (tap the profile icon) — orders are synced to spdelivery.reddevils.co.in.")
            return
        }

        val items = cartItems.value
        if (items.isEmpty()) {
            onError("Your grocery cart is empty!")
            return
        }

        viewModelScope.launch {
            try {
                val orderItems = items.map {
                    OrderItem(
                        productId = it.product.id,
                        productName = it.product.name,
                        unit = it.product.unit,
                        price = it.product.price,
                        quantity = it.quantity,
                        portionLabel = it.portionLabel
                    )
                }

                val order = repository.placeOrder(
                    customerName = currentUser.value.name,
                    customerEmail = currentUser.value.email,
                    customerPhone = currentUser.value.phone,
                    towerName = selectedTower.value,
                    flatNumber = flatInput.value,
                    deliveryNotes = deliveryNotesInput.value,
                    latitude = currentLatitude.value,
                    longitude = currentLongitude.value,
                    isLocationVerified = isLocationInsideShapoorji.value,
                    items = orderItems,
                    subtotal = cartSubtotal.value,
                    deliveryFee = deliveryFee.value,
                    discount = 0.0,
                    grandTotal = cartTotal.value,
                    paymentMethod = "Pay on Delivery (Cash / UPI QR)"
                )

                // Dual-write: mirror the order to the website (best-effort — the
                // local order plus email/Telegram alerts already succeeded).
                var storeSyncNote = ""
                try {
                    val webItems = websiteBackend.resolveWebsiteItems(
                        items.map { item ->
                            Triple(
                                item.product.name.split("|").first().trim(),
                                item.quantity,
                                item.product.unit to item.product.price
                            )
                        }
                    )
                    if (webItems.isNotEmpty()) {
                        val webResult = websiteBackend.placeOrder(
                            customerName = currentUser.value.name,
                            customerPhone = currentUser.value.phone,
                            tower = selectedTower.value,
                            flat = flatInput.value,
                            notes = deliveryNotesInput.value,
                            items = webItems
                        )
                        if (webResult.isSuccess) {
                            storeSyncNote = " • synced to store"
                            refreshWebsiteOrders()
                        } else {
                            storeSyncNote = " • store sync failed (${webResult.exceptionOrNull()?.message})"
                        }
                    }
                } catch (e: Exception) {
                    storeSyncNote = " • store sync failed (${e.message})"
                }

                clearCart()
                toastMessage.value = "Order #${order.orderNumber} placed! Email sent from order@spdelivery.reddevils.co.in$storeSyncNote"
                onSuccess(order)
            } catch (e: Exception) {
                onError("Failed to place order: ${e.message}")
            }
        }
    }

    // Admin: Advance order status
    fun advanceOrderStatus(order: Order) {
        val next = order.status.nextStatus() ?: return
        viewModelScope.launch {
            repository.updateOrderStatus(order, next)
            toastMessage.value = "Order #${order.orderNumber} updated to ${next.label}! Email & Telegram alerts sent."
        }
    }

    // Admin: Update Daily Price
    fun updateProductDailyPrice(productId: Long, newPrice: Double) {
        viewModelScope.launch {
            repository.updateDailyPrice(productId, newPrice)
            toastMessage.value = "Updated daily price to ₹$newPrice"
        }
    }

    // Admin: Add Product
    fun addProduct(
        name: String,
        category: String,
        unit: String,
        price: Double,
        mrp: Double,
        stockQty: Int,
        description: String,
        imageUrl: String,
        isDailyEssential: Boolean
    ) {
        viewModelScope.launch {
            val product = Product(
                name = name,
                category = category,
                unit = unit,
                price = price,
                mrp = mrp,
                stockQty = stockQty,
                description = description,
                imageUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400" },
                isDailyEssential = isDailyEssential
            )
            repository.insertProduct(product)
            toastMessage.value = "Added product: $name"
        }
    }

    // Admin: Delete Product
    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            toastMessage.value = "Deleted product: ${product.name}"
        }
    }

    // Admin: Reset / Sync Official Catalog
    fun resetToOfficialCatalog() {
        viewModelScope.launch {
            repository.resetToOfficialCatalog()
            toastMessage.value = "Reset to official catalog (37 fresh produce items with live prices)"
        }
    }

    // Admin: Import Sheet 2 Data (Picture URLs & Future Products)
    val sheetImportResult = MutableStateFlow<SheetImportResult?>(null)

    fun importSheetData(sheetText: String, onComplete: ((SheetImportResult) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.importSheetData(sheetText)
            sheetImportResult.value = result
            toastMessage.value = "Imported: ${result.updatedCount} photos updated, ${result.addedCount} future products added"
            onComplete?.invoke(result)
        }
    }

    fun clearSheetImportResult() {
        sheetImportResult.value = null
    }

    // Central Catalog Sync & GitHub Publishing Methods
    fun refreshCentralSyncState() {
        centralSyncUrl.value = centralSyncManager.centralSyncUrl
        lastSyncSummary.value = centralSyncManager.lastSyncSummary
        isAutoSyncEnabled.value = centralSyncManager.isAutoSyncEnabled
    }

    fun syncWithCentralCloud(targetUrl: String? = null, onComplete: ((CentralSyncResult) -> Unit)? = null) {
        viewModelScope.launch {
            isSyncingCentral.value = true
            try {
                // Pull from the live website catalog first to get instant rates & photos
                centralSyncManager.fetchAndSyncFromWebsite()

                val result = centralSyncManager.fetchAndSyncCatalog(targetUrl)
                centralSyncResult.value = result
                refreshCentralSyncState()
                if (result.success) {
                    toastMessage.value = "Central sync complete: ${result.updatedCount} photos updated, ${result.addedCount} items added"
                } else {
                    toastMessage.value = result.message
                }
                onComplete?.invoke(result)
            } finally {
                isSyncingCentral.value = false
            }
        }
    }

    fun pushCatalogToGitHubGist(token: String, gistId: String?, onComplete: ((GitHubPublishResult) -> Unit)? = null) {
        viewModelScope.launch {
            isPublishingToGitHub.value = true
            try {
                val currentProducts = allProducts.value
                val result = centralSyncManager.pushToGitHubGist(token, gistId, currentProducts)
                gitHubPublishResult.value = result
                refreshCentralSyncState()
                if (result.success) {
                    toastMessage.value = "Catalog published to GitHub Gist successfully!"
                } else {
                    toastMessage.value = result.message
                }
                onComplete?.invoke(result)
            } finally {
                isPublishingToGitHub.value = false
            }
        }
    }

    fun pushCatalogToGitHubRepo(token: String, ownerRepo: String, path: String = "catalog.json", branch: String = "main", onComplete: ((GitHubPublishResult) -> Unit)? = null) {
        viewModelScope.launch {
            isPublishingToGitHub.value = true
            try {
                val currentProducts = allProducts.value
                val result = centralSyncManager.pushToGitHubRepo(token, ownerRepo, path, branch, currentProducts)
                gitHubPublishResult.value = result
                refreshCentralSyncState()
                if (result.success) {
                    toastMessage.value = "Catalog committed to GitHub repository successfully!"
                } else {
                    toastMessage.value = result.message
                }
                onComplete?.invoke(result)
            } finally {
                isPublishingToGitHub.value = false
            }
        }
    }

    fun saveCentralSyncSettings(
        url: String,
        autoSync: Boolean,
        gistId: String? = null,
        repo: String? = null,
        path: String? = null,
        token: String? = null
    ) {
        centralSyncManager.centralSyncUrl = url
        centralSyncManager.isAutoSyncEnabled = autoSync
        if (!gistId.isNullOrBlank()) centralSyncManager.githubGistId = gistId
        if (!repo.isNullOrBlank()) centralSyncManager.githubRepo = repo
        if (!path.isNullOrBlank()) centralSyncManager.githubFilePath = path
        if (!token.isNullOrBlank()) centralSyncManager.githubToken = token
        refreshCentralSyncState()
        toastMessage.value = "Central sync settings saved."
    }

    fun exportCatalogJson(): String = centralSyncManager.exportCatalogAsJson(allProducts.value)

    fun exportCatalogCsv(): String = centralSyncManager.exportCatalogAsCsv(allProducts.value)

    // Admin: Update Product (full object)
    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
            toastMessage.value = "Updated ${product.name}"
        }
    }

    // Admin: Update Stock & Inventory
    fun updateStock(productId: Long, stockQty: Int) {
        viewModelScope.launch {
            repository.updateStock(productId, stockQty)
            toastMessage.value = "Updated stock to $stockQty"
        }
    }

    // Admin: Update Product Details (name, price, description, imageUrl, unit, category, fractional settings)
    fun updateProductDetails(
        productId: Long,
        name: String,
        price: Double,
        description: String,
        imageUrl: String,
        unit: String = "1 kg",
        category: String = "Vegetables",
        allowFractional: Boolean = false,
        fractionStepGrams: Int = 250
    ) {
        viewModelScope.launch {
            repository.updateProductDetails(productId, name, price, description, imageUrl, unit, category, allowFractional, fractionStepGrams)
            toastMessage.value = "Updated product: $name"
        }
    }

    // Admin: Add New Product into Catalog
    fun addProduct(
        name: String,
        category: String = "Vegetables",
        unit: String = "1 kg",
        price: Double,
        mrp: Double = price * 1.15,
        stockQty: Int = 50,
        description: String = "",
        imageUrl: String = "",
        isDailyEssential: Boolean = false,
        allowFractional: Boolean = false,
        fractionStepGrams: Int = 250
    ) {
        viewModelScope.launch {
            val prod = Product(
                name = name,
                price = price,
                mrp = mrp,
                stockQty = stockQty,
                description = description,
                imageUrl = imageUrl,
                unit = unit,
                category = category,
                isDailyEssential = isDailyEssential,
                allowFractional = allowFractional,
                fractionStepGrams = fractionStepGrams
            )
            repository.insertProduct(prod)
            toastMessage.value = "Added $name to catalog"
        }
    }

    // Customer: Submit Review & Rating
    fun submitReview(productId: Long, rating: Int, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addReview(
                productId = productId,
                customerName = currentUser.value.name,
                customerEmail = currentUser.value.email,
                rating = rating,
                comment = comment
            )
            toastMessage.value = "Thank you for rating this product!"
            onSuccess()
        }
    }

    // Notification & Dispatcher Configuration States
    val telegramBotToken = MutableStateFlow(dispatcher.telegramBotToken)
    val adminChatIds = MutableStateFlow(dispatcher.adminChatIds)
    val managerChatIds = MutableStateFlow(dispatcher.managerChatIds)
    val staffChatIds = MutableStateFlow(dispatcher.staffChatIds)

    val smtpHost = MutableStateFlow(dispatcher.smtpHost)
    val smtpPort = MutableStateFlow(dispatcher.smtpPort.toString())
    val smtpUsername = MutableStateFlow(dispatcher.smtpUsername)
    val smtpPassword = MutableStateFlow(dispatcher.smtpPassword)
    val senderEmail = MutableStateFlow(dispatcher.senderEmail)
    val adminEmail = MutableStateFlow(dispatcher.adminEmail)

    val isTestingTelegram = MutableStateFlow(false)
    val isTestingEmail = MutableStateFlow(false)

    fun updateAllNotificationSettings(
        botToken: String,
        adminChats: String,
        managerChats: String,
        staffChats: String,
        host: String,
        port: String,
        username: String,
        pass: String,
        sender: String,
        adminMail: String
    ) {
        dispatcher.telegramBotToken = botToken.trim()
        dispatcher.adminChatIds = adminChats.trim()
        dispatcher.managerChatIds = managerChats.trim()
        dispatcher.staffChatIds = staffChats.trim()

        dispatcher.smtpHost = host.trim()
        dispatcher.smtpPort = port.trim().toIntOrNull() ?: 465
        dispatcher.smtpUsername = username.trim()
        dispatcher.smtpPassword = pass.trim()
        dispatcher.senderEmail = sender.trim()
        dispatcher.adminEmail = adminMail.trim()

        telegramBotToken.value = dispatcher.telegramBotToken
        adminChatIds.value = dispatcher.adminChatIds
        managerChatIds.value = dispatcher.managerChatIds
        staffChatIds.value = dispatcher.staffChatIds
        smtpHost.value = dispatcher.smtpHost
        smtpPort.value = dispatcher.smtpPort.toString()
        smtpUsername.value = dispatcher.smtpUsername
        smtpPassword.value = dispatcher.smtpPassword
        senderEmail.value = dispatcher.senderEmail
        adminEmail.value = dispatcher.adminEmail

        toastMessage.value = "Saved Telegram & Email notification settings"
    }

    fun sendTestTelegramAlert(onResult: (TelegramDispatchReport) -> Unit) {
        viewModelScope.launch {
            isTestingTelegram.value = true
            try {
                val report = dispatcher.sendTestTelegramPing()
                toastMessage.value = "Telegram: ${report.successCount}/${report.totalCount} sent"
                onResult(report)
            } finally {
                isTestingTelegram.value = false
            }
        }
    }

    fun sendTestEmailAlert(onResult: (SmtpResult) -> Unit) {
        viewModelScope.launch {
            isTestingEmail.value = true
            try {
                val result = dispatcher.sendTestEmailPing()
                if (result.success) {
                    toastMessage.value = "Test email sent successfully!"
                } else {
                    toastMessage.value = "Email dispatch failed: ${result.message}"
                }
                onResult(result)
            } finally {
                isTestingEmail.value = false
            }
        }
    }

    // App Update & Upgrade Engine — checks GitHub Releases latest.json
    // (+ spd.reddevils.co.in mirror), same feed Obtainium/IzzyOnDroid use.
    fun checkForAppUpdates() {
        showUpdateDialog.value = true
        viewModelScope.launch {
            try {
                appUpdateManager.checkForUpdatesFromGitHub()
            } catch (e: Exception) {
                appUpdateManager.checkForUpdates(forcedCheck = true)
            }
        }
    }

    fun simulateUpgradeAvailable() {
        showUpdateDialog.value = true
        appUpdateManager.simulateUpdateAvailable()
    }

    fun dismissUpdateDialog() {
        showUpdateDialog.value = false
        appUpdateManager.dismissUpdate()
    }

    // Website catalog sync (spdelivery.reddevils.co.in is the central database).
    fun syncWebsiteCatalogNow(onComplete: ((CentralSyncResult) -> Unit)? = null) {
        syncWithWebsite(onComplete)
    }
}
