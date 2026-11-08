package com.mcpserver.platform.adb

import android.util.Log

class AdbCommandParser {
    
    companion object {
        private const val TAG = "AdbCommandParser"
        
        private val DANGEROUS_COMMANDS = listOf(
            "rm -rf /",
            "mkfs",
            "dd if=/dev/zero",
            "dd if=/dev/random",
            "format",
            "fdisk",
            "mount -o remount",
            "chmod 777 /",
            "chown root",
            "su -c",
            "am force-stop",
            "pm uninstall -k",
            "settings delete",
            "resetprop",
            "setprop"
        )
        
        private val COMMAND_CATEGORIES = mapOf(
            "app_management" to listOf("install", "uninstall", "list", "start", "stop", "clear", "force-stop"),
            "file_operations" to listOf("pull", "push", "ls", "cat", "mkdir", "rm", "cp", "mv"),
            "device_info" to listOf("getprop", "pm list", "dumpsys", "screencap", "screenrecord"),
            "input_events" to listOf("input text", "input tap", "input swipe", "input keyevent"),
            "system_settings" to listOf("settings get", "settings put", "am start", "am broadcast", "am service"),
            "network" to listOf("ping", "ifconfig", "dumpsys connectivity")
        )
    }
    
    data class ParsedCommand(
        val originalCommand: String,
        val category: String,
        val adbCommand: String,
        val isDangerous: Boolean,
        val description: String,
        val parameters: Map<String, String>
    )
    
    fun parseCommand(input: String): ParsedCommand {
        val trimmedInput = input.trim()
        
        // 检查危险命令
        val isDangerous = checkDangerousCommand(trimmedInput)
        
        // 分类命令
        val category = categorizeCommand(trimmedInput)
        
        // 构建ADB命令
        val adbCommand = buildAdbCommand(trimmedInput)
        
        // 提取参数
        val parameters = extractParameters(trimmedInput)
        
        // 生成描述
        val description = generateDescription(trimmedInput, category)
        
        return ParsedCommand(
            originalCommand = trimmedInput,
            category = category,
            adbCommand = adbCommand,
            isDangerous = isDangerous,
            description = description,
            parameters = parameters
        )
    }
    
    private fun checkDangerousCommand(command: String): Boolean {
        val lowerCommand = command.lowercase()
        return DANGEROUS_COMMANDS.any { dangerous ->
            lowerCommand.contains(dangerous.lowercase())
        }
    }
    
    private fun categorizeCommand(command: String): String {
        val lowerCommand = command.lowercase()
        
        for ((category, keywords) in COMMAND_CATEGORIES) {
            for (keyword in keywords) {
                if (lowerCommand.contains(keyword.lowercase())) {
                    return category
                }
            }
        }
        
        return "unknown"
    }
    
