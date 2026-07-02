package com.hege.dingpay.ui

import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hege.dingpay.data.BroadcastLanguage
import com.hege.dingpay.data.NotificationDiagnostic
import com.hege.dingpay.data.PaymentRecordEntity
import com.hege.dingpay.data.PaymentRule
import com.hege.dingpay.data.PaymentSourceType
import com.hege.dingpay.data.joinRuleValues
import com.hege.dingpay.data.splitRuleValues
import com.hege.dingpay.di.AppContainer
import com.hege.dingpay.license.LicenseState
import com.hege.dingpay.service.OemKeepAliveHelper
import com.hege.dingpay.service.SystemStatus
import com.hege.dingpay.ui.theme.DingAccent
import com.hege.dingpay.ui.theme.DingAccentSoft
import com.hege.dingpay.ui.theme.DingBackground
import com.hege.dingpay.ui.theme.DingInk
import com.hege.dingpay.ui.theme.DingInkMuted
import com.hege.dingpay.ui.theme.DingLine
import com.hege.dingpay.ui.theme.DingPrimary
import com.hege.dingpay.ui.theme.DingPrimaryDark
import com.hege.dingpay.ui.theme.DingPrimarySoft
import com.hege.dingpay.ui.theme.DingSecondary
import com.hege.dingpay.ui.theme.DingSecondarySoft
import com.hege.dingpay.ui.theme.DingSuccess
import com.hege.dingpay.ui.theme.DingSurface
import com.hege.dingpay.ui.theme.DingWarning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object Routes {
    const val dashboard = "dashboard"
    const val records = "records"
    const val settings = "settings"
    const val listener = "listener"
    const val license = "license"
}

@Composable
fun AppRoot(container: AppContainer) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val rules by container.settingsStore.rulesFlow.collectAsState(initial = emptyList())
    val records by container.paymentRepository.observeRecent().collectAsState(initial = emptyList())
    val allRecords by container.paymentRepository.observeAll().collectAsState(initial = emptyList())
    val language by container.settingsStore.languageFlow.collectAsState(initial = BroadcastLanguage.ZH)
    val license by container.licenseRepository.stateFlow.collectAsState(initial = LicenseState())
    val diagnostic by container.settingsStore.diagnosticFlow.collectAsState(initial = NotificationDiagnostic.EMPTY)

    LaunchedEffect(Unit) {
        SystemStatus.requestNotificationListenerRebind(context)
    }

    NavHost(navController = navController, startDestination = Routes.dashboard) {
        composable(Routes.dashboard) {
            DashboardScreen(
                rules = rules,
                records = records,
                license = license,
                ttsReady = container.ttsManager.isReady(),
                onToggleRule = { id, enabled ->
                    scope.launch { container.settingsStore.setRuleEnabled(id, enabled) }
                },
                onRecords = { navController.navigate(Routes.records) },
                onRuleVoice = { navController.navigate(Routes.listener) },
                onSettings = { navController.navigate(Routes.settings) },
                onLicense = { navController.navigate(Routes.license) }
            )
        }
        composable(Routes.records) {
            RecordsScreen(
                records = allRecords,
                onBack = navController::popBackStack,
                onClear = { scope.launch { container.paymentRepository.clear() } }
            )
        }
        composable(Routes.settings) {
            SettingsScreen(
                language = language,
                license = license,
                onBack = navController::popBackStack,
                onLanguage = { scope.launch { container.settingsStore.setLanguage(it) } },
                onListener = { navController.navigate(Routes.listener) },
                onLicense = { navController.navigate(Routes.license) },
                onTestTts = { container.ttsManager.test(language) }
            )
        }
        composable(Routes.listener) {
            ListenerConfigScreen(
                rules = rules,
                language = language,
                diagnostic = diagnostic,
                onBack = navController::popBackStack,
                onToggleRule = { id, enabled ->
                    scope.launch { container.settingsStore.setRuleEnabled(id, enabled) }
                },
                onPackageChange = { id, packageName ->
                    scope.launch { container.settingsStore.setRulePackage(id, packageName) }
                },
                onKeywordsChange = { id, keywords ->
                    scope.launch { container.settingsStore.setRuleKeywords(id, keywords) }
                },
                onTestTts = { container.ttsManager.test(language) }
            )
        }
        composable(Routes.license) {
            LicenseScreen(
                license = license,
                deviceId = container.licenseRepository.deviceId(),
                onBack = navController::popBackStack,
                onActivate = { key, serverUrl -> container.licenseRepository.activate(key, serverUrl) },
                onCheck = { container.licenseRepository.checkNow() },
                onServerUrlChange = { scope.launch { container.settingsStore.setServerUrl(it) } }
            )
        }
    }
}

