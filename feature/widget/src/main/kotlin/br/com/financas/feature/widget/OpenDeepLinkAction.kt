package br.com.financas.feature.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * O widget não conhece a `Activity` que trata `financas://add` (ela vive no
 * `:app`, que depende deste módulo — não o contrário). Resolvemos por
 * `Intent` implícito via `ActionCallback`, já que `actionStartActivity` só
 * aceita uma classe/`ComponentName` conhecidos em tempo de composição.
 */
class OpenDeepLinkAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val uri = parameters[KEY_DEEP_LINK] ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setPackage(context.packageName)
        context.startActivity(intent)
    }

    companion object {
        val KEY_DEEP_LINK = ActionParameters.Key<String>("deep_link")
    }
}
