import extension.setupDependencyInjection

/*
 * Copyright (c) 2026 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

plugins {
    id("io.element.android-library")
    alias(libs.plugins.kotlin.serialization)
}

setupDependencyInjection()

android {
    namespace = "io.element.android.features.ai.impl"
}

dependencies {
    api(projects.features.ai.api)
    implementation(projects.appconfig)
    implementation(projects.libraries.androidutils)
    implementation(projects.libraries.core)
    implementation(projects.libraries.di)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.network)
    implementation(projects.libraries.sessionStorage.api)
    implementation(libs.coroutines.core)
    implementation(platform(libs.network.okhttp.bom))
    implementation(libs.network.okhttp)
    implementation(libs.serialization.json)
}
