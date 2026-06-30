/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl

import org.matrix.rustcomponents.sdk.ClientBuilder

private const val APP_API_ALIASES_METHOD = "appApiAliasesEnabled"

internal fun ClientBuilder.appApiAliasesEnabledCompat(enabled: Boolean): ClientBuilder {
    val method = javaClass.methods.firstOrNull {
        it.name == APP_API_ALIASES_METHOD &&
            it.parameterTypes.size == 1 &&
            it.parameterTypes[0] == Boolean::class.javaPrimitiveType
    }

    if (method == null) {
        check(!enabled) { "The Matrix Rust SDK does not support app API aliases." }
        return this
    }

    if (hasNoHandle()) {
        return this
    }

    return method.invoke(this, enabled) as ClientBuilder
}

private fun ClientBuilder.hasNoHandle(): Boolean {
    return runCatching {
        val field = ClientBuilder::class.java.getDeclaredField("handle")
        field.isAccessible = true
        field.getLong(this) == 0L
    }.getOrDefault(false)
}
