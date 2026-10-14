package com.wolftower.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = ProfilesRepository.load(this)
        setContent { MaterialTheme { Screen(config) } }
    }

    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen(config: ProfilesConfig) {
        val ctx = this
        var enabled by remember { mutableStateOf(Settings.enabledIds(ctx)) }
        var strategies by remember {
            mutableStateOf(
                config.profiles.associate { it.id to Settings.strategyFor(ctx, it.id, config.defaultStrategy) }
            )
        }
        var crash by remember { mutableStateOf(CrashInfo.read(ctx)) }
        val running by DpiVpnService.running.collectAsState()
        val log by DpiVpnService.log.collectAsState()
        val probe by AutoProbe.state.collectAsState()

        // После автоподбора подтягиваем выбранные стратегии в интерфейс
        LaunchedEffect(probe.done) {
            if (probe.done) {
                strategies = config.profiles.associate {
                    it.id to Settings.strategyFor(ctx, it.id, config.defaultStrategy)
                }
            }
        }

        fun startVpn() {
            startService(Intent(ctx, DpiVpnService::class.java).setAction(DpiVpnService.ACTION_START))
        }

        val vpnPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { r -> if (r.resultCode == Activity.RESULT_OK) startVpn() }

        Column(
            Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Wolf Tower", style = MaterialTheme.typography.headlineSmall)

            if (crash != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Последний сбой (можно скопировать и прислать)", style = MaterialTheme.typography.titleSmall)
                        SelectionContainer { Text(crash ?: "", style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = {
                            CrashInfo.dismiss(ctx)
                            crash = null
                        }) { Text("Скрыть") }
                    }
                }
            }

            config.profiles.forEach { p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Switch(
                                checked = p.id in enabled,
                                enabled = !running,
                                onCheckedChange = { on ->
                                    enabled = if (on) enabled + p.id else enabled - p.id
                                    Settings.setEnabledIds(ctx, enabled)
                                },
                            )
                        }
                        if (p.id in enabled) {
                            val current = strategies[p.id] ?: config.defaultStrategy
                            val score = probe.scores[p.id]?.let { " (проверка: $it)" } ?: ""
                            Text(
                                "Стратегия: " + (if (current == "none") "без обхода" else current) + score,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (listOf("none") + config.strategies).forEach { s ->
                                    FilterChip(
                                        selected = s == current,
                                        enabled = !running,
                                        onClick = {
                                            Settings.setStrategyFor(ctx, p.id, s)
                                            strategies = strategies + (p.id to s)
                                        },
                                        label = { Text(if (s == "none") "без обхода" else s) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !running && !probe.running && enabled.isNotEmpty(),
                onClick = {
                    AutoProbe.start(
                        ctx.applicationContext,
                        config.profiles.filter { it.id in enabled },
                        config.strategies,
                    )
                },
            ) { Text("Подобрать стратегии автоматически") }

            if (!running && enabled.isEmpty()) {
                Text("Включи хотя бы один сервис", style = MaterialTheme.typography.bodySmall)
            }
            if (running) {
                Text("Для подбора сначала остановите защиту", style = MaterialTheme.typography.bodySmall)
            }
            if (probe.running) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(probe.progress, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { AutoProbe.cancel() }) { Text("Отмена") }
            }
            if (probe.report.isNotEmpty()) {
                Text("Результат подбора", style = MaterialTheme.typography.titleSmall)
                Text(probe.report, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !probe.running,
                onClick = {
                    if (running) {
                        startService(
                            Intent(ctx, DpiVpnService::class.java).setAction(DpiVpnService.ACTION_STOP)
                        )
                    } else {
                        val prepare = VpnService.prepare(ctx)
                        if (prepare != null) vpnPermission.launch(prepare) else startVpn()
                    }
                },
            ) { Text(if (running) "Остановить" else "Запустить") }

            Text("Лог", style = MaterialTheme.typography.titleSmall)
            Text(log.joinToString("\n"), style = MaterialTheme.typography.bodySmall)
        }
    }
}
