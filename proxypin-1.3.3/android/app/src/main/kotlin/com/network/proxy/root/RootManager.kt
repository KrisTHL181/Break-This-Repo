package com.network.proxy.root

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Root 辅助工具：为已 root 设备提供"一键解决无网络抓包"的能力。
 *
 * 无网络问题的两个根因（抓 HTTPS 时）：
 *  1. 很多 App（targetSdk>=24）默认只信任"系统证书"，不信任用户安装的 CA，
 *     导致通过 ProxyPin 的 MITM 后报"无网络/证书无效"。
 *     → 本类通过 root 把 ProxyPin 的 CA 直接装入系统证书库（[installSystemCa]）。
 *  2. 部分 App 使用 SSL Pinning 固定证书，即使装了系统 CA 也会校验失败。
 *     → 需要 Xposed/LSPosed 模块（TrustMeAlready / 算法助手 等）绕过，
 *       本类提供 [detectPinningModules] 检测并引导用户启用。
 *
 * @author ProxyBird
 */
class RootManager(private val context: Context) {

    companion object {
        private const val TAG = "RootManager"

        /** 已知的 SSL Pinning 绕过模块包名（启发式，可继续补充） */
        private val KNOWN_PINNING_MODULES = setOf(
            "org.lsposed.manager",               // LSPosed 管理器
            "de.robv.android.xposed.installer",  // 老版 Xposed Installer
            "io.github.lsposed.lspd",            // LSPosed 核心
            "com.wrbug.algorithmhelper",         // 算法助手（作者 wrbug 的常见包名）
            "com.trustmealready",
            "com.TrustMeAlready",
            "org.justtrustme",
            "com.github.justtrustme",
            "com.dima.tlsdump"
        )

        /** 通过应用名关键字识别的模块（兜底，避免包名不确定导致漏检） */
        private val LABEL_KEYWORDS = listOf(
            "trustmealready", "justtrustme", "just trust", "算法助手", "justtrust",
            "tls", "证书", "抓包", "pinning", "algorithum", "algorithmhelper"
        )

        private val SU_PATHS = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/app/Superuser.apk", "/su/bin/su", "/system/bin/magisk"
        )
    }

    /**
     * 执行一条 shell 命令并返回完整输出。
     */
    private fun exec(vararg command: String): ShellResult {
        return try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            val output = readAll(process.inputStream)
            val exit = process.waitFor()
            ShellResult(exit, output)
        } catch (e: Exception) {
            Log.w(TAG, "exec failed: ${command.joinToString(" ")}", e)
            ShellResult(-1, e.message ?: "exec failed")
        }
    }

    private fun readAll(input: InputStream): String {
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var len: Int
        while (input.read(buffer).also { len = it } != -1) {
            bos.write(buffer, 0, len)
        }
        return String(bos.toByteArray(), Charsets.UTF_8)
    }

    /**
     * 用 su 提权执行命令，返回 stdout/stderr 合并结果。
     */
    private fun su(command: String): ShellResult {
        return exec("su", "-c", command)
    }

    /**
     * 检测设备是否拥有可用的 root（su）。
     */
    fun isRooted(): Boolean {
        // 1. 常规 su 路径存在
        for (path in SU_PATHS) {
            if (File(path).exists()) {
                Log.i(TAG, "found su: $path")
                return true
            }
        }
        // 2. 直接尝试执行 su，确认能拿到 root 权限
        val result = su("id")
        val ok = result.exitCode == 0 && result.output.contains("uid=0")
        Log.i(TAG, "su id -> exit=${result.exitCode}, out=${result.output.trim()}")
        return ok
    }

    /**
     * 读取 ProxyPin 自身 CA 证书（来自 assets，初次运行后写入应用私有目录）。
     * 失败时返回 null。
     */
    fun readCaPemFromAssets(): String? {
        return try {
            context.assets.open("certs/ca.crt").use { readAll(it) }
        } catch (e: Exception) {
            Log.w(TAG, "read assets ca.crt failed", e)
            null
        }
    }

    /**
     * 把 CA 证书写入应用私有目录，返回文件路径。
     */
    private fun writeCaToFile(certPem: String): File? {
        return try {
            val dir = File(context.filesDir, "root_helper")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "proxypin-ca.crt")
            FileOutputStream(file).use { it.write(certPem.toByteArray(Charsets.UTF_8)) }
            file
        } catch (e: Exception) {
            Log.w(TAG, "write ca file failed", e)
            null
        }
    }

    /**
     * 当前系统证书库目录。
     *  - Android <= 13：/system/etc/security/cacerts
     *  - Android 14+：/apex/com.android.conscrypt/cacerts（需 Magisk overlay 或 remount）
     */
    private fun cacertsDir(): String {
        return if (Build.VERSION.SDK_INT >= 34) {
            "/apex/com.android.conscrypt/cacerts"
        } else {
            "/system/etc/security/cacerts"
        }
    }

    /**
     * 一键把 ProxyPin CA 安装为系统证书（需要 root）。
     *
     * @param certPem CA 证书 PEM 内容
     * @param hashName 系统证书文件名，形如 "aabbccdd.0"（由 Dart 端 [CertificateManager.systemCertificateName] 提供）
     * @return 结构化结果：success, message, needReboot
     */
    fun installSystemCa(certPem: String, hashName: String): Map<String, Any> {
        if (!isRooted()) {
            return resultOf(false, "设备未检测到 root 权限，无法自动安装系统证书。请先确认已 root，或改用 Magisk 模块。", needReboot = false)
        }

        val certFile = writeCaToFile(certPem) ?: return resultOf(false, "写入证书临时文件失败", needReboot = false)
        val hash = hashName.removeSuffix(".0")
        if (hash.isBlank()) {
            return resultOf(false, "证书哈希文件名无效：$hashName", needReboot = false)
        }
        val src = certFile.absolutePath
        val cacerts = cacertsDir()
        val target = "$cacerts/$hashName"

        // Android 14+：先尝试 remount APEX（部分设备可行），失败再走系统分区
        val remountTarget = if (Build.VERSION.SDK_INT >= 34) {
            "mount -o rw,remount /apex/com.android.conscrypt 2>/dev/null;"
        } else {
            "mount -o rw,remount /system 2>/dev/null; mount -o rw,remount / 2>/dev/null;"
        }

        val command = buildString {
            append("set -e; ")
            append(remountTarget)
            append("cp \"$src\" \"$target\" && chmod 644 \"$target\" && ")
            append("echo OK_INSTALLED")
        }

        val result = su(command)
        val success = result.exitCode == 0 && result.output.contains("OK_INSTALLED")

        if (success) {
            return resultOf(
                true,
                "系统证书安装成功（$target）。部分 App 需要重启或重新打开才能生效。",
                needReboot = Build.VERSION.SDK_INT >= 34
            )
        }

        // 直接安装失败：尝试通过 Magisk 模块方式（Android 14+ 推荐）
        val magisk = tryInstallViaMagisk(certPem, hashName)
        if (magisk["success"] == true) {
            return resultOf(true, (magisk["message"] as String), needReboot = true)
        }

        return resultOf(
            false,
            "安装失败：${result.output.trim().take(200)}。建议使用 Magisk 模块方式安装（见输出/引导）。",
            needReboot = false
        )
    }

    /**
     * 生成并部署一个 Magisk 模块，把 ProxyPin CA 作为系统证书安装。
     * 部署后需要重启设备生效。
     *
     * @param certPem CA 证书 PEM
     * @param hashName 系统证书文件名
     * @return 结构化结果
     */
    fun installViaMagisk(certPem: String, hashName: String): Map<String, Any> {
        val certFile = writeCaToFile(certPem) ?: return resultOf(false, "写入证书临时文件失败", false)
        return tryInstallViaMagisk(certPem, hashName)
    }

    private fun tryInstallViaMagisk(certPem: String, hashName: String): Map<String, Any> {
        return try {
            // Magisk 模块目录（root 后 /data 可写）
            val moduleDir = "/data/adb/modules/proxypin-system-ca"
            val certFile = writeCaToFile(certPem) ?: return resultOf(false, "写入证书临时文件失败", false)
            val hash = hashName.removeSuffix(".0")

            // 模块结构：
            //   /data/adb/modules/proxypin-system-ca/
            //     module.prop
            //     system/etc/security/cacerts/<hash>.0
            val contentDir = "$moduleDir/system/etc/security/cacerts"
            val commands = buildString {
                append("set -e; ")
                append("mkdir -p \"$contentDir\"; ")
                append("cp \"${certFile.absolutePath}\" \"$contentDir/$hashName\"; ")
                append("chmod 644 \"$contentDir/$hashName\"; ")
                append("echo 'id=proxypin-system-ca' > \"$moduleDir/module.prop\"; ")
                append("echo 'name=ProxyPin System CA' >> \"$moduleDir/module.prop\"; ")
                append("echo 'version=1.0' >> \"$moduleDir/module.prop\"; ")
                append("echo 'versionCode=1' >> \"$moduleDir/module.prop\"; ")
                append("echo 'author=ProxyBird' >> \"$moduleDir/module.prop\"; ")
                append("echo 'description=Auto-install ProxyPin CA as system cert' >> \"$moduleDir/module.prop\"; ")
                append("echo OK_MAGISK")
            }
            val result = su(commands)
            val success = result.exitCode == 0 && result.output.contains("OK_MAGISK")
            if (success) {
                resultOf(true, "已生成 Magisk 模块，请重启设备后生效。", needReboot = true)
            } else {
                resultOf(false, "Magisk 模块部署失败：${result.output.trim().take(200)}", false)
            }
        } catch (e: Exception) {
            resultOf(false, "Magisk 模块部署异常：${e.message}", false)
        }
    }

    /**
     * 把 CA 打包成标准 Magisk 模块 zip（可手动安装到 Magisk），保存到应用下载/文件目录。
     * 不要求 root，方便在未 root 或安装失败时兜底。
     *
     * @return 保存路径
     */
    fun exportMagiskModuleZip(certPem: String, hashName: String): String? {
        return try {
            val hash = hashName.removeSuffix(".0")
            val outDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "magisk_module")
            if (!outDir.exists()) outDir.mkdirs()
            val zipFile = File(outDir, "proxypin-system-ca.zip")

            val temp = File(context.filesDir, "proxypin-ca.crt")
            FileOutputStream(temp).use { it.write(certPem.toByteArray(Charsets.UTF_8)) }

            zipFile.outputStream().use { zos ->
                java.util.zip.ZipOutputStream(zos).use { zip ->
                    fun addEntry(path: String, bytes: ByteArray) {
                        zip.putNextEntry(java.util.zip.ZipEntry(path))
                        zip.write(bytes)
                        zip.closeEntry()
                    }
                    addEntry("module.prop", """
                        id=proxypin-system-ca
                        name=ProxyPin System CA
                        version=1.0
                        versionCode=1
                        author=ProxyBird
                        description=Auto-install ProxyPin CA as system cert
                    """.trimIndent().toByteArray(Charsets.UTF_8))
                    addEntry("system/etc/security/cacerts/$hashName", temp.readBytes())
                    addEntry(
                        "post-fs-data.sh",
                        "#!/system/bin/sh\nmkdir -p \$MODDIR/system/etc/security/cacerts\ncp -f $temp  \${MODDIR}/system/etc/security/cacerts/$hashName\n".toByteArray()
                    )
                }
            }
            zipFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "export magisk module zip failed", e)
            null
        }
    }

    /**
     * 检测设备上已安装的 SSL Pinning 绕过模块（Xposed/LSPosed 体系）。
     *
     * @return List<Map>：package, label, type（xposed/lsposed/pinning_module）
     */
    fun detectPinningModules(): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()
        val pm = context.packageManager
        val installed = try {
            pm.getInstalledApplications(0)
        } catch (e: Exception) {
            return result
        }

        for (app in installed) {
            val pkg = app.packageName ?: continue
            val label = try {
                pm.getApplicationLabel(app).toString()
            } catch (e: Exception) {
                ""
            }

            if (pkg in KNOWN_PINNING_MODULES) {
                result.add(
                    mapOf(
                        "package" to pkg,
                        "label" to label,
                        "type" to when {
                            pkg.contains("lsposed") -> "lsposed"
                            pkg.contains("xposed") -> "xposed"
                            else -> "pinning_module"
                        }
                    )
                )
                continue
            }

            val labelLower = label.lowercase()
            if (LABEL_KEYWORDS.any { labelLower.contains(it.lowercase()) }) {
                result.add(
                    mapOf(
                        "package" to pkg,
                        "label" to label,
                        "type" to "pinning_module"
                    )
                )
            }
        }
        return result.distinctBy { it["package"] }
    }

    /**
     * 检测 LSPosed 框架是否可用（作为引导安装绕过模块的前置判断）。
     */
    fun hasLsposed(): Boolean {
        return detectPinningModules().any {
            it["type"] == "lsposed" || it["type"] == "xposed"
        }
    }

    private fun resultOf(success: Boolean, message: String, needReboot: Boolean): Map<String, Any> {
        return mapOf(
            "success" to success,
            "message" to message,
            "needReboot" to needReboot
        )
    }

    data class ShellResult(val exitCode: Int, val output: String)
}
