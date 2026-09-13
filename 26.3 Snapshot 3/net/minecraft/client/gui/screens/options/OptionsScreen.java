package net.minecraft.client.gui.screens.options;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;

public class OptionsScreen extends Screen implements HasGamemasterPermissionReaction {
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
   private final boolean inWorld;

   public OptionsScreen(final Screen lastScreen, final Options options, final boolean inWorld) {
      super(TITLE);
      this.lastScreen = lastScreen;
      this.options = options;
      this.inWorld = inWorld;
   }

   @Override
   protected void init() {
      LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(8));
      header.addChild(new StringWidget(TITLE, this.font), LayoutSettings::alignHorizontallyCenter);
      LinearLayout subHeader = header.addChild(LinearLayout.horizontal()).spacing(8);
      subHeader.addChild(this.options.fov().createButton(this.minecraft.options));
      if (this.inWorld) {
         subHeader.addChild(
            Button.builder(
                  Component.translatable("options.worldOptions.button"),
                  var1x -> this.minecraft.gui.setScreen(new WorldOptionsScreen(this, Objects.requireNonNull(this.minecraft.level)))
               )
               .build()
         );
      } else {
         subHeader.addChild(this.createOnlineButton());
      }

      GridLayout gridLayout = new GridLayout();
      gridLayout.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
      GridLayout.RowHelper helper = gridLayout.createRowHelper(2);
      helper.addChild(this.openScreenButton(SKIN_CUSTOMIZATION, () -> new SkinCustomizationScreen(this, this.options)));
      helper.addChild(this.openScreenButton(SOUNDS, () -> new SoundOptionsScreen(this, this.options)));
      helper.addChild(this.openScreenButton(VIDEO, () -> new VideoSettingsScreen(this, this.minecraft, this.options)));
      helper.addChild(this.openScreenButton(CONTROLS, () -> new ControlsScreen(this, this.options)));
      helper.addChild(this.openScreenButton(LANGUAGE, () -> new LanguageSelectScreen(this, this.options, this.minecraft.getLanguageManager())));
      helper.addChild(this.openScreenButton(CHAT, () -> new ChatOptionsScreen(this, this.options)));
      helper.addChild(
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
      helper.addChild(this.openScreenButton(ACCESSIBILITY, () -> new AccessibilityOptionsScreen(this, this.options)));
      Button telemetryButton = helper.addChild(this.openScreenButton(TELEMETRY, () -> new TelemetryInfoScreen(this, this.options)));
      if (!this.minecraft.allowsTelemetry()) {
         telemetryButton.active = false;
         telemetryButton.setTooltip(TELEMETRY_DISABLED_TOOLTIP);
      }

      helper.addChild(this.openScreenButton(CREDITS_AND_ATTRIBUTION, () -> new CreditsAndAttributionScreen(this)));
      this.layout.addToContents(gridLayout);
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());
      this.layout.visitWidgets(x$0 -> this.addRenderableWidget(x$0));
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.lastScreen);
   }

   public Screen getLastScreen() {
      return this.lastScreen;
   }

   private void applyPacks(final PackRepository packRepository) {
      this.options.updateResourcePacks(packRepository);
      this.minecraft.gui.setScreen(this);
   }

   private LayoutElement createOnlineButton() {
      return Button.builder(Component.translatable("options.online"), var1 -> this.minecraft.gui.setScreen(new OnlineOptionsScreen(this, this.options)))
         .bounds(this.width / 2 + 5, this.height / 6 - 12 + 24, 150, 20)
         .build();
   }

   @Override
   public void removed() {
      this.options.save();
   }

   private Button openScreenButton(final Component message, final Supplier<Screen> screenToScreen) {
      return Button.builder(message, var2 -> this.minecraft.gui.setScreen(screenToScreen.get())).build();
   }

   @Override
   public void onGamemasterPermissionChanged(final boolean hasGamemasterPermission) {
      this.minecraft.gui.setScreen(new OptionsScreen(this.lastScreen, this.minecraft.options, true));
   }
}
