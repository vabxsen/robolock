package com.robolock.app.blocking

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * The owners a [ComposeView] needs before it can be attached to a window it does not control.
 *
 * A Compose view added straight to WindowManager crashes on composition: outside an Activity there
 * is no lifecycle, no ViewModelStore and no saved-state registry for it to find. This supplies all
 * three and drives the lifecycle by hand.
 *
 * Ordering matters. [SavedStateRegistryController.performRestore] must run before the lifecycle
 * moves past INITIALIZED, or the registry throws when Compose reads it.
 */
class OverlayHost(private val context: Context) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var view: ComposeView? = null

    /** Builds the view and moves it to RESUMED, ready to be added to a window. */
    fun createView(content: @Composable () -> Unit): View {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayHost)
            setViewTreeViewModelStoreOwner(this@OverlayHost)
            setViewTreeSavedStateRegistryOwner(this@OverlayHost)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { content() }
        }

        view = composeView
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        return composeView
    }

    /** Tears the composition down. Safe to call more than once. */
    fun destroy() {
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        view = null
    }
}
