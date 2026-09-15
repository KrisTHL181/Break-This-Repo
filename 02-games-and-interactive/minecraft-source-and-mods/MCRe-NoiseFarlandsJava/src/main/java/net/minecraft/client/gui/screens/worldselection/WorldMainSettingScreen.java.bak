package net.minecraft.client.gui.screens.worldselection;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

/**
 * 🔥 MCRe NoiseFarlands —— 世界主要设置界面
 *
 * <p>将 FarLandsTraveler 模组的所有配置项移植到世界创建流程中， 允许玩家在创建世界时直接配置边境之地相关参数。
 *
 * @author MCRe Ultimate Scaler
 * @since 2026-08-04
 */
@OnlyIn(Dist.CLIENT)
public class WorldMainSettingScreen extends Screen {

    // ==================== 界面布局常量 ====================
    private static final int CONTENT_WIDTH = 320;
    private static final int SCROLL_AREA_MIN_HEIGHT = 130;

    private final Screen parent;
    private final WorldCreationContext settings;

    // ==================== 配置数据 ====================
    private final FarLandsConfigData configData;

    // ==================== 布局组件 ====================
    private final LinearLayout scrollContent = LinearLayout.vertical().spacing(6);
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private @Nullable ScrollableLayout scrollArea;

    /** "限制返回值"启用时显示的整数输入框（null 表示尚未在 init() 中创建） */
    private @Nullable EditBox limitValueInput;

    // ==================== 构造函数 ====================
    public WorldMainSettingScreen(final Screen parent, final WorldCreationContext settings) {
        super(Component.literal("世界自定义设置 │ World custom settings"));
        this.parent = parent;
        this.settings = settings;
        this.configData = new FarLandsConfigData();
    }