    private fun buildAdbCommand(command: String): String {
        val lowerCommand = command.lowercase()
        
        // 根据命令类型构建相应的ADB命令
        return when {
            lowerCommand.startsWith("install") -> {
                val packagePath = command.substringAfter("install").trim()
                "pm install $packagePath"
            }
            lowerCommand.startsWith("uninstall") -> {
                val packageName = command.substringAfter("uninstall").trim()
                "pm uninstall $packageName"
            }
            lowerCommand.startsWith("list packages") -> {
                "pm list packages"
            }
            lowerCommand.startsWith("start") -> {
                val activity = command.substringAfter("start").trim()
                "am start $activity"
            }
            lowerCommand.startsWith("stop") -> {
                val packageName = command.substringAfter("stop").trim()
                "am force-stop $packageName"
            }
            lowerCommand.startsWith("clear") -> {
                val packageName = command.substringAfter("clear").trim()
                "pm clear $packageName"
            }
            lowerCommand.startsWith("ls") -> {
                val path = command.substringAfter("ls").trim()
                "ls $path"
            }
            lowerCommand.startsWith("cat") -> {
                val file = command.substringAfter("cat").trim()
                "cat $file"
            }
            lowerCommand.startsWith("mkdir") -> {
                val directory = command.substringAfter("mkdir").trim()
                "mkdir -p $directory"
            }
            lowerCommand.startsWith("rm") -> {
                val target = command.substringAfter("rm").trim()
                "rm $target"
            }
            lowerCommand.startsWith("cp") -> {
                val parts = command.substringAfter("cp").trim().split(" ", limit = 2)
                if (parts.size == 2) {
                    "cp ${parts[0]} ${parts[1]}"
                } else {
                    "cp $command"
                }
            }
            lowerCommand.startsWith("mv") -> {
                val parts = command.substringAfter("mv").trim().split(" ", limit = 2)
                if (parts.size == 2) {
                    "mv ${parts[0]} ${parts[1]}"
                } else {
                    "mv $command"
                }
            }
            lowerCommand.startsWith("pull") -> {
                val parts = command.substringAfter("pull").trim().split(" ", limit = 2)
                if (parts.size == 2) {
                    "pull ${parts[0]} ${parts[1]}"
                } else {
                    "pull ${parts[0]}"
                }
            }
            lowerCommand.startsWith("push") -> {
                val parts = command.substringAfter("push").trim().split(" ", limit = 2)
                if (parts.size == 2) {
                    "push ${parts[0]} ${parts[1]}"
                } else {
                    "push $command"
                }
            }
            lowerCommand.startsWith("getprop") -> {
                val property = command.substringAfter("getprop").trim()
                "getprop $property"
            }
            lowerCommand.startsWith("pm list") -> {
                val type = command.substringAfter("pm list").trim()
                "pm list $type"
            }
            lowerCommand.startsWith("dumpsys") -> {
                val service = command.substringAfter("dumpsys").trim()
                "dumpsys $service"
            }
            lowerCommand.startsWith("screencap") -> {
                "screencap -p /sdcard/screenshot.png"
            }
            lowerCommand.startsWith("screenrecord") -> {
                val duration = command.substringAfter("screenrecord").trim()
                "screenrecord --time-limit ${duration.ifEmpty { "10" }} /sdcard/recording.mp4"
            }
            lowerCommand.startsWith("input text") -> {
                val text = command.substringAfter("input text").trim()
                "input text $text"
            }
            lowerCommand.startsWith("input tap") -> {
                val coordinates = command.substringAfter("input tap").trim()
                "input tap $coordinates"
            }
            lowerCommand.startsWith("input swipe") -> {
                val coordinates = command.substringAfter("input swipe").trim()
                "input swipe $coordinates"
            }
            lowerCommand.startsWith("input keyevent") -> {
                val keycode = command.substringAfter("input keyevent").trim()
                "input keyevent $keycode"
            }
            lowerCommand.startsWith("settings get") -> {
                val setting = command.substringAfter("settings get").trim()
                "settings get $setting"
            }
            lowerCommand.startsWith("settings put") -> {
                val setting = command.substringAfter("settings put").trim()
                "settings put $setting"
            }
            lowerCommand.startsWith("am start") -> {
                val activity = command.substringAfter("am start").trim()
                "am start $activity"
            }
            lowerCommand.startsWith("am broadcast") -> {
                val intent = command.substringAfter("am broadcast").trim()
                "am broadcast $intent"
            }
            lowerCommand.startsWith("am service") -> {
                val service = command.substringAfter("am service").trim()
                "am service $service"
            }
            lowerCommand.startsWith("ping") -> {
                val host = command.substringAfter("ping").trim()
                "ping $host"
            }
            lowerCommand.startsWith("ifconfig") -> {
                val interfaceName = command.substringAfter("ifconfig").trim()
                "ifconfig $interfaceName"
            }
            lowerCommand.startsWith("dumpsys connectivity") -> {
                "dumpsys connectivity"
            }
            else -> {
                // 默认处理，直接返回原始命令
                command
            }
        }
    }
    
