package com.goalnudge.app.overlay

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Compose butuh Lifecycle + SavedStateRegistry owner. Window yang ditambahkan lewat
 * WindowManager (bukan Activity/Fragment) tidak punya keduanya secara default, jadi kita
 * sediakan sendiri dan tempel ke ComposeView via ViewTree*Owner.set(...).
 */
class OverlayLifecycleOwner : SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun performRestore(bundle: Bundle?) {
        savedStateRegistryController.performRestore(bundle)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}
