package net.ModMetadata.I18N;

import java.util.HashMap;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 🔧 MCRe：自定义内存 I18N 翻译管理器
 *
 * <p>在内存中维护一套语言字典（languageCode → key → value），完全绕过资源包 /
 * 散列资源加载链路——因此无需改动 vanilla 资源优先级即可使用自定义翻译。
 *
 * <p>工作原理（与参考类 TppTranslateManager 相同）：
 * <ul>
 *   <li>构造时从 {@link Options#languageCode}（即 options.txt 的 lang 键）读取当前语言；</li>
 *   <li>{@link #getI18N(String, Object...)} 按当前语言查字典，支持 {@link String#format} 格式化；</li>
 *   <li>不支持的语言自动回退到 en_us，保证永不缺键返回原始 key 本身。</li>
 * </ul>
 *
 * <p>本类内置中英文示例模板，可自行向 {@link #getTranslations()} 中添加 key。
 *
 * @author MCRe Ultimate Scaler
 */
@OnlyIn(Dist.CLIENT)
public class ModTranslateResources {

    private String languageCode = "en_us";

    /** languageCode → (key → 翻译文本) */
    private static final HashMap<String, HashMap<String, String>> TRANSLATIONS = new HashMap<>();
    /** languageCode → 语言显示名 */
    private static final HashMap<String, String> DISPLAY_NAMES = new HashMap<>();

    public ModTranslateResources() {
        Minecraft minecraft = Minecraft.getInstance();
        if (this.isSupportedLanguage(minecraft.options.languageCode)) {
            this.languageCode = minecraft.options.languageCode;
        }
    }

    public static HashMap<String, HashMap<String, String>> getTranslations() {
        return TRANSLATIONS;
    }

    public String getLanguageCode() {
        return this.languageCode;
    }

    public void setLanguageCode(final String languageCode) {
        this.languageCode = languageCode;
    }

    /**
     * 取翻译文本并格式化。
     *
     * <p>o 若当前语言未被注册，回退到 en_us；en_us 也没有该 key 时返回 key 本身。
     * <p>o 格式化参数异常时返回未格式化文本（容错）。
     */
    public String getI18N(final String key, final Object... args) {
        HashMap<String, String> languageMap = TRANSLATIONS.getOrDefault(this.languageCode, TRANSLATIONS.get("en_us"));
        String value = languageMap != null ? languageMap.getOrDefault(key, key) : key;
        if (args != null && args.length != 0) {
            try {
                return String.format(Locale.ROOT, value, args);
            } catch (Exception e) {
                return value;
            }
        }
        return value;
    }

    /**
     * 取翻译文本（无参数）。
     *
     * <p>🔧 修复：显式转发到变长参数版本，避免与 this.getI18N(key, args)
     * 重载解析发生无限自递归（参考类 TppTranslateManager 存在此隐患）。
     */
    public String getI18N(final String key) {
        return this.getI18N(key, (Object[]) new Object[0]);
    }

    public boolean isSupportedLanguage(final String languageCode) {
        return TRANSLATIONS.containsKey(languageCode);
    }

    /** 跟随游戏当前语言（例如切换语言时调用） */
    public void syncGameLanguage(final Minecraft minecraft) {
        String gameLanguage = minecraft.getLanguageManager().getSelected();
        if (this.isSupportedLanguage(gameLanguage)) {
            this.setLanguageCode(gameLanguage);
        } else {
            System.out.printf("[ModTranslateResources] Unsupported language : %s%n", gameLanguage);
        }
    }

    public HashMap<String, String> getDisplayNames() {
        return DISPLAY_NAMES;
    }

    /**
     * 🔧 MCRe：静态查询入口 —— 供 ClientLanguage 桥接调用。
     *
     * <p>按当前游戏语言查自定义翻译表，命中返回翻译，未命中返回 null
     * （null 表示"无自定义翻译"，由原版语言表继续处理）。
     */
    public static String getCustomTranslation(final String key) {
        String lang = Minecraft.getInstance().options.languageCode;
        HashMap<String, String> map = TRANSLATIONS.getOrDefault(lang, TRANSLATIONS.get("en_us"));
        return map != null ? map.get(key) : null;
    }

    static {
        HashMap<String, String> en_us = new HashMap<>();
        HashMap<String, String> zh_cn = new HashMap<>();
        DISPLAY_NAMES.put("en_us", "English (US)");
        DISPLAY_NAMES.put("zh_cn", "中文 (简体)");

        zh_cn.put("generator.minecraft.caves", "洞穴");
        zh_cn.put("generator.minecraft.floating_islands", "浮岛");
        zh_cn.put("generator.minecraft.1_18_1_overworld", "1.18.1 无限世界");
        zh_cn.put("generator.minecraft.so_high_overworld", "很高的主世界");
        
        en_us.put("generator.minecraft.caves", "Caves");
        en_us.put("generator.minecraft.floating_islands", "Floating Islands");
        en_us.put("generator.minecraft.1_18_1_overworld", "1.18.1 Infinity World");
        en_us.put("generator.minecraft.so_high_overworld", "So High Overworld");

        TRANSLATIONS.put("en_us", en_us);
        TRANSLATIONS.put("zh_cn", zh_cn);
    }
}