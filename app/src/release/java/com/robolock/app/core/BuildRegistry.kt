package com.robolock.app.core

import com.robolock.rules.DefaultSupportedApps
import com.robolock.rules.SupportedAppRegistry

/** Production watch list: the real apps Robolock supports. */
internal object BuildRegistry {
    val registry: SupportedAppRegistry = DefaultSupportedApps
}