    // ==================== 初始化 ====================
    @Override
    protected void init() {
        // 标题放在 header
        LinearLayout header = LinearLayout.vertical().spacing(4);
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.title.copy().withStyle(ChatFormatting.BOLD), this.font));
        header.addChild(new StringWidget(
        Component.literal("§7进行对世界生成器的自定义 │ Customize the world generator"),
        this.font
        ));
        this.layout.addToHeader(header);

        // 主体内容：滚动面板
        this.buildScrollContent();
        this.scrollArea = new ScrollableLayout(this.minecraft, this.scrollContent, SCROLL_AREA_MIN_HEIGHT);
        this.scrollArea.setMinWidth(CONTENT_WIDTH);
        this.layout.addToContents(this.scrollArea);

        // 底部按钮
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        footer.addChild(Button.builder(
                Component.literal("完成"),
                button -> this.onDone()
        ).build());
        footer.addChild(Button.builder(
                Component.literal("取消"),
                button -> this.onClose()
        ).build());

        // 统一注册所有组件
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.repositionElements();
    }

    // ==================== 构建滚动内容 ====================

    private void buildScrollContent() {
        this.scrollContent.defaultCellSetting().alignHorizontallyCenter();

        // ========== 第一组：噪声插值设置 ==========
        this.scrollContent.addChild(this.createSectionHeader(
                Component.literal("§6§l噪声插值设置")
        ));

        // 1. 限制返回值（开关）——绑定 configData.limitReturnValue，切换时联动下方输入框显隐
        SwitchGrid.Builder limitReturnBuilder = SwitchGrid.builder(CONTENT_WIDTH - 20)
                .withRowSpacing(4);
        limitReturnBuilder.addSwitch(
                Component.literal("限制返回值"),
                () -> this.configData.limitReturnValue,
                val -> {
                    this.configData.limitReturnValue = val;
                    if (this.limitValueInput != null) {
                        // 联动：开启时显示输入框，关闭时隐藏（并把高度置 0 让布局收回占位）
                        this.limitValueInput.setVisible(val);
                        this.limitValueInput.setHeight(val ? 20 : 0);
                        this.repositionElements();
                    }
                }
        ).withInfo(Component.literal("启用后，噪声插值将把返回值限制在下方输入的整数范围内（例如 -10 或 1145）"));
        this.scrollContent.addChild(limitReturnBuilder.build().layout(), s -> s.paddingHorizontal(10));

        // 2. 限制返回值的整数输入框（仅在开关启用时显示，宽度 = 滚动面板宽 - 7px，居中顶住面板）
        this.limitValueInput = new IntegerOnlyEditBox(
        this.font, 0, 0, CONTENT_WIDTH - 7, 20, Component.literal("限制返回值")
        );
        this.limitValueInput.setValue(String.valueOf(this.configData.limitReturnValueValue));
        this.limitValueInput.setResponder(val -> {
            try {
                this.configData.limitReturnValueValue = Integer.parseInt(val.trim().isEmpty() ? "0" : val.trim());
            } catch (NumberFormatException ignored) {
            }
        });
        this.limitValueInput.setVisible(this.configData.limitReturnValue);
        this.limitValueInput.setHeight(this.configData.limitReturnValue ? 20 : 0);
        this.scrollContent.addChild(this.limitValueInput);

        this.scrollContent.addChild(this.createSectionHeader(
                Component.literal("§6§l边境之地设置")
        ));

        // 3. 启用天空网格（开关）
        SwitchGrid.Builder skyGridBuilder = SwitchGrid.builder(CONTENT_WIDTH - 20)
                .withRowSpacing(4)
                .withInfoUnderneath(2, true);
        skyGridBuilder.addSwitch(
                Component.literal("启用天空网格"),
                () -> this.configData.enableSkyGrid,
                val -> this.configData.enableSkyGrid = val
        ).withInfo(Component.literal("使边缘之地生成天空网格而不是天空之桥"));
        skyGridBuilder.addSwitch(
                Component.literal("强制生成天空网格"),
                () -> this.configData.forceSkyGrid,
                val -> this.configData.forceSkyGrid = val
        ).withInfo(Component.literal("即使插值前的密度值被限制，也强制生成天空网格"));
        skyGridBuilder.addSwitch(
                Component.literal("渐进式边境之地"),
                () -> this.configData.progressiveFarlands,
                val -> this.configData.progressiveFarlands = val
        ).withInfo(Component.literal("强制让自实现的 lerp 方法返回 start，不再进行原计算"));
        this.scrollContent.addChild(skyGridBuilder.build().layout(), s -> s.paddingHorizontal(10));

        // ========== 第二组：修复类设置 ==========
        this.scrollContent.addChild(this.createSectionHeader(
                Component.literal("§6§l修复类设置")
        ));

        SwitchGrid.Builder borderBuilder = SwitchGrid.builder(CONTENT_WIDTH - 20).withRowSpacing(4);
        borderBuilder.addSwitch(
                Component.literal("修复在 33552992 生成区块时的非法状态异常"),
                () -> this.configData.fixChunkOutOfBounds,
                val -> this.configData.fixChunkOutOfBounds = val
        ).withInfo(Component.literal("从 1.21.2 开始，在 X/Z 超过 ±33552992 的位置生成区块时，会导致游戏崩溃。\n抛出的异常为：IllegalStateException(\"Trying to create chunk out of reasonable bounds: \" + pos)\n这很明显是人为限制。"));
        borderBuilder.addSwitch(
                Component.literal("修复 MineshaftPieces 取平均值导致的 int 溢出崩溃"),
                () -> this.configData.fixAverageFunctionOverFlow,
                val -> this.configData.fixAverageFunctionOverFlow = val
        ).withInfo(Component.literal("当原版的取平均值方法在尝试取 1073741824 以上的平均值时会崩溃\n开启这个设置会解决这个问题"));
        borderBuilder.addSwitch(
                Component.literal("修复末地环"),
                () -> this.configData.fixEndRings,
                val -> this.configData.fixEndRings = val
        ).withInfo(Component.literal("修复在密度函数 getHeightValue 计算距离时导致的 NaN 非法值"));
        borderBuilder.addSwitch(
                Component.literal("修复 在Float精度丢失过大时生成除玩家外实体导致的崩溃"),
                () -> this.configData.fixFloatOverFlowCrash,
                val -> this.configData.fixFloatOverFlowCrash = val
        ).withInfo(Component.literal("从某个版本开始，游戏生成的实体必定会强加载一次区块，由于 Float 精度丢失，导致强加载区块必定会崩溃，此设置会修复这个问题。"));
        this.scrollContent.addChild(borderBuilder.build().layout(), s -> s.paddingHorizontal(10));

        // ========== 第三组：精度系统 ==========
        this.scrollContent.addChild(this.createSectionHeader(
                Component.literal("§b§l边境之地位置与样式")
        ));

        CycleButton<String> precisionModeButton = CycleButton.builder(
                mode -> switch (mode) {
                    case "32bit" -> Component.literal("Beta");
                    case "64bit" -> Component.literal("Vanilla");
                    case "Release" -> Component.literal("Release");
                    case "1.18-exp-32bit" -> Component.literal("1.18 Experimental Snapshot 4");
                    case "1.18-exp-64bit" ->
                            Component.literal("1.18 Experimental Snapshot 4 (64bit Ver)");
                    default -> Component.literal(mode);
                },
                "32bit"
        )
                .withInfo(Component.literal("§e控制边境之地的位置\n§fBeta: ±12550821\nVanilla: 2^88/171.103\nRelease: 2^63/171.103\n1.18 Experimental Snapshot 4: 1606505088\n\n§eBeta 属于 infdev 20100327~beta 1.7.3之间的边境之地距离配置\n§eVanilla 属于1.18.2+后边境之地后的距离配置\n§eRelease 属于beta 1.8.1~1.13.2 的边境之地距离配置\n§e1.18 Experimental Snapshot 4: 包含64bit Ver版本，都是只在实验性快照出现的边境之地距离配置"))
                .withValues("32bit", "64bit", "Release", "1.18-exp-32bit", "1.18-exp-64bit")
                .create(0, 0, CONTENT_WIDTH - 20, 20,
                        Component.literal("边境之地位置"),
                        (button, val) -> this.configData.precisionMode = val
                );
        // 添加这一行：将 precisionModeButton 添加到滚动内容中
        this.scrollContent.addChild(precisionModeButton, s -> s.paddingHorizontal(2));

        CycleButton<String> farlandsStyleButton = CycleButton.builder(
                mode -> switch (mode) {
                    case "Java-1.18.2+" -> Component.literal("Java Edition 1.18.2+");
                    case "Bedrock-Edition" -> Component.literal("Bedrock Edition 1.17.20+");
                    default -> Component.literal(mode);
                },
                "Java-1.18.2+"
        )
                .withInfo(Component.literal("§e控制边境之地的样式\n§fJava 1.18.2+: 类似高原地形，从溢出开始形成巨大高墙\nBedrock Edition: 模拟基岩版1.17.20之后的边境之地"))
                .withValues("Java-1.18.2+", "Bedrock-Edition")
                .create(0, 0, CONTENT_WIDTH - 20, 20,
                        Component.literal("边境之地样式"),
                        (button, val) -> this.configData.farlandsStyle = val
                );
        this.scrollContent.addChild(farlandsStyleButton, s -> s.paddingHorizontal(10));
        this.scrollContent.addChild(new StringWidget(
        Component.literal("§7用于控制边境之地位置和样式"),
        this.font
        ).setMaxWidth(CONTENT_WIDTH - 40), s -> s.paddingHorizontal(20).paddingBottom(4));

        // ========== 第四组：假区块设置 ==========
        this.scrollContent.addChild(this.createSectionHeader(
                Component.literal("§d§l扩展设置")
        ));

        SwitchGrid.Builder fcBuilder = SwitchGrid.builder(CONTENT_WIDTH - 20)
                .withRowSpacing(3)
                .withInfoUnderneath(2, false);
        fcBuilder.addSwitch(
                Component.literal("扩展数据包密度函数字面量限制"),
                () -> this.configData.expandDatapackValueRange,
                val -> this.configData.expandDatapackValueRange = val
        ).withInfo(Component.literal("由于原版的密度函数插值限制在 [-1000000 ~ 1000000] 之间，这意味着不能加载超过这个限制的数据包\n(例如 1.18.1 边境之地数据包)\n开启这个设置将把 NOISE_VALUE_CODEC 扩展到正负无限之间来解决这个问题"));
        fcBuilder.addSwitch(
                Component.literal("扩展部分噪声取值限制"),
                () -> this.configData.expandNoiseValueRetrievalLimit,
                val -> this.configData.expandNoiseValueRetrievalLimit = val
        ).withInfo(Component.literal("原版的部分噪声具有取值限制，这些限制将噪声取值到了一个极小的范围内\n开启这个设置将解除这个限制"));
        fcBuilder.addSwitch(
                Component.literal("允许玩家坐标为非法值"),
                () -> this.configData.allowIllegalValuePlayerPosition,
                val -> this.configData.allowIllegalValuePlayerPosition = val
        ).withInfo(Component.literal("在原版中，游戏检测到玩家坐标是非法值，会判定非法发包导致崩溃\n打开此设置将会解决这个问题"));
        fcBuilder.addSwitch(
                Component.literal("模拟回绕溢出"),
                () -> this.configData.simulatedWraparoundOverflow,
                val -> this.configData.simulatedWraparoundOverflow = val
        ).withInfo(Component.literal("允许改版在运行部分世界生成器方法时使用回绕溢出而不是原版的饱和溢出\n§e用于模拟基岩版在多架构情况下的多种溢出\n§c[警告] 不保证世界生成器不会损坏"));
        this.scrollContent.addChild(fcBuilder.build().layout(), s -> s.paddingHorizontal(10));

        // ========== 第五组：结构生成 ==========
        this.scrollContent.addChild(this.createSectionHeader(
                Component.literal("§a§l控制世界配置")
        ));

        SwitchGrid.Builder structBuilder = SwitchGrid.builder(CONTENT_WIDTH - 20).withRowSpacing(4);
        structBuilder.addSwitch(
                Component.literal("不生成结构"),
                () -> this.configData.disabledStructureSpawn,
                val -> this.configData.disabledStructureSpawn = val
        ).withInfo(Component.literal("无论游戏规则 / 世界生成器怎么定义某个结构，也总是不生成结构"));
        structBuilder.addSwitch(
                Component.literal("不生成除玩家外任何实体"),
                () -> this.configData.disabledEntitySpawn,
                val -> this.configData.disabledEntitySpawn = val
        ).withInfo(Component.literal("无论游戏规则 / 世界生成器是否需要生成除玩家外实体，也总是一律不生成"));
        this.scrollContent.addChild(structBuilder.build().layout(), s -> s.paddingHorizontal(10));
    }

    // ==================== 布局调整 ====================

    @Override
    protected void repositionElements() {
        if (this.scrollArea != null) {
            this.scrollArea.setMaxHeight(SCROLL_AREA_MIN_HEIGHT);
            this.layout.arrangeElements();
            int availableExtraHeight = this.height - this.layout.getFooterHeight() - this.scrollArea.getRectangle().bottom();
            this.scrollArea.setMaxHeight(this.scrollArea.getHeight() + availableExtraHeight);
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        // 黑色背景
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);

        // 渲染所有已注册的 renderable
        super.extractRenderState(graphics, mouseX, mouseY, a);

        // ===== 动态总结栏（每次渲染都读取最新值） =====
        Component summary = Component.literal(
                "§7当前配置: 边境之地样式 §e" + this.configData.farlandsStyle
                        + " §r§7| 边境之地位置 §b" + this.configData.precisionMode
        );
        int summaryWidth = this.font.width(summary);
        int summaryY = (this.scrollArea != null ? this.scrollArea.getRectangle().bottom() : this.height / 2) + 5;
        graphics.text(this.font, summary.getVisualOrderText(),
                (this.width - summaryWidth) / 2, summaryY, -6250336);

        // 顶部分割线
        if (this.scrollArea != null) {
            int separatorY = this.layout.getHeaderHeight();
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    Screen.HEADER_SEPARATOR,
                    0,
                    separatorY,
                    0.0F,
                    0.0F,
                    this.width,
                    2,
                    32,
                    2
            );

            // 底部分割线
            int footerTop = this.height - this.layout.getFooterHeight();
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    Screen.FOOTER_SEPARATOR,
                    0,
                    footerTop - 2,
                    0.0F,
                    0.0F,
                    this.width,
                    2,
                    32,
                    2
            );
        }
    }

    // ==================== 辅助方法 ====================

    private MultiLineTextWidget createSectionHeader(final Component text) {
        MultiLineTextWidget widget = new MultiLineTextWidget(text, this.font);
        widget.setMaxWidth(CONTENT_WIDTH - 20);
        widget.setCentered(true);
        return widget;
    }

    // ==================== 回调 ====================

    private void onDone() {
        // 保存当前配置到静态字段
        FarLandsConfigData.activeConfig = this.configData;
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        // 空实现
    }

    // ==================== 配置数据类 ====================

    public static class FarLandsConfigData {
        /** "限制返回值"开关 */
        public boolean limitReturnValue = false;

        /** "限制返回值"输入框的整数值 */
        public int limitReturnValueValue = 9;

        public boolean enableSkyGrid = false;
        public boolean forceSkyGrid = false;
        public boolean progressiveFarlands = false;
        public boolean fixChunkOutOfBounds = true;
        public boolean fixAverageFunctionOverFlow = true;

        public String precisionMode = "32bit";
        public String farlandsStyle = "Java-1.18.2+";

        public boolean fixEndRings = false;
        public boolean fixFloatOverFlowCrash = true;
        public boolean expandDatapackValueRange = true;
        public boolean expandNoiseValueRetrievalLimit = true;
        public boolean allowIllegalValuePlayerPosition = true;
        public boolean simulatedWraparoundOverflow = false;

        public boolean disabledStructureSpawn = false;
        public boolean disabledEntitySpawn = false;

        /** 当前活动的 FarLands 配置，由 WorldMainSettingScreen.onDone() 写入 */
        public static FarLandsConfigData activeConfig = new FarLandsConfigData();
    }

    /** 🔧 MCRe：整数专用输入框 —— 只允许输入整数（含一位可选负号 -）， 用于"限制返回值"等需要数值输入的配置项。粘贴时也会过滤非法字符。 */
    private static class IntegerOnlyEditBox extends EditBox {
        private IntegerOnlyEditBox(final Font font, final int x, final int y, final int width, final int height, final Component narration) {
            super(font, x, y, width, height, narration);
        }

        @Override
        public boolean charTyped(final CharacterEvent event) {
            int codepoint = event.codepoint();
            // 数字直接放行；负号仅允许出现在首位且只出现一次
            if (codepoint >= '0' && codepoint <= '9') {
                return super.charTyped(event);
            } else if (codepoint == '-' && this.getCursorPosition() == 0 && !this.getValue().contains("-")) {
                return super.charTyped(event);
            }
            return false;
        }

        @Override
        public void insertText(final String input) {
            // 过滤粘贴内容：只保留数字，负号仅当本地尚无负号且位于插入首部时保留
            StringBuilder filtered = new StringBuilder();
            boolean canMinus = !this.getValue().contains("-") && this.getCursorPosition() == 0;
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (c >= '0' && c <= '9') {
                    filtered.append(c);
                } else if (c == '-' && canMinus && filtered.length() == 0) {
                    filtered.append(c);
                    canMinus = false;
                }
            }
            super.insertText(filtered.toString());
        }
    }
}