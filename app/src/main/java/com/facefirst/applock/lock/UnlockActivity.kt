package com.facefirst.applock.lock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.facefirst.applock.R

/**
 * FAZ 3 GEÇİCİ KİLİT EKRANI: doğrulama yok, yalnızca kilit akışını sınamak içindir.
 * FAZ 4 (biyometrik) ve FAZ 5 (PIN) bunu gerçek doğrulamayla değiştirecek.
 * Bu sürüm gerçek koruma SAĞLAMAZ.
 */
class UnlockActivity : ComponentActivity() {
    private lateinit var target: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = intent.getStringExtra(EXTRA_PACKAGE) ?: run { goHome(); return }
        setContent {
            BackHandler { goHome() }
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.locked_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.stub_note), Modifier.padding(vertical = 16.dp))
                        Button(onClick = { unlock() }) { Text(stringResource(R.string.stub_unlock)) }
                    }
                }
            }
        }
    }

    private fun unlock() {
        LockRuntime.machine?.onUnlocked(target)
        packageManager.getLaunchIntentForPackage(target)?.let {
            startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object { const val EXTRA_PACKAGE = "target_package" }
}
