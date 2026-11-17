/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.web.transcode;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/// The text a human must read before the transcode feature may be enabled.
///
/// Deliberately short: this is not a licence, it is the operator's acknowledgement
/// that transcoding starts external ffmpeg processes which consume the host's CPU,
/// GPU and disk, that the ffmpeg build may carry its own licence obligations, and
/// that the panel never overwrites source media. The text is hashed so an enable
/// can record exactly the document that was shown.
@NotNullByDefault
public final class TranscodeTerms {

    /// Bump when the text changes; an enable records the version it accepted.
    public static final int VERSION = 1;

    private static final String TEXT = """
            转码功能使用须知

            1. 转码会启动外部 ffmpeg 进程，占用本机的 CPU、GPU、内存与磁盘。队列并发、线程数和
               硬件编码器槽位由面板限制，但你仍应确认这台机器承受得住批量编码。
            2. ffmpeg / ffprobe 可能来自系统包、镜像内置，或面板下载的第三方静态构建；其许可证与
               编码器可用性由实际构建决定，面板只负责探测与调用。
            3. 面板不会覆盖源文件。输出先写临时文件，成功后才改名；失败时可能留下 .part 或
               .failed 文件，是否保留由设置决定。
            4. 你需要确保对媒体根中的文件拥有读取与写入权限，并自行承担转码结果的使用责任。
            5. 关闭转码功能不会删除已有输出、预设或日志；删除媒体文件请使用文件管理器或应急工具。
            6. 启用需要开发者密码；Agent 不能启用或关闭这个功能，也不能修改失败策略。
            """;

    private static final String HASH = sha256(TEXT);

    private TranscodeTerms() {
    }

    /// The text as shown by the enable flow.
    public static String text() {
        return TEXT;
    }

    /// `sha256:<hex>` of [#text()].
    public static String hash() {
        return HASH;
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}