package com.network.proxy.plugin

import com.network.proxy.root.RootManager
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Root 辅助通道：把"自动解决无网络"的能力暴露给 Flutter。
 *
 * Channel: com.proxy/rootCert
 *
 * 方法：
 *  - isRooted(): 是否已 root
 *  - installSystemCa(certPem, hashName): 一键安装系统证书
 *  - installViaMagisk(certPem, hashName): 通过 Magisk 模块安装（需重启）
 *  - exportMagiskModuleZip(certPem, hashName): 导出可手动安装的 Magisk zip
 *  - detectPinningModules(): 检测已安装的证书绕过模块
 *  - hasLsposed(): 是否已装 LSPosed/Xposed 框架
 */
class RootCertPlugin : AndroidFlutterPlugin() {
    companion object {
        const val CHANNEL = "com.proxy/rootCert"
    }

    override fun onAttachedToEngine(binding: io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding) {
        val channel = MethodChannel(binding.binaryMessenger, CHANNEL)
        channel.setMethodCallHandler { call: MethodCall, result: MethodChannel.Result ->
            val root = RootManager(activity)
            when (call.method) {
                "isRooted" -> {
                    result.success(root.isRooted())
                }
                "installSystemCa" -> {
                    val certPem = call.argument<String>("certPem") ?: ""
                    val hashName = call.argument<String>("hashName") ?: ""
                    if (certPem.isBlank() || hashName.isBlank()) {
                        result.error("bad_args", "certPem/hashName 不能为空", null)
                        return@setMethodCallHandler
                    }
                    result.success(root.installSystemCa(certPem, hashName))
                }
                "installViaMagisk" -> {
                    val certPem = call.argument<String>("certPem") ?: ""
                    val hashName = call.argument<String>("hashName") ?: ""
                    if (certPem.isBlank() || hashName.isBlank()) {
                        result.error("bad_args", "certPem/hashName 不能为空", null)
                        return@setMethodCallHandler
                    }
                    result.success(root.installViaMagisk(certPem, hashName))
                }
                "exportMagiskModuleZip" -> {
                    val certPem = call.argument<String>("certPem") ?: ""
                    val hashName = call.argument<String>("hashName") ?: ""
                    if (certPem.isBlank() || hashName.isBlank()) {
                        result.error("bad_args", "certPem/hashName 不能为空", null)
                        return@setMethodCallHandler
                    }
                    val path = root.exportMagiskModuleZip(certPem, hashName)
                    result.success(path)
                }
                "detectPinningModules" -> {
                    result.success(root.detectPinningModules())
                }
                "hasLsposed" -> {
                    result.success(root.hasLsposed())
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    override fun onDetachedFromEngine(binding: io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding) {
    }
}
