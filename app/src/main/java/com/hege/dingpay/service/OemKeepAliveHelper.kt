package com.hege.dingpay.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.Locale

/**
 * 国产 ROM 后台保活引导：按厂商跳转自启动 / 后台管理页面。
 * 所有厂商页面都打不开时，回落到本 app 的应用详情设置页。
 */
object OemKeepAliveHelper {
    private data class OemEntry(
        val manufacturers: Set<String>,
        val components: List<ComponentName>
    )

    /** 常见厂商的自启动 / 后台管理页面组件清单，按命中顺序尝试。 */
    private val oemEntries = listOf(
        OemEntry(
            manufacturers = setOf("xiaomi", "redmi"),
            components = listOf(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                ),
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.powercenter.PowerSettings"
                )
            )
        ),
        OemEntry(
            manufacturers = setOf("huawei", "honor"),
            components = listOf(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                ),
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                ),
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                ),
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"
                )
            )
        ),
        OemEntry(
            manufacturers = setOf("oppo", "realme", "oneplus"),
            components = listOf(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                ),
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                ),
                ComponentName(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"
                ),
                ComponentName(
                    "com.oplus.safecenter",
                    "com.oplus.safecenter.permission.startup.StartupAppListActivity"
                ),
                ComponentName(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                ),
                ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            )
        ),
        OemEntry(
            manufacturers = setOf("vivo", "iqoo"),
            components = listOf(
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                ),
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                ),
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                ),
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.PurviewTabActivity"
                ),
                ComponentName(
                    "com.vivo.abe",
                    "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"
                )
            )
        ),
        OemEntry(
            manufacturers = setOf("samsung"),
            components = listOf(
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.battery.ui.BatteryActivity"
                ),
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            )
        ),
        OemEntry(
            manufacturers = setOf("meizu"),
            components = listOf(
                ComponentName(
                    "com.meizu.safe",
                    "com.meizu.safe.permission.SmartBGActivity"
                ),
                ComponentName(
                    "com.meizu.safe",
                    "com.meizu.safe.permission.PermissionMainActivity"
                )
            )
        )
    )

    /**
     * 尝试打开当前厂商的自启动 / 后台管理页面。
     * @return true 表示打开了厂商页面；false 表示全部失败并已回落到应用详情设置页。
     */
    fun openKeepAliveSettings(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim().lowercase(Locale.US)
        val components = oemEntries
            .firstOrNull { entry -> entry.manufacturers.any { manufacturer.contains(it) } }
            ?.components
            .orEmpty()
        for (component in components) {
            val intent = Intent()
                .setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val launched = runCatching { context.startActivity(intent) }
                .onFailure { error ->
                    Log.d(TAG, "OEM keep-alive page unavailable component=$component: ${error.message}")
                }
                .isSuccess
            if (launched) {
                Log.d(TAG, "Opened OEM keep-alive page component=$component")
                return true
            }
        }
        openAppDetailsSettings(context)
        return false
    }

    private fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { error ->
                Log.w(TAG, "Failed to open app details settings", error)
            }
    }

    private const val TAG = "DingPayKeepAlive"
}
