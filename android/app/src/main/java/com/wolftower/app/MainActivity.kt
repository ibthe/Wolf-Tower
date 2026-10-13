package com.wolftower.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profiles = ProfilesRepository.load(this)
        setContent { MaterialTheme { Screen(profiles) } }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    private fun Screen(profiles: List<Profile>) {
        var enabled by remember { mutableStateOf(Settings.enabledIds(this)) }
        var strategy by remember { mutableStateOf(Settings.strategy(this)) }
        val running by DpiVpnService.running.collectAsState()
        val log by DpiVpnService.log.collectAsState()

        val strategies = profiles.filter { it.id in enabled }
            .flatMap { it.strategies }.distinct()
            .ifEmpty { profiles.flatMap { it.strategies }.distinct() }

        fun startService() {
            startService(Intent(this, DpiVpnService::class.java).setAction(DpiVpnService.ACTION_START))
        }

        val vpnPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { r -> if (r.resultCode == Activity.RESULT_OK) startService() }

        Column(
            Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Сервисы", style = MaterialTheme.typography.titleLarge)
            profiles.forEach { p ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.name)
                    Switch(
                        checked = p.id in enabled,
                        enabled = !running,
                        onCheckedChange = { on ->
                            enabled = if (on) enabled + p.id else enabled - p.id
                            Settings.setEnabledIds(this@MainActivity, enabled)
                        },
                    )
                }
            }

            Text("Стратегия", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                strategies.forEach { s ->
                    FilterChip(
                        selected = s == strategy,
                        enabled = !running,
                        onClick = { strategy = s; Settings.setStrategy(this@MainActivity, s) },
                        label = { Text(s) },
                    )
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (running) {
                        startService(
                            Intent(this@MainActivity, DpiVpnService::class.java)
                                .setAction(DpiVpnService.ACTION_STOP)
                        )
                    } else {
                        val prepare = VpnService.prepare(this@MainActivity)
                        if (prepare != null) vpnPermission.launch(prepare) else startService()
                    }
                },
            ) { Text(if (running) "Остановить" else "Запустить") }

            Text("Лог", style = MaterialTheme.typography.titleMedium)
            Text(log.joinToString("\n"), style = MaterialTheme.typography.bodySmall)
        }
    }
}
