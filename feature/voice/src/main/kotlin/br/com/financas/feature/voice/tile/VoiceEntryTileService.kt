package br.com.financas.feature.voice.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import br.com.financas.feature.voice.capture.VoiceCaptureActivity
import dagger.hilt.android.AndroidEntryPoint

/** Rota 5 (§5.6): puxar a barra de status + um toque = microfone escutando. */
@AndroidEntryPoint
class VoiceEntryTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, VoiceCaptureActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
