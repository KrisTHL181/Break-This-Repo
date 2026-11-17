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

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/// 模型能力的默认判断表（docs/chat.md §9）。
///
/// 判据是模型 id 的**词元序列**，而不是子串：`qwen3.8-flash` 拆成
/// `qwen 3 . 8 - flash`，`tokens("qwen","3","8")` 才命中；子串判断会把
/// `qwen3-8b`（参数量）也算成 3.8。匹配按「命中的词元数」评分，分最高的规则
/// 胜出，同分规则的能力取并集——这就是 RikkaHub `ModelRegistry` 的做法，
/// 好处是具体型号（`qwen3.8-max`）能压过家族通配（`qwen3.8`）。
///
/// 表里的规则只描述「这个型号确定有/没有」的能力：能识图、能思考、能给工具。
/// 认不出来的 id 仍然落到 `unknown`（全关），由用户在聊天页覆盖，不瞎猜。
@NotNullByDefault
final class ChatModelRules {

    /// 一条规则。`sequences` 之间是 AND，单条序列内部是「按顺序出现的词元」。
    /// 每个词元可以写 `gpt|chatgpt` 这样的多选。
    record Rule(List<List<String>> sequences, List<List<String>> exclusions, @Nullable String exact,
                boolean vision, boolean files, boolean reasoning, boolean tools,
                int maxContext, int maxOutput) {

        boolean matches(List<String> tokens) {
            if (exact != null && !exact.isEmpty()) {
                return exact.equalsIgnoreCase(String.join("", tokens));
            }
            for (List<String> exclusion : exclusions) {
                if (sequenceMatches(tokens, exclusion)) {
                    return false;
                }
            }
            for (List<String> sequence : sequences) {
                if (!sequenceMatches(tokens, sequence)) {
                    return false;
                }
            }
            return !sequences.isEmpty();
        }

        int score(List<String> tokens) {
            if (exact != null && !exact.isEmpty()) {
                return 1000 + tokens.size();
            }
            int score = 0;
            for (List<String> sequence : sequences) {
                score += sequence.size();
            }
            return score;
        }
    }

    static final class Builder {
        private final List<List<String>> sequences = new ArrayList<>();
        private final List<List<String>> exclusions = new ArrayList<>();
        private String exact;
        private boolean vision;
        private boolean files;
        private boolean reasoning;
        private boolean tools = true;
        private int maxContext = 128_000;
        private int maxOutput = 16_384;

        /// 追加一段词元序列；多次调用是 AND。
        Builder tokens(String... alternatives) {
            sequences.add(List.of(alternatives));
            return this;
        }

        /// 出现这段序列就不匹配（例如 `gpt-5-chat` 是纯文本）。
        Builder notTokens(String... alternatives) {
            exclusions.add(List.of(alternatives));
            return this;
        }

        Builder exact(String id) {
            this.exact = id;
            return this;
        }

        Builder vision() {
            this.vision = true;
            return this;
        }

        Builder files() {
            this.files = true;
            return this;
        }

        Builder reasoning() {
            this.reasoning = true;
            return this;
        }

        Builder noTools() {
            this.tools = false;
            return this;
        }

        Builder ctx(int value) {
            this.maxContext = value;
            return this;
        }

        Builder out(int value) {
            this.maxOutput = value;
            return this;
        }

        Rule build() {
            return new Rule(List.copyOf(sequences), List.copyOf(exclusions), exact,
                    vision, files, reasoning, tools, maxContext, maxOutput);
        }
    }

    private ChatModelRules() {
    }

    private static Builder def() {
        return new Builder();
    }

    /// 命中规则时返回 true/false；没有任何规则命中返回 null（交给 unknown）。
    static @Nullable Result resolve(String model) {
        String id = model == null ? "" : model.trim();
        if (id.isEmpty()) {
            return null;
        }
        List<String> tokens = tokenize(id);
        if (tokens.isEmpty()) {
            return null;
        }
        int best = -1;
        List<Rule> hits = new ArrayList<>();
        for (Rule rule : RULES) {
            if (!rule.matches(tokens)) {
                continue;
            }
            int score = rule.score(tokens);
            if (score > best) {
                best = score;
                hits.clear();
                hits.add(rule);
            } else if (score == best) {
                hits.add(rule);
            }
        }
        if (hits.isEmpty()) {
            return null;
        }
        boolean vision = false;
        boolean files = false;
        boolean reasoning = false;
        boolean tools = false;
        int context = 0;
        int output = 0;
        for (Rule rule : hits) {
            vision |= rule.vision();
            files |= rule.files();
            reasoning |= rule.reasoning();
            tools |= rule.tools();
            context = Math.max(context, rule.maxContext());
            output = Math.max(output, rule.maxOutput());
        }
        return new Result(vision, files, reasoning, tools, context, output);
    }

