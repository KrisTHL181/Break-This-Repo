package net.minecraft.client.gui.screens.options;

import dev.miru.gui.screens.MultiActionScreen;
import dev.miru.gui.screens.config.GeneralSettingScreen;
import dev.miru.helper.ComponentActionPair;
import dev.miru.main.ModMain;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.EqualSpacingLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.Difficulty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OptionsScreen extends Screen {
   private static final Component TITLE = Component.translatable("options.title");
   private static final Component SKIN_CUSTOMIZATION = Component.translatable("options.skinCustomisation");
   private static final Component SOUNDS = Component.translatable("options.sounds");
   private static final Component VIDEO = Component.translatable("options.video");
   public static final Component CONTROLS = Component.translatable("options.controls");
   private static final Component LANGUAGE = Component.translatable("options.language");
   private static final Component CHAT = Component.translatable("options.chat");
   private static final Component RESOURCEPACK = Component.translatable("options.resourcepack");
   private static final Component ACCESSIBILITY = Component.translatable("options.accessibility");
   private static final Component TELEMETRY = Component.translatable("options.telemetry");
   private static final Tooltip TELEMETRY_DISABLED_TOOLTIP = Tooltip.create(Component.translatable("options.telemetry.disabled"));
   private static final Component CREDITS_AND_ATTRIBUTION = Component.translatable("options.credits_and_attribution");
   private static final int COLUMNS = 2;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 61, 33);
   private final Screen lastScreen;
   private final Options options;
   private @Nullable CycleButton<Difficulty> difficultyButton;
   private @Nullable LockIconButton lockButton;

   public OptionsScreen(Screen p_344232_, Options p_342732_) {
      super(TITLE);
      this.lastScreen = p_344232_;
      this.options = p_342732_;
   }

   @Override
   protected void init() {
      LinearLayout linearlayout = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
      linearlayout.addChild(new StringWidget(TITLE, this.font), LayoutSettings::alignHorizontallyCenter);
      LinearLayout linearlayout1 = linearlayout.addChild(LinearLayout.horizontal()).spacing(8);
      linearlayout1.addChild(this.options.fov().createButton(this.minecraft.options));
      linearlayout1.addChild(this.createOnlineButton());
      GridLayout gridlayout = new GridLayout();
      gridlayout.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
      GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(2);
      gridlayout$rowhelper.addChild(this.openScreenButton(SOUNDS, () -> new SoundOptionsScreen(this, this.options)));
      gridlayout$rowhelper.addChild(this.openScreenButton(VIDEO, () -> new VideoSettingsScreen(this, this.minecraft, this.options)));
      gridlayout$rowhelper.addChild(this.openScreenButton(CONTROLS, () -> new ControlsScreen(this, this.options)));
      gridlayout$rowhelper.addChild(this.openScreenButton(LANGUAGE, () -> new LanguageSelectScreen(this, this.options, this.minecraft.getLanguageManager())));
      gridlayout$rowhelper.addChild(
         this.openScreenButton(
            RESOURCEPACK,
            () -> new PackSelectionScreen(
               this.minecraft.getResourcePackRepository(),
               this::applyPacks,
               this.minecraft.getResourcePackDirectory(),
               Component.translatable("resourcePack.title")
            )
         )
      );
      gridlayout$rowhelper.addChild(
         this.openScreenButton(
            Component.literal(ModMain.getI18N("screen.options.more")),
            () -> new MultiActionScreen(
               this,
               List.of(
                  new ComponentActionPair(SKIN_CUSTOMIZATION, () -> this.minecraft.setScreen(new SkinCustomizationScreen(this, this.options))),
                  new ComponentActionPair(CHAT, () -> this.minecraft.setScreen(new ChatOptionsScreen(this, this.options))),
                  new ComponentActionPair(ACCESSIBILITY, () -> this.minecraft.setScreen(new AccessibilityOptionsScreen(this, this.options))),
                  new ComponentActionPair(TELEMETRY, () -> this.minecraft.setScreen(new TelemetryInfoScreen(this, this.options))),
                  new ComponentActionPair(CREDITS_AND_ATTRIBUTION, () -> this.minecraft.setScreen(new CreditsAndAttributionScreen(this)))
               ),
               10,
               20,
               200,
               20
            )
         )
      );
      gridlayout$rowhelper.addChild(
         this.openScreenButton(Component.literal(ModMain.getI18N("screen.pause.tpp_menu")), () -> new GeneralSettingScreen(this, ModMain.getOptions()))
      );
      this.layout.addToContents(gridlayout);
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, p_342134_ -> this.onClose()).width(200).build());
      this.layout.visitWidgets(p_343581_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_343581_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void applyPacks(PackRepository p_343305_) {
      this.options.updateResourcePacks(p_343305_);
      this.minecraft.setScreen(this);
   }

   private LayoutElement createOnlineButton() {
      if (this.minecraft.level != null && this.minecraft.hasSingleplayerServer()) {
         this.difficultyButton = createDifficultyButton(0, 0, "options.difficulty", this.minecraft);
         if (!this.minecraft.level.getLevelData().isHardcore()) {
            this.lockButton = new LockIconButton(
               0,
               0,
               p_342662_ -> this.minecraft
                  .setScreen(
                     new ConfirmScreen(
                        this::lockCallback,
                        Component.translatable("difficulty.lock.title"),
                        Component.translatable("difficulty.lock.question", this.minecraft.level.getLevelData().getDifficulty().getDisplayName())
                     )
                  )
            );
            this.difficultyButton.setWidth(this.difficultyButton.getWidth() - this.lockButton.getWidth());
            this.lockButton.setLocked(false);
            this.lockButton.active = true;
            this.difficultyButton.active = !this.lockButton.isLocked();
            EqualSpacingLayout equalspacinglayout = new EqualSpacingLayout(150, 0, EqualSpacingLayout.Orientation.HORIZONTAL);
            equalspacinglayout.addChild(this.difficultyButton);
            equalspacinglayout.addChild(this.lockButton);
            return equalspacinglayout;
         } else {
            this.difficultyButton.active = false;
            return this.difficultyButton;
         }
      } else {
         return Button.builder(Component.translatable("options.online"), p_342674_ -> this.minecraft.setScreen(new OnlineOptionsScreen(this, this.options)))
            .bounds(this.width / 2 + 5, this.height / 6 - 12 + 24, 150, 20)
            .build();
      }
   }

   public static CycleButton<Difficulty> createDifficultyButton(int p_344941_, int p_344675_, String p_345303_, Minecraft p_344456_) {
      return CycleButton.builder(Difficulty::getDisplayName, p_344456_.level.getDifficulty())
         .withValues(Difficulty.values())
         .create(
            p_344941_,
            p_344675_,
            150,
            20,
            Component.translatable(p_345303_),
            (p_342052_, p_343592_) -> p_344456_.getConnection().send(new ServerboundChangeDifficultyPacket(p_343592_))
         );
   }

   private void lockCallback(boolean p_344308_) {
      this.minecraft.setScreen(this);
      if (p_344308_ && this.minecraft.level != null && this.lockButton != null && this.difficultyButton != null) {
         this.minecraft.getConnection().send(new ServerboundLockDifficultyPacket(true));
         this.lockButton.setLocked(false);
         this.lockButton.active = false;
         this.difficultyButton.active = true;
      }
   }

   @Override
   public void removed() {
      this.options.save();
   }

   private Button openScreenButton(Component p_344129_, Supplier<Screen> p_342943_) {
      return Button.builder(p_344129_, p_344387_ -> this.minecraft.setScreen(p_342943_.get())).build();
   }
}
