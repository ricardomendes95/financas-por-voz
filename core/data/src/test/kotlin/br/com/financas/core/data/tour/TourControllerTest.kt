package br.com.financas.core.data.tour

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TourControllerTest {

    private fun newController(): TourController =
        TourController(TourPreferences(ApplicationProvider.getApplicationContext()))

    @Test
    fun `start posiciona no primeiro passo`() = runTest {
        val controller = newController()

        controller.start()

        assertThat(controller.currentStep.value).isEqualTo(TourStep.entries.first())
    }

    @Test
    fun `next avanca na ordem dos passos`() = runTest {
        val controller = newController()
        controller.start()

        controller.next()

        assertThat(controller.currentStep.value).isEqualTo(TourStep.entries[1])
    }

    @Test
    fun `next no ultimo passo encerra o tour`() = runTest {
        val controller = newController()
        controller.start()
        repeat(TourStep.entries.size - 1) { controller.next() }

        controller.next()

        assertThat(controller.currentStep.value).isNull()
    }

    @Test
    fun `skip encerra o tour a partir de qualquer passo`() = runTest {
        val controller = newController()
        controller.start()
        controller.next()

        controller.skip()

        assertThat(controller.currentStep.value).isNull()
    }

    @Test
    fun `finalizar o tour marca a preferencia como concluida`() {
        // Sem runTest de propósito: o marcador roda em Dispatchers.IO real, próprio do
        // controller (não do escopo de teste) — misturado com o relógio virtual do
        // runTest, o polling abaixo nunca cederia tempo real de verdade para essa
        // coroutine terminar, e o teste travaria até estourar o timeout.
        val preferences = TourPreferences(ApplicationProvider.getApplicationContext())
        val controller = TourController(preferences)
        controller.start()

        controller.skip()

        val deadline = System.currentTimeMillis() + 2_000
        var completed = false
        while (System.currentTimeMillis() < deadline && !completed) {
            completed = runBlocking { preferences.observeCompleted().first() }
            if (!completed) Thread.sleep(20)
        }
        assertThat(completed).isTrue()
    }
}
