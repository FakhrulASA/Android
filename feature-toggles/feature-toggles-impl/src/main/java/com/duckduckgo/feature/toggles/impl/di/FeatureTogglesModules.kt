/*
 * Copyright (c) 2021 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.feature.toggles.impl.di

import com.duckduckgo.app.global.plugins.PluginPoint
import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.feature.toggles.api.FeatureTogglesPlugin
import com.duckduckgo.feature.toggles.impl.FeatureCustomConfigPluginPoint
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dagger.multibindings.Multibinds
import dagger.SingleInstanceIn

@Module
@ContributesTo(AppScope::class)
abstract class FeatureTogglesBindingModule {

    @Multibinds
    abstract fun provideFeatureTogglesPlugins(): Set<@JvmSuppressWildcards FeatureTogglesPlugin>

}

@Module
@ContributesTo(AppScope::class)
class FeatureTogglesModule {

    @Provides
    @SingleInstanceIn(AppScope::class)
    fun provideFeatureTogglesPluginPoint(toggles: Set<@JvmSuppressWildcards FeatureTogglesPlugin>): PluginPoint<FeatureTogglesPlugin> {
        return FeatureCustomConfigPluginPoint(toggles)
    }
}