    record Result(boolean vision, boolean files, boolean reasoning, boolean tools,
                  int maxContext, int maxOutput) {
    }

    /// 词元：连续的字母、连续的数字、以及单个其它字符。全部小写。
    static List<String> tokenize(String id) {
        String input = id.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        for (int index = 0; index < input.length(); ) {
            char ch = input.charAt(index);
            if (Character.isLetter(ch)) {
                int start = index;
                while (index < input.length() && Character.isLetter(input.charAt(index))) {
                    index++;
                }
                tokens.add(input.substring(start, index));
            } else if (Character.isDigit(ch)) {
                int start = index;
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
                tokens.add(input.substring(start, index));
            } else {
                tokens.add(String.valueOf(ch));
                index++;
            }
        }
        return tokens;
    }

    /// 序列必须按顺序出现（不要求连续）；`72b`、`32k` 这种量级数字不参与匹配，
    /// 否则 `qwen3-8b` 会被当成 3.8 版本。
    private static boolean sequenceMatches(List<String> tokens, List<String> sequence) {
        if (sequence.isEmpty()) {
            return false;
        }
        int index = 0;
        for (int i = 0; i < tokens.size(); i++) {
            if (isMagnitude(tokens, i)) {
                continue;
            }
            if (sequence.get(index).contains(tokens.get(i))) {
                index++;
                if (index == sequence.size()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isMagnitude(List<String> tokens, int index) {
        String token = tokens.get(index);
        if (token.isEmpty() || !Character.isDigit(token.charAt(0))) {
            return false;
        }
        String next = index + 1 < tokens.size() ? tokens.get(index + 1) : "";
        return "b".equals(next) || "k".equals(next);
    }

    // ------------------------------------------------------------------
    // 规则表。新增型号时按「家族 → 版本 → 具体变体」的顺序加，具体变体的词元
    // 更多，自然压过家族规则。vision 只在型号确实吃图时打开；不知道就不加。
    // ------------------------------------------------------------------

    private static final List<Rule> RULES = List.of(
            // OpenAI
            def().tokens("gpt|chatgpt", "4", "o").vision().ctx(128_000).out(16_384).build(),
            def().tokens("gpt", "4", "turbo").vision().ctx(128_000).out(4_096).build(),
            def().tokens("gpt", "4", "vision").vision().ctx(128_000).out(4_096).build(),
            def().tokens("gpt", "4", "1").vision().ctx(1_000_000).out(32_768).build(),
            def().tokens("gpt", "5", "chat").noTools().ctx(128_000).out(8_192).build(),
            def().tokens("gpt", "5").notTokens("5", "chat").vision().reasoning()
                    .ctx(400_000).out(128_000).build(),
            def().tokens("gpt", "oss").reasoning().ctx(128_000).out(32_768).build(),
            def().tokens("o", "1|3|4").reasoning().ctx(200_000).out(100_000).build(),

            // Google
            def().tokens("gemini").vision().reasoning().ctx(1_000_000).out(65_536).build(),
            def().tokens("gemma").ctx(128_000).out(8_192).build(),

            // Anthropic
            def().tokens("claude", "3").vision().files().ctx(200_000).out(8_192).build(),
            def().tokens("claude", "5").vision().files().reasoning().ctx(1_000_000).out(64_000).build(),
            def().tokens("claude", "4", "5").vision().files().reasoning().ctx(1_000_000).out(64_000).build(),
            def().tokens("claude", "4").vision().files().reasoning().ctx(200_000).out(32_000).build(),
            def().tokens("claude").vision().files().ctx(200_000).out(8_192).build(),

            // DeepSeek
            def().tokens("deepseek", "v", "4", "flash", "vision", "exp").vision().reasoning()
                    .ctx(1_000_000).out(64_000).build(),
            def().tokens("deepseek", "v", "4", "1", "flash").vision().reasoning()
                    .ctx(1_000_000).out(64_000).build(),
            def().tokens("deepseek", "v", "4").reasoning().ctx(1_000_000).out(64_000).build(),
            def().tokens("deepseek", "flash").vision().reasoning().ctx(1_000_000).out(64_000).build(),
            def().tokens("deepseek", "v", "3").reasoning().ctx(128_000).out(8_192).build(),
            def().tokens("deepseek").noTools().ctx(128_000).out(8_192).build(),
            def().tokens("deepseek", "r", "1|2").reasoning().ctx(128_000).out(32_768).build(),
            def().tokens("deepseek", "reasoner").reasoning().ctx(128_000).out(32_768).build(),
            def().tokens("deepseek", "chat").ctx(128_000).out(8_192).build(),

            // 通义千问 / 阿里百炼
            def().tokens("qwen", "mt").noTools().ctx(128_000).out(8_192).build(),
            def().tokens("qwen", "4", "max").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "4").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3", "5", "max").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3", "6", "max").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3", "7", "max").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3", "8", "max").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3", "vl").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3", "omni").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3", "5").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3", "6").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3", "7").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3", "8").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("qwen", "3").reasoning().ctx(128_000).out(32_768).build(),
            def().tokens("qwq").reasoning().ctx(128_000).out(32_768).build(),
            def().tokens("qwen").ctx(128_000).out(8_192).build(),

            // 智谱 GLM
            def().tokens("glm", "4", "5", "v|vl").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("glm", "4", "6", "v|vl").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("glm", "4", "v").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("glm", "5", "3", "flash").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("glm", "5").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("glm", "4", "5").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("glm", "4", "6").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("glm", "4", "7").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("glm").ctx(128_000).out(8_192).build(),

            // Kimi / Moonshot
            def().tokens("kimi", "k", "3").vision().reasoning().ctx(256_000).out(32_768).build(),
            def().tokens("kimi", "k", "2", "5").vision().reasoning().ctx(256_000).out(16_384).build(),
            def().tokens("kimi", "k", "2", "6").vision().reasoning().ctx(256_000).out(16_384).build(),
            def().tokens("kimi", "k", "2").reasoning().ctx(256_000).out(16_384).build(),
            def().tokens("kimi|moonshot").ctx(256_000).out(16_384).build(),
            def().exact("k3").vision().reasoning().ctx(256_000).out(32_768).build(),

            // 字节豆包
            def().tokens("doubao", "2", "1").vision().reasoning().ctx(256_000).out(32_768).build(),
            def().tokens("doubao", "2", "0").vision().reasoning().ctx(256_000).out(16_384).build(),
            def().tokens("doubao", "1", "8").vision().reasoning().ctx(256_000).out(16_384).build(),
            def().tokens("doubao", "1", "6").vision().reasoning().ctx(256_000).out(16_384).build(),
            def().tokens("doubao").ctx(256_000).out(8_192).build(),

            // xAI
            def().tokens("grok", "4").vision().reasoning().ctx(256_000).out(32_768).build(),
            def().tokens("grok").ctx(131_072).out(16_384).build(),

            // MiniMax
            def().tokens("minimax", "m", "3").vision().reasoning().ctx(204_000).out(32_768).build(),
            def().tokens("minimax", "m", "2").reasoning().ctx(204_000).out(16_384).build(),
            def().tokens("minimax").reasoning().ctx(204_000).out(16_384).build(),

            // 阶跃 / 商汤 / 小米 / 腾讯混元 / 美团 / 其它
            def().tokens("step", "5").vision().reasoning().ctx(128_000).out(32_768).build(),
            def().tokens("step", "3", "7", "flash").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("step", "3").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("intern", "s", "1").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("internvl").vision().ctx(128_000).out(8_192).build(),
            def().tokens("mimo", "v", "2", "5", "pro").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("mimo", "v", "2|3").vision().reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("hy|hunyuan", "3|4").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("longcat", "2", "0").reasoning().ctx(128_000).out(16_384).build(),
            def().tokens("muse", "spark|glimmer").vision().reasoning().ctx(128_000).out(16_384).build(),

            // 开源视觉/文本模型
            def().tokens("llava").vision().ctx(32_768).out(4_096).build(),
            def().tokens("pixtral").vision().ctx(128_000).out(8_192).build(),
            def().tokens("moondream").vision().ctx(8_192).out(2_048).build(),
            def().tokens("llama").ctx(128_000).out(8_192).build(),
            def().tokens("mistral|mixtral").ctx(128_000).out(8_192).build(),

            // 名称里带这些词就按「吃图」处理，最后兜底；具体型号的词元更多，会压过它
            def().tokens("vision").vision().ctx(128_000).out(8_192).build(),
            def().tokens("vl").vision().ctx(128_000).out(8_192).build(),
            def().tokens("omni").vision().ctx(128_000).out(8_192).build()
    );
}