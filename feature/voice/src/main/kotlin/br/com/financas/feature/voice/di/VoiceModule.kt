package br.com.financas.feature.voice.di

import br.com.financas.feature.voice.notification.AndroidEntryNotifier
import br.com.financas.feature.voice.notification.EntryNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceModule {

    @Binds
    abstract fun bindEntryNotifier(impl: AndroidEntryNotifier): EntryNotifier
}