@Composable
private fun SelectedPackageRow(
    packageName: String,
    label: String?,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DingLine, RoundedCornerShape(8.dp))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label ?: packageName, color = DingInk, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (label != null) {
                Text(packageName, color = DingInkMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = DingInkMuted)
        }
    }
}

@Composable
private fun InstalledAppPickerDialog(
    apps: List<InstalledAppInfo>,
    selectedPackages: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query, selectedPackages) {
        apps
            .filterNot { it.packageName in selectedPackages }
            .filter {
                query.isBlank() ||
                    it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择已安装 App") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("搜索 App 或包名") },
                    singleLine = true,
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredApps.isEmpty()) {
                        item {
                            Text("没有可添加的应用", color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(filteredApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(app.packageName) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(app.label, color = DingInk, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.packageName, color = DingInkMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Filled.Add, contentDescription = null, tint = DingPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    rules: List<PaymentRule>,
    records: List<PaymentRecordEntity>,
    license: LicenseState,
    ttsReady: Boolean,
    onToggleRule: (String, Boolean) -> Unit,
    onRecords: () -> Unit,
    onRuleVoice: () -> Unit,
    onSettings: () -> Unit,
    onLicense: () -> Unit
) {
    val context = LocalContext.current
    val listenerEnabled = SystemStatus.isNotificationListenerEnabled(context)
    val enabledCount = rules.count { it.enabled }

    Scaffold(
        containerColor = DingBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("DingPay", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "系统设置")
                    }
                },
                colors = topBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            item {
                DashboardHero(
                    total = formatMoney(records.sumOf { it.amount }),
                    count = records.size,
                    licenseActive = license.isActive,
                    listenerEnabled = listenerEnabled,
                    onRecords = onRecords,
                    onLicense = onLicense
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatusTile("监听", if (listenerEnabled) "运行中" else "未开启", listenerEnabled, Icons.Filled.Notifications, Modifier.weight(1f))
                    StatusTile("来源", "$enabledCount/${rules.size}", enabledCount > 0, Icons.Filled.Tune, Modifier.weight(1f))
                    StatusTile("语音", if (ttsReady) "可用" else "准备中", ttsReady, Icons.AutoMirrored.Filled.VolumeUp, Modifier.weight(1f))
                }
            }
            item { SectionTitle("收款来源", "开启后自动监听并播报") }
            items(rules, key = { it.id }) { rule ->
                SourceControlCard(
                    rule = rule,
                    onToggleRule = onToggleRule,
                    onRecords = onRecords,
                    onVoice = onRuleVoice
                )
            }
            item { SectionTitle("快捷入口", "处理授权、权限和播报设置") }
            item {
                ActionListCard {
                    ActionRow(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "收款记录",
                        subtitle = "查看统计、明细和通知文本摘要",
                        onClick = onRecords
                    )
                    HorizontalDivider(color = DingLine)
                    ActionRow(
                        icon = Icons.Filled.Shield,
                        title = "授权激活",
                        subtitle = when {
                            license.isPermanent -> "已授权，永久有效"
                            license.isActive -> "已授权，剩余 ${license.remainingDays} 天"
                            else -> "未授权或已过期"
                        },
                        onClick = onLicense
                    )
                    HorizontalDivider(color = DingLine)
                    ActionRow(
                        icon = Icons.Filled.Settings,
                        title = "系统设置",
                        subtitle = "语言、TTS 测试、监听与关于",
                        onClick = onSettings
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DashboardHero(
    total: String,
    count: Int,
    licenseActive: Boolean,
    listenerEnabled: Boolean,
    onRecords: () -> Unit,
    onLicense: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(DingPrimaryDark, DingPrimary, Color(0xFF2C8B72), DingSecondary)
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(
                        text = if (licenseActive) "授权有效" else "需要授权",
                        ok = licenseActive
                    )
                    Spacer(Modifier.weight(1f))
                    StatusBadge(
                        text = if (listenerEnabled) "监听已开启" else "监听未开启",
                        ok = listenerEnabled
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("今日最近收款", color = Color(0xFFEAF8F1), style = MaterialTheme.typography.bodyMedium)
                    Text(total, color = Color.White, style = MaterialTheme.typography.headlineLarge)
                    Text("最近记录 $count 条，通知内容仅在本机处理", color = Color(0xDDEAF8F1), style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRecords,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DingPrimaryDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("看记录")
                    }
                    OutlinedButton(
                        onClick = onLicense,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xAAFFFFFF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("授权")
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceControlCard(
    rule: PaymentRule,
    onToggleRule: (String, Boolean) -> Unit,
    onRecords: () -> Unit,
    onVoice: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DingSurface),
        border = BorderStroke(1.dp, DingLine)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = sourceIcon(rule.sourceType), tint = sourceTint(rule.sourceType), background = sourceSoftColor(rule.sourceType))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(rule.title, style = MaterialTheme.typography.titleMedium, color = DingInk)
                    Text(rule.description, color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = rule.enabled, onCheckedChange = { onToggleRule(rule.id, it) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRecords,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("记录")
                }
                Button(
                    onClick = onVoice,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("配置")
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordsScreen(
    records: List<PaymentRecordEntity>,
    onBack: () -> Unit,
    onClear: () -> Unit
) {
    Scaffold(
        containerColor = DingBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("收款记录", fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = topBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            item {
                SummaryCard(
                    title = "记录总览",
                    metrics = listOf(
                        "总笔数" to records.size.toString(),
                        "总金额" to formatMoney(records.sumOf { it.amount })
                    )
                )
            }
            item { SectionTitle("明细", "共 ${records.size} 条") }
            if (records.isEmpty()) {
                item { EmptyState("还没有收款记录", "开启监听后，匹配到的到账通知会出现在这里。") }
            } else {
                items(records, key = { it.id }) { record -> PaymentRecordRow(record) }
                item {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("清空记录")
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SummaryCard(title: String, metrics: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DingPrimaryDark)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                metrics.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x18FFFFFF))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(label, color = Color(0xCCEAF8F1), style = MaterialTheme.typography.bodyMedium)
                        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentRecordRow(record: PaymentRecordEntity) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DingSurface),
        border = BorderStroke(1.dp, DingLine)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = sourceIcon(record.sourceType),
                tint = DingPrimary,
                background = DingPrimarySoft
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(record.sourceName, fontWeight = FontWeight.Bold, color = DingInk)
                Text(formatTime(record.postedAt), color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
                if (record.text.isNotBlank()) {
                    Text(record.text, maxLines = 1, overflow = TextOverflow.Ellipsis, color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("+${record.currency}${"%.2f".format(Locale.US, record.amount)}", color = DingSuccess, fontWeight = FontWeight.ExtraBold)
                Text("成功", color = DingSuccess, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    language: BroadcastLanguage,
    license: LicenseState,
    onBack: () -> Unit,
    onLanguage: (BroadcastLanguage) -> Unit,
    onListener: () -> Unit,
    onLicense: () -> Unit,
    onTestTts: () -> Unit
) {
    var showAbout by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = DingBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("系统设置", fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = topBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            item {
                ActionListCard {
                    ActionRow(Icons.Filled.Notifications, "监听配置", "通知监听、后台保活和来源规则", onListener)
                    HorizontalDivider(color = DingLine)
                    ActionRow(
                        icon = Icons.Filled.Shield,
                        title = "授权激活",
                        subtitle = when {
                            license.isPermanent -> "已授权，永久有效"
                            license.isActive -> "已授权，到期剩余 ${license.remainingDays} 天"
                            else -> "未激活或已过期"
                        },
                        onClick = onLicense
                    )
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DingSurface),
                    border = BorderStroke(1.dp, DingLine)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.Translate, DingPrimary, DingPrimarySoft)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("播报语言", style = MaterialTheme.typography.titleMedium)
                                Text("选择中文或西班牙语 TTS 模板", color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LanguageButton("中文", language == BroadcastLanguage.ZH, Modifier.weight(1f)) {
                                onLanguage(BroadcastLanguage.ZH)
                            }
                            LanguageButton("西语", language == BroadcastLanguage.ES, Modifier.weight(1f)) {
                                onLanguage(BroadcastLanguage.ES)
                            }
                            OutlinedButton(onClick = onTestTts, shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("测试")
                            }
                        }
                    }
                }
            }
            item {
                ActionListCard {
                    ActionRow(Icons.Filled.CreditCard, "关于 DingPay", "隐私边界与使用风险说明") { showAbout = true }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("知道了") } },
            title = { Text("关于 DingPay") },
            text = {
                Text("本软件通过监听系统通知读取到账提醒并在本机语音播报。通知内容仅在本机处理，不上传、不存储到后台。因第三方 App 未推送通知、系统后台限制、TTS 缺失等造成延迟或漏播，需由使用者自行确认风险。")
            }
        )
    }
}

@Composable
private fun LanguageButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val buttonModifier = modifier.height(44.dp)
    if (selected) {
        Button(onClick = onClick, modifier = buttonModifier, shape = RoundedCornerShape(8.dp)) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, maxLines = 1, overflow = TextOverflow.Clip)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = buttonModifier, shape = RoundedCornerShape(8.dp)) {
            Text(text, maxLines = 1, overflow = TextOverflow.Clip)
        }
    }
}

private data class InstalledAppInfo(
    val label: String,
    val packageName: String
)

private fun loadInstalledApps(context: Context): List<InstalledAppInfo> {
    val packageManager = context.packageManager
    @Suppress("DEPRECATION")
    val launcherApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.ResolveInfoFlags.of(0)
        )
    } else {
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0
        )
    }
    return launcherApps
        .asSequence()
        .mapNotNull { resolveInfo -> resolveInfo.activityInfo?.applicationInfo }
        .map { appInfo ->
            InstalledAppInfo(
                label = appInfo.loadLabel(packageManager).toString(),
                packageName = appInfo.packageName
            )
        }
        .distinctBy { it.packageName }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        .toList()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListenerConfigScreen(
    rules: List<PaymentRule>,
    language: BroadcastLanguage,
    diagnostic: NotificationDiagnostic,
    onBack: () -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onPackageChange: (String, String) -> Unit,
    onKeywordsChange: (String, List<String>) -> Unit,
    onTestTts: () -> Unit
) {
    val context = LocalContext.current
    val listenerEnabled = SystemStatus.isNotificationListenerEnabled(context)
    val batteryOk = SystemStatus.isIgnoringBatteryOptimizations(context)
    val alarmAudible = SystemStatus.isAlarmVolumeAudible(context)
    var installedApps by remember { mutableStateOf(emptyList<InstalledAppInfo>()) }
    var reconnectMessage by remember { mutableStateOf("") }
    var keepAliveMessage by remember { mutableStateOf("") }
    LaunchedEffect(listenerEnabled) {
        if (listenerEnabled) {
            SystemStatus.requestNotificationListenerRebind(context)
        }
    }
    LaunchedEffect(Unit) {
        installedApps = loadInstalledApps(context)
    }
    Scaffold(
        containerColor = DingBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("监听配置", fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = topBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            item { DiagnosticCard(diagnostic) }
            item {
                InfoCard(
                    title = "系统说明",
                    lines = listOf(
                        "监听配置已预设，可按实际手机修改包名",
                        "请开启通知监听权限以接收收款通知",
                        "建议加入电池白名单以保持后台运行",
                        "国产手机请在厂商设置里开启自启动和后台无限制"
                    )
                )
            }
            item {
                StatusActionCard(
                    title = "通知栏监听",
                    status = if (listenerEnabled) "已开启" else "未开启",
                    ok = listenerEnabled,
                    icon = Icons.Filled.Notifications,
                    action = "打开设置",
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
            item {
                StatusActionCard(
                    title = "监听重连",
                    status = reconnectMessage.ifBlank {
                        if (listenerEnabled) "长时间收不到通知时可强制重连" else "请先开启通知监听权限"
                    },
                    ok = listenerEnabled,
                    icon = Icons.Filled.Refresh,
                    action = "强制重连",
                    onAction = {
                        runCatching { SystemStatus.forceReconnectNotificationListener(context) }
                        reconnectMessage = "已发起重连，请稍后观察上方通知处理状态"
                    }
                )
            }
            item {
                StatusActionCard(
                    title = "后台保活",
                    status = if (batteryOk) "已在白名单" else "建议设置",
                    ok = batteryOk,
                    icon = Icons.Filled.Shield,
                    action = "电池设置",
                    onAction = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(intent) }
                    }
                )
            }
            item {
                StatusActionCard(
                    title = "厂商后台保活",
                    status = keepAliveMessage.ifBlank { "请开启自启动，并把后台耗电设为无限制" },
                    ok = batteryOk,
                    icon = Icons.Filled.Settings,
                    action = "去开启",
                    onAction = {
                        val openedOemPage = OemKeepAliveHelper.openKeepAliveSettings(context)
                        keepAliveMessage = if (openedOemPage) {
                            "已打开厂商后台管理页，请允许自启动和后台无限制"
                        } else {
                            "未找到厂商专属页面，已打开应用详情，请手动开启自启动和后台无限制"
                        }
                    }
                )
            }
            item {
                StatusActionCard(
                    title = "闹钟音量",
                    status = if (alarmAudible) "正常" else "闹钟音量为 0，语音播报将听不到",
                    ok = alarmAudible,
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    action = "声音设置",
                    onAction = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }
                    }
                )
            }
            item {
                StatusActionCard(
                    title = "TTS 状态",
                    status = if (language == BroadcastLanguage.ZH) "中文播报" else "Español",
                    ok = true,
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    action = "测试",
                    onAction = onTestTts
                )
            }
            item { SectionTitle("监听来源", "预设规则可开关或修改包名") }
            items(rules, key = { it.id }) { rule ->
                RuleConfigCard(
                    rule = rule,
                    installedApps = installedApps,
                    onToggle = { onToggleRule(rule.id, it) },
                    onPackageChange = { onPackageChange(rule.id, it) },
                    onKeywordsChange = { onKeywordsChange(rule.id, it) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RuleConfigCard(
    rule: PaymentRule,
    installedApps: List<InstalledAppInfo>,
    onToggle: (Boolean) -> Unit,
    onPackageChange: (String) -> Unit,
    onKeywordsChange: (List<String>) -> Unit
) {
    var showAppPicker by remember { mutableStateOf(false) }
    var showManualPackage by remember { mutableStateOf(false) }
    var manualPackage by remember { mutableStateOf("") }
    var keywordText by remember(rule.id, rule.keywords) {
        mutableStateOf(joinRuleValues(rule.keywords))
    }
    val selectedPackages = rule.packageNames

    if (showAppPicker) {
        InstalledAppPickerDialog(
            apps = installedApps,
            selectedPackages = selectedPackages,
            onDismiss = { showAppPicker = false },
            onSelect = { packageName ->
                onPackageChange(joinRuleValues(selectedPackages + packageName))
                showAppPicker = false
            }
        )
    }

    if (showManualPackage) {
        AlertDialog(
            onDismissRequest = { showManualPackage = false },
            title = { Text("手动添加包名") },
            text = {
                OutlinedTextField(
                    value = manualPackage,
                    onValueChange = { manualPackage = it },
                    label = { Text("应用包名") },
                    singleLine = true,
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val packageName = manualPackage.trim()
                        if (packageName.isNotBlank()) {
                            onPackageChange(joinRuleValues(selectedPackages + packageName))
                        }
                        manualPackage = ""
                        showManualPackage = false
                    }
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualPackage = false }) {
                    Text("取消")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DingSurface),
        border = BorderStroke(1.dp, DingLine)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(sourceIcon(rule.sourceType), sourceTint(rule.sourceType), sourceSoftColor(rule.sourceType))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(rule.title, style = MaterialTheme.typography.titleMedium)
                    Text(rule.description, color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("监听 App", color = DingInk, style = MaterialTheme.typography.titleSmall)
                if (selectedPackages.isEmpty()) {
                    Text("未选择 App 时不限制包名，只按关键词匹配。", color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
                } else {
                    selectedPackages.forEach { packageName ->
                        SelectedPackageRow(
                            packageName = packageName,
                            label = installedApps.firstOrNull { it.packageName == packageName }?.label,
                            onRemove = {
                                onPackageChange(joinRuleValues(selectedPackages - packageName))
                            }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showAppPicker = true }, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("添加 App")
                    }
                    TextButton(onClick = { showManualPackage = true }) {
                        Text("手动包名")
                    }
                }
            }
            OutlinedTextField(
                value = keywordText,
                onValueChange = { keywordText = it },
                label = { Text("关键词") },
                minLines = 2,
                colors = textFieldColors(),
                supportingText = { Text("多个关键词用逗号、换行或分号分隔；通知包含任意关键词就会继续解析金额。") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onKeywordsChange(splitRuleValues(keywordText)) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存关键词")
            }
            OutlinedTextField(
                value = rule.packageName,
                onValueChange = onPackageChange,
                label = { Text("应用包名") },
                singleLine = true,
                colors = textFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Text("关键词：${rule.keywords.joinToString("、")}", color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseScreen(
    license: LicenseState,
    deviceId: String,
    onBack: () -> Unit,
    onActivate: suspend (String, String) -> LicenseState,
    onCheck: suspend () -> LicenseState,
    onServerUrlChange: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var licenseKey by remember(license.licenseKey) { mutableStateOf(license.licenseKey) }
    var serverUrl by remember(license.serverUrl) { mutableStateOf(license.serverUrl) }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = DingBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("授权激活", fontWeight = FontWeight.Bold) },
                navigationIcon = { BackButton(onBack) },
                colors = topBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(2.dp)) }
            item {
                LicenseStatusCard(license)
            }
            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DingSurface),
                    border = BorderStroke(1.dp, DingLine)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.CreditCard, DingPrimary, DingPrimarySoft)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("设备信息", style = MaterialTheme.typography.titleMedium)
                                Text(deviceId, color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DingSurface),
                    border = BorderStroke(1.dp, DingLine)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = {
                                serverUrl = it
                                onServerUrlChange(it)
                            },
                            label = { Text("授权后台地址") },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = licenseKey,
                            onValueChange = { licenseKey = it },
                            label = { Text("授权码") },
                            minLines = 3,
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                enabled = !loading,
                                onClick = {
                                    scope.launch {
                                        loading = true
                                        message = runCatching { onActivate(licenseKey, serverUrl) }
                                            .fold(
                                                onSuccess = { "激活成功" },
                                                onFailure = { "激活失败：${it.message.orEmpty()}" }
                                            )
                                        loading = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (loading) "处理中" else "激活")
                            }
                            OutlinedButton(
                                enabled = !loading,
                                onClick = {
                                    scope.launch {
                                        loading = true
                                        message = runCatching { onCheck() }
                                            .fold(
                                                onSuccess = { "校验完成：${it.status}" },
                                                onFailure = { "校验失败：${it.message.orEmpty()}" }
                                            )
                                        loading = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("校验")
                            }
                        }
                        if (message.isNotBlank()) {
                            Surface(
                                color = DingPrimarySoft,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(message, color = DingPrimaryDark, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LicenseStatusCard(license: LicenseState) {
    val active = license.isActive
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) DingPrimaryDark else DingAccentSoft)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (active) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (active) Color.White else DingAccent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(if (active) "已授权" else "未授权", color = if (active) Color.White else DingInk, style = MaterialTheme.typography.titleLarge)
                    Text(
                        when {
                            license.isPermanent -> "永久有效"
                            active -> "剩余 ${license.remainingDays} 天"
                            else -> "请输入授权码激活"
                        },
                        color = if (active) Color(0xDDEAF8F1) else DingInkMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (license.expiresAtMillis > 0L) {
                Text(
                    "到期时间：${formatTime(license.expiresAtMillis)}",
                    color = if (active) Color(0xDDEAF8F1) else DingInkMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ActionListCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DingSurface),
        border = BorderStroke(1.dp, DingLine)
    ) {
        Column(content = content)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, tint = DingPrimary, background = DingPrimarySoft)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = DingInk, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DingInkMuted)
    }
}

@Composable
private fun StatusTile(title: String, value: String, ok: Boolean, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DingSurface),
        border = BorderStroke(1.dp, DingLine)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = if (ok) DingSuccess else DingWarning, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = DingInkMuted)
            Text(value, color = if (ok) DingSuccess else DingWarning, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatusBadge(text: String, ok: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (ok) Color(0x2AFFFFFF) else Color(0xFFFFE4DF))
            .border(1.dp, if (ok) Color(0x55FFFFFF) else Color(0x44E85D4F), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (ok) Color.White else DingAccent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text, color = if (ok) Color.White else DingAccent, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = DingInk, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InfoCard(title: String, lines: List<String>) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DingSecondarySoft),
        border = BorderStroke(1.dp, Color(0xFFE9C56C))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = DingInk, style = MaterialTheme.typography.titleMedium)
            lines.forEach { Text("• $it", color = DingInkMuted, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DingSurface),
        border = BorderStroke(1.dp, DingLine)
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = DingPrimary, modifier = Modifier.size(34.dp))
            Text(title, color = DingInk, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = DingInkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DiagnosticCard(diagnostic: NotificationDiagnostic) {
    val clipboard = LocalClipboardManager.current
    val hasDiagnostic = diagnostic.updatedAtMillis > 0L
    val ok = diagnostic.stage == NotificationDiagnostic.STAGE_BROADCAST ||
        diagnostic.stage == NotificationDiagnostic.STAGE_TTS_QUEUED
    val stageLabel = diagnosticStageLabel(diagnostic.stage)
    val containerColor = if (ok) DingPrimarySoft else DingSecondarySoft
    val borderColor = if (ok) DingPrimary else if (hasDiagnostic) Color(0xFFE9C56C) else DingLine
    val statusColor = if (ok) DingSuccess else if (hasDiagnostic) DingWarning else DingInkMuted
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(
                    icon = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    tint = statusColor,
                    background = Color(0x22FFFFFF)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("最近一条通知处理", style = MaterialTheme.typography.titleMedium, color = DingInk)
                    Text(
                        if (hasDiagnostic) formatTime(diagnostic.updatedAtMillis) else "等待新通知",
                        color = DingInkMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                stageLabel,
                modifier = Modifier.fillMaxWidth(),
                color = statusColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!hasDiagnostic) {
                Text(
                    "收到新通知后，这里会显示包名、匹配结果和未播报原因。",
                    color = DingInkMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (diagnostic.packageName.isNotBlank()) {
                Text(
                    "包名：${diagnostic.packageName}",
                    color = DingInk,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (diagnostic.rawTextPreview.isNotBlank()) {
                Text(
                    diagnostic.rawTextPreview,
                    color = DingInkMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!ok && diagnostic.packageName.isNotBlank()) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(diagnostic.packageName)) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("复制包名")
                }
            }
        }
    }
}

private fun diagnosticStageLabel(stage: String): String = when (stage) {
    NotificationDiagnostic.STAGE_LICENSE_INACTIVE -> "授权未激活拦截"
    NotificationDiagnostic.STAGE_NO_RULE_MATCH -> "没有规则匹配 / 包名或关键词不符"
    NotificationDiagnostic.STAGE_NO_AMOUNT -> "通知里没解析出金额"
    NotificationDiagnostic.STAGE_DUPLICATE -> "重复通知已忽略"
    NotificationDiagnostic.STAGE_BROADCAST -> "已播报"
    NotificationDiagnostic.STAGE_TTS_QUEUED -> "TTS 已排队"
    NotificationDiagnostic.STAGE_TTS_FAILED -> "TTS 播报失败"
    else -> stage.ifBlank { "等待通知" }
}

@Composable
private fun StatusActionCard(
    title: String,
    status: String,
    ok: Boolean,
    icon: ImageVector,
    action: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DingSurface),
        border = BorderStroke(1.dp, DingLine)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = icon, tint = if (ok) DingSuccess else DingWarning, background = if (ok) DingPrimarySoft else DingSecondarySoft)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = DingInk, style = MaterialTheme.typography.titleMedium)
                Text(status, color = if (ok) DingSuccess else DingWarning, style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onAction, shape = RoundedCornerShape(8.dp)) { Text(action) }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color, background: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun topBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = DingBackground,
    titleContentColor = DingInk,
    navigationIconContentColor = DingInk,
    actionIconContentColor = DingInk
)

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DingPrimary,
    focusedLabelColor = DingPrimary,
    cursorColor = DingPrimary
)

private fun sourceIcon(sourceType: PaymentSourceType): ImageVector = when (sourceType) {
    PaymentSourceType.BANK -> Icons.Filled.AccountBalance
    PaymentSourceType.YAPPY -> Icons.Filled.CreditCard
    PaymentSourceType.EMAIL -> Icons.Filled.Email
}

private fun sourceIcon(sourceType: String): ImageVector = when (sourceType) {
    PaymentSourceType.BANK.name -> Icons.Filled.AccountBalance
    PaymentSourceType.YAPPY.name -> Icons.Filled.CreditCard
    PaymentSourceType.EMAIL.name -> Icons.Filled.Email
    else -> Icons.AutoMirrored.Filled.ReceiptLong
}

private fun sourceTint(sourceType: PaymentSourceType): Color = when (sourceType) {
    PaymentSourceType.BANK -> DingPrimary
    PaymentSourceType.YAPPY -> DingAccent
    PaymentSourceType.EMAIL -> DingWarning
}

private fun sourceSoftColor(sourceType: PaymentSourceType): Color = when (sourceType) {
    PaymentSourceType.BANK -> DingPrimarySoft
    PaymentSourceType.YAPPY -> DingAccentSoft
    PaymentSourceType.EMAIL -> DingSecondarySoft
}

private fun formatMoney(value: Double): String = "$${String.format(Locale.US, "%.2f", value)}"

private fun formatTime(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timeMillis))
}
