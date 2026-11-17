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
package org.jackhuang.hmcl.web.chat;

import org.jackhuang.hmcl.dsh.DshAccount;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/// 模型能力（docs/chat.md §9）。
///
/// 优先级：用户在聊天页手动覆盖 > 供应商预设 > 内置常见模型表 > unknown。
/// 这里只回答“能不能”，不改变模型实际行为；发送前的附件映射与 reasoning 折叠都读它。
@NotNullByDefault
public final class ChatCapabilities {

    public record Capabilities(
            boolean vision, boolean files, boolean reasoning, boolean tools,
            int maxContext, int maxOutput, String source) {
    }

    /// 这个模型的思考「怎么控」：kind = none / toggle / effort / budget / forced，
    /// efforts 是模型声明允许的英文挡位（none/minimal/low/medium/high/xhigh/max），
    /// budget 是数字预算范围。UI 只展示这里有的档，协议按厂商语法下发时再夹一次。
    public record ReasoningMode(String kind, boolean canDisable, List<String> efforts,
                                int minBudget, int maxBudget, String source) {

        public static final ReasoningMode NONE =
                new ReasoningMode("none", false, List.of(), 0, 0, "none");
        public static final ReasoningMode FORCED =
                new ReasoningMode("forced", false, List.of(), 0, 0, "builtin");

        public boolean usable() {
            return !"none".equals(kind) && !"forced".equals(kind);
        }
    }

    private ChatCapabilities() {
    }

    public static Capabilities resolve(ChatStore store, String accountName, String model,
                                       String protocol) throws SQLException {
        String cleaned = model == null ? "" : model.trim();
        ChatStore.Capability saved = store.capability(accountName, cleaned);
        // 手动覆盖与「识图实测」都是用户/真实调用得出的结论，压过一切推断。
        if (saved != null && "manual".equals(saved.source())) {
            return of(saved);
        }
        if (saved != null && "probe".equals(saved.source())) {
            return of(saved);
        }
        Capabilities preset = preset(protocol, cleaned);
        if (preset != null) {
            return preset;
        }
        Capabilities catalog = ChatModelCatalog.resolve(cleaned);
        if (catalog != null) {
            return catalog;
        }
        Capabilities known = knownModel(cleaned);
        if (known != null) {
            return known;
        }
        if (saved != null) {
            return of(saved);
        }
        return new Capabilities(false, false, false, false, 32_768, 4_096, "unknown");
    }

