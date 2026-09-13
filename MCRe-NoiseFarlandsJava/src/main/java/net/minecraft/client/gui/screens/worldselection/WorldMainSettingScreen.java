package net.minecraft.client.gui.screens.worldselection;

import java.util.function.Consumer;
import net.MinecraftTools.Math.DynamicAccuracy.BigDecimal;
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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.WorldReposition;
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

    // 🔧 MCRe：第二页「地形偏移与缩放」输入框引用——开关启用时显示，关闭时收起
    private @Nullable EditBox xWorldScalerInput;
    private @Nullable EditBox yWorldScalerInput;
    private @Nullable EditBox zWorldScalerInput;
    private @Nullable EditBox xWorldOffsetInput;
    private @Nullable EditBox yWorldOffsetInput;
    private @Nullable EditBox zWorldOffsetInput;

    // 🔧 MCRe：第二页「流体替换」方块 ID 输入框引用——开关启用时显示，关闭时收起
    private @Nullable EditBox replaceDefaultFluidBlockInput;
    private @Nullable EditBox replaceUndergroundLavaBlockInput;

    // ==================== 分页状态 ====================
    /** 当前页码（0 起），进入界面默认为第一页 */
    private int currentPage = 0;

    /** 总页数（目前仅 1、2 两页，等待扩展） */
    private static final int PAGE_COUNT = 2;

    private @Nullable Button prevPageButton;
    private @Nullable Button nextPageButton;
    private @Nullable StringWidget titleWidget;
    private @Nullable StringWidget subtitleWidget;
    private @Nullable StringWidget pageNumberWidget;

    // ==================== 构造函数 ====================
    public WorldMainSettingScreen(final Screen parent, final WorldCreationContext settings) {
        super(Component.literal("世界自定义设置 │ World custom settings"));
        this.parent = parent;
        this.settings = settings;
        // 🔧 MCRe：从 options.txt 同目录的 farlands_config.json 加载上次保存的全局配置
        this.configData = FarLandsConfigStorage.load(Minecraft.getInstance().gameDirectory);
    }

    // ==================== 初始化 ====================
    @Override
    protected void init() {
        // 标题放在 header（存为字段以便换页时更新文案）
        LinearLayout header = LinearLayout.vertical().spacing(4);
        header.defaultCellSetting().alignHorizontallyCenter();
        this.titleWidget = new StringWidget(this.title.copy().withStyle(ChatFormatting.BOLD), this.font);
        header.addChild(this.titleWidget);
        this.subtitleWidget = new StringWidget(
        Component.literal("§7进行对世界生成器的自定义 │ Customize the world generator"),
        this.font
        );
        header.addChild(this.subtitleWidget);
        this.pageNumberWidget = new StringWidget(Component.literal(""), this.font);
        header.addChild(this.pageNumberWidget);
        this.layout.addToHeader(header);

        // 主体内容：滚动面板
        this.buildCurrentPageContent();
        this.scrollArea = new ScrollableLayout(this.minecraft, this.scrollContent, SCROLL_AREA_MIN_HEIGHT);
        this.scrollArea.setMinWidth(CONTENT_WIDTH);
        this.layout.addToContents(this.scrollArea);

        // 底部按钮：上一页 + 完成 + 取消 + 下一页
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.prevPageButton = footer.addChild(Button.builder(
                Component.literal("<="),
                button -> this.switchPage(-1)
        ).size(75, 20).build());
        footer.addChild(Button.builder(
                Component.literal("完成"),
                button -> this.onDone()
        ).build());
        footer.addChild(Button.builder(
                Component.literal("取消"),
                button -> this.onClose()
        ).build());
        this.nextPageButton = footer.addChild(Button.builder(
                Component.literal("=>"),
                button -> this.switchPage(1)
        ).size(75, 20).build());

        // 统一注册所有组件
        this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
        this.updatePageState();
        this.repositionElements();
    }

    // ==================== 构建滚动内容（按页分发） ====================

    private void buildCurrentPageContent() {
        this.scrollContent.removeChildren();
        this.scrollContent.defaultCellSetting().alignHorizontallyCenter();
        switch (this.currentPage) {
            case 0 -> this.buildPage1Content();
            case 1 -> this.buildPage2Content();
            default -> {}
        }
    }

    // 第 1 页：世界自定义设置（原内容）
    private void buildPage1Content() {
        this.scrollContent.defaultCellSetting().alignHorizontallyCenter();

        // ========== 第一组：噪声插值设置 ==========
        this.scrollContent.addChild(this.createSectionHeader(
                Component.literal("§u§l噪声插值设置")
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
        ).withInfo(Component.literal("启用后，噪声插值将把输入坐标量级限制到等级对应的对数范围，等级支持任意数值（含小数，例如 -10、9、1145）"));
        this.scrollContent.addChild(limitReturnBuilder.build().layout(), s -> s.paddingHorizontal(10));

        // 2. 限制返回值的输入框（仅在开关启用时显示，宽度 = 滚动面板宽 - 7px，居中顶住面板）
        this.limitValueInput = new NumberOnlyEditBox(
        this.font, 0, 0, CONTENT_WIDTH - 7, 20, Component.literal("限制返回值")
        );
        this.limitValueInput.setValue(WorldMainSettingScreen.formatLimitValue(this.configData.limitReturnValueValue));
        this.limitValueInput.setResponder(val -> {
            if (val.trim().isEmpty()) {
                return; // 空输入不写入配置
            }
            try {
                this.configData.limitReturnValueValue = Double.parseDouble(val.trim());
            } catch (NumberFormatException ignored) {
            }
        });
        this.limitValueInput.setVisible(this.configData.limitReturnValue);
        this.limitValueInput.setHeight(this.configData.limitReturnValue ? 20 : 0);
        this.scrollContent.addChild(this.limitValueInput);

        this.scrollContent.addChild(this.createSectionHeader(
                Component.literal("§d§l边境之地设置")
        ));

        // 3. 启用天空网格（开关）
        SwitchGrid.Builder skyGridBuilder = SwitchGrid.builder(CONTENT_WIDTH - 20)
                .withRowSpacing(4)
                .withInfoUnderneath(4, true);
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
                .withInfoUnderneath(4, false);
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

    // 第 2 页：地形偏移与缩放
    private void buildPage2Content() {
        this.scrollContent.defaultCellSetting().alignHorizontallyCenter();

        // ========== 第一节：地形生成器缩放 ==========
        this.scrollContent.addChild(this.createSectionHeader(
                Component.literal("§b§l地形生成器偏移缩放")
        ));

        // 1. 启用地形缩放（开关）—— 切换时联动下方三个 X/Y/Z 输入框显隐
        SwitchGrid.Builder scalerBuilder = SwitchGrid.builder(CONTENT_WIDTH - 20)
                .withRowSpacing(4);
        scalerBuilder.addSwitch(
                Component.literal("启用 世界生成器地形缩放"),
                () -> this.configData.enabledTerrainScaler,
                val -> {
                    this.configData.enabledTerrainScaler = val;
                    this.setScalerInputsVisible(val);
                }
        ).withInfo(Component.literal(
                "启用后，世界生成器输出将按 X/Y/Z 三个轴的缩放因子对坐标进行缩放。\n"
                        + "§e典型用途：§r把边境之地推得更远（缩放 > 1）或拉近（缩放 < 1）\n"
                        + "§c[警告] §r会影响所有方块坐标/结构生成/实体位置，超大缩放可能产生意外行为"
        ));
        this.scrollContent.addChild(scalerBuilder.build().layout(), s -> s.paddingHorizontal(10));

        // 2. 三个缩放输入框（顺序：x 在上、y 中、z 下），高度 20、宽度 = CONTENT_WIDTH 占满滚动面板
        this.xWorldScalerInput = this.createBigDecimalInput(
                Component.literal("X 轴缩放"),
                this.configData.xWorldScaler,
                val -> this.configData.xWorldScaler = val
        );
        this.yWorldScalerInput = this.createBigDecimalInput(
                Component.literal("Y 轴缩放"),
                this.configData.yWorldScaler,
                val -> this.configData.yWorldScaler = val
        );
        this.zWorldScalerInput = this.createBigDecimalInput(
                Component.literal("Z 轴缩放"),
                this.configData.zWorldScaler,
                val -> this.configData.zWorldScaler = val
        );
        this.scrollContent.addChild(this.xWorldScalerInput);
        this.scrollContent.addChild(this.yWorldScalerInput);
        this.scrollContent.addChild(this.zWorldScalerInput);

        // ========== 第二节：地形生成器偏移 ==========
        SwitchGrid.Builder offsetBuilder = SwitchGrid.builder(CONTENT_WIDTH - 20)
                .withRowSpacing(4);
        offsetBuilder.addSwitch(
                Component.literal("启用 世界生成器地形偏移"),
                () -> this.configData.enabledTerrainOffsets,
                val -> {
                    this.configData.enabledTerrainOffsets = val;
                    this.setOffsetInputsVisible(val);
                }
        ).withInfo(Component.literal(
                "启用后，世界生成器输出将按 X/Y/Z 三个轴的偏移量对坐标进行平移。\n"
                        + "§e典型用途：§r让玩家从远离原点的位置开始探索，绕过 32 位整数限制\n"
                        + "§c[警告] §r偏移会改变区块坐标基准，玩家出生点/结构位置都会偏移"
        ));
        this.scrollContent.addChild(offsetBuilder.build().layout(), s -> s.paddingHorizontal(10));

        // 3. 三个偏移输入框（顺序：x 在上、y 中、z 下）
        this.xWorldOffsetInput = this.createBigDecimalInput(
                Component.literal("X 轴偏移"),
                this.configData.xWorldOffset,
                val -> this.configData.xWorldOffset = val
        );
        this.yWorldOffsetInput = this.createBigDecimalInput(
                Component.literal("Y 轴偏移"),
                this.configData.yWorldOffset,
                val -> this.configData.yWorldOffset = val
        );
        this.zWorldOffsetInput = this.createBigDecimalInput(
                Component.literal("Z 轴偏移"),
                this.configData.zWorldOffset,
                val -> this.configData.zWorldOffset = val
        );
        this.scrollContent.addChild(this.xWorldOffsetInput);
        this.scrollContent.addChild(this.yWorldOffsetInput);
        this.scrollContent.addChild(this.zWorldOffsetInput);

        // ========== 第三节：YClampedGradient 扩展（独立开关） ==========
        // 🔧 MCRe：YClampedGradient 控制 Y 轴 base stone 海拔梯度——
        // 偏移后 Y 轴将不会出现任何边境之地，所以单独做开关供玩家权衡。
        SwitchGrid.Builder yGradientBuilder = SwitchGrid.builder(CONTENT_WIDTH - 20)
                .withRowSpacing(4);
        yGradientBuilder.addSwitch(
                Component.literal("启用 Y 轴扩展偏移"),
                () -> this.configData.enabledYClampedGradientOffset,
                val -> this.configData.enabledYClampedGradientOffset = val
        ).withInfo(Component.literal(
                "YClampedGradient 控制 Y 轴 base stone 海拔梯度。\n"
                        + "§e启用后：§r可以将 Y 轴探索范围大幅扩展（配合 yWorldOffset/yWorldScaler 使用）\n"
                        + "§c[警告] §r启用后 Y 轴将不会出现任何边境之地，海拔梯度（基岩/海平面过渡）会失效\n"
                        + "§7建议：§r除非你想专门探索超高 Y 区域，否则保持关闭"
        ));
        this.scrollContent.addChild(yGradientBuilder.build().layout(), s -> s.paddingHorizontal(10));

        // ========== 第三节追加：禁用 Offset 噪声（NoOffset 数据包移植）==========
        // 🔧 MCRe：禁用 ShiftedNoise 的 shift_x/y/z 偏移——解决渐消之地地形消失
        SwitchGrid.Builder disableOffsetBuilder = SwitchGrid.builder(CONTENT_WIDTH - 20)
                .withRowSpacing(4);
        disableOffsetBuilder.addSwitch(
                Component.literal("禁用 Offset 噪声"),
                () -> this.configData.disableOffsetNoise,
                val -> this.configData.disableOffsetNoise = val
        ).withInfo(Component.literal(
                "此开关用于禁用噪声 Offset，可以用来解决渐消之地导致的地形消失，\\n"
                        + "展现渐消之后更多的边境层"
        ));
        this.scrollContent.addChild(disableOffsetBuilder.build().layout(), s -> s.paddingHorizontal(10));

        // ========== 第四节：流体替换 ==========
        // 🔧 MCRe：UltimateScaler FluidReplace 移植——玩家可自定义海平面流体 + 地底熔岩
        // 每个开关单独一行 + 下方紧跟对应输入框（开关关闭时输入框隐藏）
        this.scrollContent.addChild(this.createSectionHeader(
                Component.literal("§a§l含水层与岩浆层")
        ));

        // 开关 1：替换默认流体 + 下方输入框
        SwitchGrid.Builder fluidReplaceBuilder1 = SwitchGrid.builder(CONTENT_WIDTH - 20)
                .withRowSpacing(4);
        fluidReplaceBuilder1.addSwitch(
                Component.literal("替换默认流体（海平面以下）"),
                () -> this.configData.replaceDefaultFluid,
                val -> {
                    this.configData.replaceDefaultFluid = val;
                    this.setFluidReplaceInputsVisible();
                }
        ).withInfo(Component.literal(
                "启用后，原版海平面以下的默认流体（水）将被替换为指定的方块。\n"
                        + "§e典型用途：§r把海变成空气（minecraft:air）或自定义流体\n"
                        + "§c[警告] §r方块 ID 必须合法（namespace:path 格式），否则回退到默认流体"
        ));
        this.scrollContent.addChild(fluidReplaceBuilder1.build().layout(), s -> s.paddingHorizontal(10));

        this.replaceDefaultFluidBlockInput = this.createStringInput(
                Component.literal("默认流体方块 ID（namespace:path）"),
                this.configData.replaceDefaultFluidBlock,
                val -> this.configData.replaceDefaultFluidBlock = val
        );
        this.scrollContent.addChild(this.replaceDefaultFluidBlockInput, s -> s.paddingHorizontal(10));

        // 开关 2：替换地底熔岩 + 下方输入框
        SwitchGrid.Builder fluidReplaceBuilder2 = SwitchGrid.builder(CONTENT_WIDTH - 20)
                .withRowSpacing(4);
        fluidReplaceBuilder2.addSwitch(
                Component.literal("替换地底熔岩（Y=-54）"),
                () -> this.configData.replaceUndergroundLava,
                val -> {
                    this.configData.replaceUndergroundLava = val;
                    this.setFluidReplaceInputsVisible();
                }
        ).withInfo(Component.literal(
                "启用后，原版地底熔岩层（Y=-54）的熔岩将被替换为指定的方块。\n"
                        + "§e典型用途：§r把地下熔岩变成空气（避免误伤）或自定义方块\n"
                        + "§c[警告] §r方块 ID 必须合法（namespace:path 格式），否则回退到熔岩"
        ));
        this.scrollContent.addChild(fluidReplaceBuilder2.build().layout(), s -> s.paddingHorizontal(10));

        this.replaceUndergroundLavaBlockInput = this.createStringInput(
                Component.literal("地底熔岩方块 ID（namespace:path）"),
                this.configData.replaceUndergroundLavaBlock,
                val -> this.configData.replaceUndergroundLavaBlock = val
        );
        this.scrollContent.addChild(this.replaceUndergroundLavaBlockInput, s -> s.paddingHorizontal(10));

        // 根据开关状态初始化输入框显隐（页面切换后重新构建时生效）
        this.setScalerInputsVisible(this.configData.enabledTerrainScaler);
        this.setOffsetInputsVisible(this.configData.enabledTerrainOffsets);
        this.setFluidReplaceInputsVisible();
    }

    private void setScalerInputsVisible(final boolean visible) {
        final int h = visible ? 20 : 0;
        if (this.xWorldScalerInput != null) {
            this.xWorldScalerInput.setVisible(visible);
            this.xWorldScalerInput.setHeight(h);
        }
        if (this.yWorldScalerInput != null) {
            this.yWorldScalerInput.setVisible(visible);
            this.yWorldScalerInput.setHeight(h);
        }
        if (this.zWorldScalerInput != null) {
            this.zWorldScalerInput.setVisible(visible);
            this.zWorldScalerInput.setHeight(h);
        }
        this.repositionElements();
    }

    /** 🔧 MCRe：偏移开关 → 三个 X/Y/Z 输入框同步显隐 */
    private void setOffsetInputsVisible(final boolean visible) {
        final int h = visible ? 20 : 0;
        if (this.xWorldOffsetInput != null) {
            this.xWorldOffsetInput.setVisible(visible);
            this.xWorldOffsetInput.setHeight(h);
        }
        if (this.yWorldOffsetInput != null) {
            this.yWorldOffsetInput.setVisible(visible);
            this.yWorldOffsetInput.setHeight(h);
        }
        if (this.zWorldOffsetInput != null) {
            this.zWorldOffsetInput.setVisible(visible);
            this.zWorldOffsetInput.setHeight(h);
        }
        this.repositionElements();
    }

    /**
     * 🔧 MCRe：创建 BigDecimal 输入框——高度 20、宽度 = CONTENT_WIDTH 占满滚动面板， 支持科学记数法 e/E（自研 BigDecimal
     * 构造器原生支持）。 responder 自动用 BigDecimal 构造器校验格式，非法输入静默丢弃（保留旧值）。
     */
    private BigDecimalEditBox createBigDecimalInput(
            final Component narration, final String initialValue, final Consumer<
                    String> onValidValue) {
        final BigDecimalEditBox box = new BigDecimalEditBox(
        this.font, 0, 0, CONTENT_WIDTH, 20, narration
        );
        box.setValue(initialValue);
        box.setResponder(val -> {
            final String trimmed = val.trim();
            if (trimmed.isEmpty()) {
                return; // 空输入不写入
            }
            try {
                // 校验格式合法性——自研 BigDecimal 构造器原生支持 e/E、负号、小数
                new BigDecimal(trimmed);
                onValidValue.accept(trimmed);
            } catch (NumberFormatException ignored) {
                // 格式非法（如 "1e"、"1e10.5"）——保留旧值，不写入 configData
            }
        });
        return box;
    }

    /**
     * 🔧 MCRe：通用字符串输入框——不限字符集（用于方块 ID "namespace:path" 等）， 移除 EditBox 默认 maxLength=32
     * 限制，setResponder 直接写原值到 configData。 运行时解析失败由调用方处理（如 {@code
     * BuiltInRegistries.BLOCK.getOptional} 返回空时回退）。
     */
    private EditBox createStringInput(final Component narration, final String initialValue, final Consumer<
                    String> onValueChange) {
        final EditBox box = new EditBox(this.font, 0, 0, CONTENT_WIDTH, 20, narration);
        box.setMaxLength(Integer.MAX_VALUE);
        box.setValue(initialValue);
        box.setResponder(onValueChange::accept);
        return box;
    }

    /** 🔧 MCRe：流体替换开关 → 两个方块 ID 输入框同步显隐（仿 setOffsetInputsVisible 模式） */
    private void setFluidReplaceInputsVisible() {
        final boolean fluidVisible = this.configData.replaceDefaultFluid;
        final boolean lavaVisible = this.configData.replaceUndergroundLava;
        if (this.replaceDefaultFluidBlockInput != null) {
            this.replaceDefaultFluidBlockInput.setVisible(fluidVisible);
            this.replaceDefaultFluidBlockInput.setHeight(fluidVisible ? 20 : 0);
        }
        if (this.replaceUndergroundLavaBlockInput != null) {
            this.replaceUndergroundLavaBlockInput.setVisible(lavaVisible);
            this.replaceUndergroundLavaBlockInput.setHeight(lavaVisible ? 20 : 0);
        }
        this.repositionElements();
    }

    // ==================== 分页逻辑 ====================

    private void switchPage(final int delta) {
        int newPage = this.currentPage + delta;
        if (newPage < 0 || newPage >= PAGE_COUNT) {
            return;
        }
        this.currentPage = newPage;
        this.buildCurrentPageContent();
        // 换页后刷新滚动面板，收集新页控件
        if (this.scrollArea != null) {
            this.scrollArea.arrangeElements();
        }
        this.updatePageState();
        this.repositionElements();
    }

    /** 刷新页码显示、上一页/下一页按钮可用状态、标题/副标题文案 */
    private void updatePageState() {
        if (this.pageNumberWidget != null) {
            this.pageNumberWidget.setMessage(Component.literal("§7第 " + (this.currentPage + 1) + " / " + PAGE_COUNT + " 页"));
        }
        if (this.prevPageButton != null) {
            this.prevPageButton.active = this.currentPage > 0;
        }
        if (this.nextPageButton != null) {
            this.nextPageButton.active = this.currentPage < PAGE_COUNT - 1;
        }
        if (this.titleWidget != null && this.subtitleWidget != null) {
            if (this.currentPage == 1) {
                this.titleWidget.setMessage(Component.literal("地形偏移与缩放").withStyle(ChatFormatting.BOLD));
                this.subtitleWidget.setMessage(Component.literal("§7用于在 32 位整数限制下探索更远的地形"));
            } else {
                this.titleWidget.setMessage(this.title.copy().withStyle(ChatFormatting.BOLD));
                this.subtitleWidget.setMessage(Component.literal("§7进行对世界生成器的自定义 │ Customize the world generator"));
            }
        }
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
        FarLandsConfigData cfg = this.configData;
        FarLandsConfigData.activeConfig = cfg;
        // 一次性解析为 BigDecimal（自研 DynamicAccuracy 库，无大小限制），避免运行期反复 parse 字符串
        WorldReposition.refresh(new WorldReposition.RepositionConfig(
        cfg.enabledTerrainScaler ? WorldReposition.parseOrFallback(cfg.xWorldScaler, BigDecimal.ONE) : BigDecimal.ONE,
        cfg.enabledTerrainScaler ? WorldReposition.parseOrFallback(cfg.yWorldScaler, BigDecimal.ONE) : BigDecimal.ONE,
        cfg.enabledTerrainScaler ? WorldReposition.parseOrFallback(cfg.zWorldScaler, BigDecimal.ONE) : BigDecimal.ONE,
        cfg.enabledTerrainOffsets ? WorldReposition.parseOrFallback(cfg.xWorldOffset, BigDecimal.ZERO) : BigDecimal.ZERO,
        cfg.enabledTerrainOffsets ? WorldReposition.parseOrFallback(cfg.yWorldOffset, BigDecimal.ZERO) : BigDecimal.ZERO,
        cfg.enabledTerrainOffsets ? WorldReposition.parseOrFallback(cfg.zWorldOffset, BigDecimal.ZERO) : BigDecimal.ZERO,
        cfg.enabledYClampedGradientOffset
        ));
        FarLandsConfigStorage.save(Minecraft.getInstance().gameDirectory, cfg);
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

    /** 🔧 MCRe：限制值的显示格式化——整数不带 ".0"，小数保留原样 */
    private static String formatLimitValue(final double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    public static class FarLandsConfigData {
        /** "限制返回值"开关 */
        public boolean limitReturnValue = false;

        /** "限制返回值"输入框的数值（支持任意实数，含小数） */
        public double limitReturnValueValue = 9.0;

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

        // 🔧 MCRe：流体替换（UltimateScaler FluidReplace 移植）——玩家可自定义海平面流体 + 地底熔岩
        public boolean replaceDefaultFluid = false;
        public String replaceDefaultFluidBlock = "minecraft:air";
        public boolean replaceUndergroundLava = false;
        public String replaceUndergroundLavaBlock = "minecraft:air";

        public boolean disabledStructureSpawn = false;
        public boolean disabledEntitySpawn = false;

        // 🔧 MCRe：第二页「地形偏移与缩放」——
        // scaler 默认 1（无缩放），offset 默认 0（无偏移）。用 String 存 BigDecimal.toString()，
        // Gson 自动序列化，无需写 TypeAdapter；UI 层用 BigDecimal.parse(...) → WorldReposition 刷新缓存。
        public boolean enabledTerrainScaler = false;
        public String xWorldScaler = "1";
        public String yWorldScaler = "1";
        public String zWorldScaler = "1";

        public boolean enabledTerrainOffsets = false;
        public String xWorldOffset = "0";
        public String yWorldOffset = "0";
        public String zWorldOffset = "0";

        // 🔧 MCRe：YClampedGradient 偏移独立开关——默认关闭。
        // 原因：YClampedGradient 控制 Y 轴 base stone 海拔梯度，偏移后 Y 轴边境之地特征会消失。
        public boolean enabledYClampedGradientOffset = false;

        // 🔧 MCRe：禁用 Offset 噪声（NoOffset 数据包移植）——禁用 ShiftedNoise 的 shift_x/y/z 偏移
        // 用于解决渐消之地地形消失，展现渐消之后更多的边境层
        public boolean disableOffsetNoise = false;

        /** 当前活动的 FarLands 配置，由 WorldMainSettingScreen.onDone() 写入 */
        public static FarLandsConfigData activeConfig = new FarLandsConfigData();
    }

    /** 🔧 MCRe：数字专用输入框 —— 只允许输入数字（含一位可选负号 -、小数点 .）， 支持整数与小数（如 -10、1145、-3.14）。粘贴时也会过滤非法字符。 */
    private static class NumberOnlyEditBox extends EditBox {
        private NumberOnlyEditBox(final Font font, final int x, final int y, final int width, final int height, final Component narration) {
            super(font, x, y, width, height, narration);
            // 🔧 MCRe：移除 EditBox 默认 maxLength=32 限制——科学记数法长串不受 32 字符截断
            this.setMaxLength(Integer.MAX_VALUE);
        }

        @Override
        public boolean charTyped(final CharacterEvent event) {
            int codepoint = event.codepoint();
            // 数字直接放行
            if (codepoint >= '0' && codepoint <= '9') {
                return super.charTyped(event);
            }
            // 负号仅允许出现在首位且只出现一次
            if (codepoint == '-' && this.getCursorPosition() == 0 && !this.getValue().contains("-")) {
                return super.charTyped(event);
            }
            // 小数点仅允许出现一次（不支持科学计数法的 'e'，保持简单）
            if (codepoint == '.' && !this.getValue().contains(".")) {
                return super.charTyped(event);
            }
            return false;
        }

        @Override
        public void insertText(final String input) {
            // 过滤粘贴内容：只保留数字、小数点、负号（负号仅首位一次、小数点仅一次）
            StringBuilder filtered = new StringBuilder();
            boolean canMinus = !this.getValue().contains("-") && this.getCursorPosition() == 0;
            // 🔧 修复：canDot 只看当前值是否已有点，不再判断 input 本身——
            // 否则键盘敲单个 '.' 时 input="." 会让 canDot=false，小数点被自己吞掉。
            boolean canDot = !this.getValue().contains(".");
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (c >= '0' && c <= '9') {
                    filtered.append(c);
                } else if (c == '-' && canMinus && filtered.length() == 0) {
                    filtered.append(c);
                    canMinus = false;
                } else if (c == '.' && canDot) {
                    filtered.append(c);
                    canDot = false;
                }
            }
            super.insertText(filtered.toString());
        }
    }

    /**
     * 🔧 MCRe：BigDecimal 专用输入框——支持科学记数法 e/E。 解析路径：String → BigDecimal（自研 DynamicAccuracy 库构造器原生支持
     * e/E）→ WorldReposition.parseOrFallback。 非法输入（如 "1e"、"1e10.5"、".e5"）在 responder 端用 BigDecimal
     * 构造器校验后静默丢弃，保留旧值。
     */
    private static class BigDecimalEditBox extends EditBox {
        private BigDecimalEditBox(final Font font, final int x, final int y, final int width, final int height, final Component narration) {
            super(font, x, y, width, height, narration);
            this.setMaxLength(Integer.MAX_VALUE);
        }

        /** 校验在 cursor 位置插入 c 后是否仍为合法科学记数法前缀 */
        private boolean canInsertAtCursor(final char c) {
            String v = this.getValue();
            int cursor = this.getCursorPosition();
            // 构造插入后的字符串
            StringBuilder sb = new StringBuilder(v);
            sb.insert(cursor, c);
            String candidate = sb.toString();
            // 快速校验：只含 0-9 . - + e/E，且结构合法
            // 允许的前缀模式：[数字]* [.]? [数字]* [eE]? [+-]? [数字]*
            // 逐字符状态机校验
            boolean hasDigit = false;
            boolean hasDot = false;
            boolean hasE = false;
            boolean eSeen = false;
            for (int i = 0; i < candidate.length(); i++) {
                char ch = candidate.charAt(i);
                if (ch >= '0' && ch <= '9') {
                    hasDigit = true;
                    if (eSeen) continue;
                } else if (ch == '.') {
                    if (hasDot || hasE) return false;
                    hasDot = true;
                } else if (ch == 'e' || ch == 'E') {
                    if (hasE || !hasDigit) return false; // e 前必须有数字
                    hasE = true;
                    eSeen = true;
                } else if (ch == '-' || ch == '+') {
                    // 符号只能出现在开头或 e/E 后
                    if (i != 0 && candidate.charAt(i - 1) != 'e' && candidate.charAt(i - 1) != 'E')
                        return false;
                } else {
                    return false; // 非法字符
                }
            }
            // 不能以 . 结尾、不能以 e/E 结尾、不能以 +/- 结尾
            char last = candidate.charAt(candidate.length() - 1);
            return last != '.' && last != 'e' && last != 'E' && last != '-' && last != '+';
        }

        @Override
        public boolean charTyped(final CharacterEvent event) {
            int codepoint = event.codepoint();
            char c = (char) codepoint;
            // 只允许 ASCII 可打印字符
            if (codepoint > 127) return false;
            return canInsertAtCursor(c) && super.charTyped(event);
        }

        @Override
        public void insertText(final String input) {
            StringBuilder filtered = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (canInsertAtCursor(c)) {
                    filtered.append(c);
                }
            }
            super.insertText(filtered.toString());
        }
    }
}