    private fun extractParameters(command: String): Map<String, String> {
        val parameters = mutableMapOf<String, String>()
        val parts = command.split(" ")
        
        var i = 0
        while (i < parts.size) {
            when (parts[i]) {
                "-p", "--path" -> {
                    if (i + 1 < parts.size) {
                        parameters["path"] = parts[i + 1]
                        i += 2
                    } else {
                        i++
                    }
                }
                "-f", "--force" -> {
                    parameters["force"] = "true"
                    i++
                }
                "-r", "--recursive" -> {
                    parameters["recursive"] = "true"
                    i++
                }
                "-v", "--verbose" -> {
                    parameters["verbose"] = "true"
                    i++
                }
                "-l", "--list" -> {
                    parameters["list"] = "true"
                    i++
                }
                else -> {
                    // 跳过未知参数
                    i++
                }
            }
        }
        
        return parameters
    }
    
    private fun generateDescription(command: String, category: String): String {
        val lowerCommand = command.lowercase()
        
        return when (category) {
            "app_management" -> {
                when {
                    lowerCommand.startsWith("install") -> "安装应用"
                    lowerCommand.startsWith("uninstall") -> "卸载应用"
                    lowerCommand.startsWith("list") -> "列出已安装应用"
                    lowerCommand.startsWith("start") -> "启动应用/Activity"
                    lowerCommand.startsWith("stop") -> "停止应用"
                    lowerCommand.startsWith("clear") -> "清除应用数据"
                    lowerCommand.startsWith("force-stop") -> "强制停止应用"
                    else -> "应用管理操作"
                }
            }
            "file_operations" -> {
                when {
                    lowerCommand.startsWith("ls") -> "列出文件"
                    lowerCommand.startsWith("cat") -> "查看文件内容"
                    lowerCommand.startsWith("mkdir") -> "创建目录"
                    lowerCommand.startsWith("rm") -> "删除文件"
                    lowerCommand.startsWith("cp") -> "复制文件"
                    lowerCommand.startsWith("mv") -> "移动文件"
                    lowerCommand.startsWith("pull") -> "从设备拉取文件"
                    lowerCommand.startsWith("push") -> "推送文件到设备"
                    else -> "文件操作"
                }
            }
            "device_info" -> {
                when {
                    lowerCommand.startsWith("getprop") -> "获取系统属性"
                    lowerCommand.startsWith("pm list") -> "列出包信息"
                    lowerCommand.startsWith("dumpsys") -> "转储系统服务信息"
                    lowerCommand.startsWith("screencap") -> "截取屏幕截图"
                    lowerCommand.startsWith("screenrecord") -> "录制屏幕"
                    else -> "设备信息查询"
                }
            }
            "input_events" -> {
                when {
                    lowerCommand.startsWith("input text") -> "输入文本"
                    lowerCommand.startsWith("input tap") -> "模拟点击"
                    lowerCommand.startsWith("input swipe") -> "模拟滑动"
                    lowerCommand.startsWith("input keyevent") -> "发送按键事件"
                    else -> "输入事件"
                }
            }
            "system_settings" -> {
                when {
                    lowerCommand.startsWith("settings get") -> "获取系统设置"
                    lowerCommand.startsWith("settings put") -> "修改系统设置"
                    lowerCommand.startsWith("am start") -> "启动Activity"
                    lowerCommand.startsWith("am broadcast") -> "发送广播"
                    lowerCommand.startsWith("am service") -> "启动服务"
                    else -> "系统设置操作"
                }
            }
            "network" -> {
                when {
                    lowerCommand.startsWith("ping") -> "网络连通性测试"
                    lowerCommand.startsWith("ifconfig") -> "网络接口配置"
                    lowerCommand.startsWith("dumpsys connectivity") -> "连接状态信息"
                    else -> "网络操作"
                }
            }
            else -> "其他命令"
        }
    }
    
    fun getSupportedCommands(): Map<String, List<String>> {
        return COMMAND_CATEGORIES
    }
    
    fun isCommandSupported(command: String): Boolean {
        val category = categorizeCommand(command)
        return category != "unknown"
    }
}