    /// 解析思考控制方式：模型声明（models.dev）优先，其次是各家的语法/内置规则。
    /// 只有模型确实声明了 `reasoning` 才返回可用挡位，否则 UI 不该出现选择器。
    public static ReasoningMode reasoningMode(Capabilities caps, String protocol,
                                              @Nullable String base, String model) {
        if (!caps.reasoning()) {
            return ReasoningMode.NONE;
        }
        String id = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        String endpoint = base == null ? "" : base.toLowerCase(Locale.ROOT);
        ChatModelCatalog.Entry entry = ChatModelCatalog.entry(id);
        String kind = entry == null ? "none" : entry.reasoningKind();
        List<String> declared = entry == null ? List.of() : entry.reasoningValues();
        int min = entry == null ? 0 : entry.reasoningMin();
        int max = entry == null ? 0 : entry.reasoningMax();

        // Anthropic 的语法就是 budget_tokens，模型声明只用来定范围
        if ("anthropic-messages".equals(protocol)) {
            return new ReasoningMode("budget", true, List.of(),
                    Math.max(1_024, min), max > 0 ? Math.min(max, 32_768) : 32_768,
                    entry == null ? "vendor" : "catalog");
        }
        // DeepSeek 官方扩展模式只认 thinking.type 开关，不暴露预算或 effort 挡位。
        if (ChatEndpoints.DEEPSEEK_ANTHROPIC.equals(protocol)) {
            return new ReasoningMode("toggle", true, List.of(), 0, 0,
                    "vendor");
        }
        // 智谱只认 thinking.type 开关
        if (endpoint.contains("bigmodel") || endpoint.contains("zhipu")) {
            return new ReasoningMode("toggle", true, List.of(), 0, 0,
                    entry == null ? "vendor" : "catalog");
        }
        // DeepSeek 官方接口认 reasoning_effort = none/low/high/max（medium/xhigh 会被它
        // 自己映射成 low/high），所以挡位是真的能控；只有老的 reasoner 是纯思考模型。
        if (endpoint.contains("api.deepseek.com")) {
            if (id.contains("reasoner")) {
                return ReasoningMode.FORCED;
            }
            List<String> values = declared.isEmpty()
                    ? new java.util.ArrayList<>(List.of("none", "low", "high", "max"))
                    : new java.util.ArrayList<>(declared);
            if (!values.contains("none")) {
                values.add(0, "none");
            }
            return new ReasoningMode("effort", true, List.copyOf(values),
                    0, 0, entry == null ? "vendor" : "catalog");
        }
        // 百炼用 enable_thinking + thinking_budget；对用户仍然呈现强度档
        if (endpoint.contains("dashscope") || endpoint.contains("aliyuncs")) {
            List<String> values = declared.isEmpty() ? List.of("low", "medium", "high") : declared;
            return new ReasoningMode("effort", true, values, 0, 0,
                    entry == null ? "vendor" : "catalog");
        }
        // 其余 OpenAI 兼容端点：reasoning_effort。预算类声明也按强度呈现，
        // 因为我们的编码器只对 Anthropic 发 budget_tokens。
        if ("effort".equals(kind) || "budget".equals(kind)) {
            List<String> values = "effort".equals(kind) && !declared.isEmpty()
                    ? declared : List.of("low", "medium", "high");
            return new ReasoningMode("effort", values.contains("none"), values, 0, 0, "catalog");
        }
        if ("toggle".equals(kind)) {
            return new ReasoningMode("toggle", true, List.of(), 0, 0, "catalog");
        }

        // 目录没覆盖：按家族给一个保守的默认
        if (id.startsWith("gpt-5") || id.startsWith("o1") || id.startsWith("o3") || id.startsWith("o4")) {
            // gpt-5.1 起才认 reasoning_effort=none；更早的型号只能往低档压
            boolean none = id.startsWith("gpt-5.1") || id.startsWith("gpt-5.2")
                    || id.startsWith("gpt-5.3") || id.startsWith("gpt-5.4")
                    || id.startsWith("gpt-5.5") || id.startsWith("gpt-5.6");
            return new ReasoningMode("effort", none,
                    none ? List.of("none", "minimal", "low", "medium", "high")
                            : List.of("minimal", "low", "medium", "high"), 0, 0, "builtin");
        }
        if (id.contains("glm")) {
            return new ReasoningMode("toggle", true, List.of(), 0, 0, "builtin");
        }
        if (id.contains("deepseek") && (id.contains("reasoner") || id.contains("v4"))) {
            return ReasoningMode.FORCED;
        }
        return new ReasoningMode("effort", true, List.of("low", "medium", "high"), 0, 0, "builtin");
    }

    private static Capabilities of(ChatStore.Capability saved) {
        return new Capabilities(saved.vision(), saved.files(), saved.reasoning(),
                saved.tools(), saved.maxContext(), saved.maxOutput(), saved.source());
    }

    private static Capabilities preset(String protocol, String model) {
        if (ChatEndpoints.DEEPSEEK_ANTHROPIC.equals(protocol)) {
            // 官方扩展模式：图片走 Files API，文档按抽取文本发送；服务端工具可用。
            return new Capabilities(true, false, true, true, 1_000_000, 64_000, "vendor");
        }
        if ("anthropic-messages".equals(protocol)) {
            boolean reasoning = model.contains("claude-3-7") || model.contains("claude-4")
                    || model.contains("claude-sonnet-4") || model.contains("thinking");
            return new Capabilities(true, true, reasoning, true, 200_000,
                    reasoning ? 32_000 : 8_192, "vendor");
        }
        return null;
    }

    /// 内置的常见模型表。判据在 [ChatModelRules]：按模型 id 的词元匹配，
    /// 具体型号压过家族通配，认不出来的留给 `unknown` 由用户覆盖。
    static Capabilities knownModel(String model) {
        ChatModelRules.Result rule = ChatModelRules.resolve(model);
        if (rule == null) {
            return null;
        }
        return new Capabilities(rule.vision(), rule.files(), rule.reasoning(), rule.tools(),
                rule.maxContext(), rule.maxOutput(), "builtin");
    }

    /// 供应商协议本身对附件的限制：OpenAI completions 没有原生文档块，Anthropic 有。
    public static boolean nativeDocuments(String protocol) {
        return "anthropic-messages".equals(protocol);
    }

    public static String protocolOf(ChatStore store, DshAccount account) throws SQLException {
        ChatStore.AccountProfile profile = store.profile(account.key());
        if (profile != null && profile.protocol() != null && !profile.protocol().isBlank()) {
            return profile.protocol();
        }
        return ChatEndpoints.defaultProtocol(account);
    }
}