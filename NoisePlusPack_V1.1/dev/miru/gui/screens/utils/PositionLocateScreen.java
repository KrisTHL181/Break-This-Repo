package dev.miru.gui.screens.utils;

import dev.miru.gui.screens.MessageScreen;
import dev.miru.helper.KitUtil;
import dev.miru.localization.TppTranslateManager;
import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import dev.miru.options.modes.NoiseModifyMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PositionLocateScreen extends Screen {
   private Screen parent;
   private TppSettings tppSettings;
   private String previewString = "(--, --, --)";
   private boolean applyToOriginal = true;

   public PositionLocateScreen(Screen parent, TppSettings tppSettings) {
      super(Component.literal(ModMain.getI18N("screen.position_locator.title")));
      this.parent = parent;
      this.tppSettings = tppSettings;
   }

   @Override
   public void init() {
      EditBox posInputX = KitUtil.editbox(
         this.font, "", Component.literal(ModMain.getI18N("screen.position_locator.input.x.hint")), 200, 20, this.width / 2 - 100, this.height / 2 - 100
      );
      EditBox posInputY = KitUtil.editbox(
         this.font, "", Component.literal(ModMain.getI18N("screen.position_locator.input.y.hint")), 200, 20, this.width / 2 - 100, this.height / 2 - 75
      );
      EditBox posInputZ = KitUtil.editbox(
         this.font, "", Component.literal(ModMain.getI18N("screen.position_locator.input.z.hint")), 200, 20, this.width / 2 - 100, this.height / 2 - 50
      );
      Button modeBtn = KitUtil.button(Component.literal(this.getApplyDesc()), Component.empty(), btn -> {
         this.applyToOriginal = !this.applyToOriginal;
         btn.setMessage(Component.literal(this.getApplyDesc()));
      }, 200, 20, this.width / 2 - 100, this.height / 2 - 125);
      Button actionBtn = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.position_locator.get_terrain_position")),
         Component.literal(ModMain.getI18N("screen.position_locator.get_terrain_position.hint")),
         btn -> {
            try {
               double valueX = Double.parseDouble(posInputX.getValue());
               double valueY = Double.parseDouble(posInputY.getValue());
               double valueZ = Double.parseDouble(posInputZ.getValue());
               NoiseModifyMode noiseModifyValue = this.tppSettings.getWgen_modifyModeValue();
               if (this.applyToOriginal) {
                  double revX = noiseModifyValue.reverseApply(valueX, this.tppSettings.getScalerX(), this.tppSettings.getOffsetX());
                  double revY = noiseModifyValue.reverseApply(valueY, this.tppSettings.getScalerY(), this.tppSettings.getOffsetY());
                  double revZ = noiseModifyValue.reverseApply(valueZ, this.tppSettings.getScalerZ(), this.tppSettings.getOffsetZ());
                  this.previewString = "(%s, %s, %s)".formatted(revX, revY, revZ);
               } else {
                  double modX = noiseModifyValue.applyFunction.apply(valueX, this.tppSettings.getScalerX(), this.tppSettings.getOffsetZ());
                  double modY = noiseModifyValue.applyFunction.apply(valueY, this.tppSettings.getScalerY(), this.tppSettings.getOffsetY());
                  double modZ = noiseModifyValue.applyFunction.apply(valueZ, this.tppSettings.getScalerZ(), this.tppSettings.getOffsetZ());
                  this.previewString = "(%s, %s, %s)".formatted(modX, modY, modZ);
               }
            } catch (NumberFormatException numberFormatException) {
               this.minecraft.setScreen(new MessageScreen("Error parsing value : %s".formatted(numberFormatException), this));
            }
         },
         200,
         20,
         this.width / 2 - 100,
         this.height / 2 - 20
      );
      Button backBtn = KitUtil.button(
         Component.literal(ModMain.getI18N("screen.position_locator.back")),
         Component.empty(),
         btn -> this.minecraft.setScreen(this.parent),
         200,
         20,
         this.width / 2 - 100,
         this.height / 2 + 5
      );
      this.addRenderableWidget(modeBtn);
      this.addRenderableWidget(posInputX);
      this.addRenderableWidget(posInputY);
      this.addRenderableWidget(posInputZ);
      this.addRenderableWidget(actionBtn);
      this.addRenderableWidget(backBtn);
   }

   @Override
   public void render(GuiGraphics guiGraphics, int mx, int my, float d) {
      super.render(guiGraphics, mx, my, d);
      guiGraphics.fill(this.width / 2 - 100, this.height / 2 + 40, this.width / 2 + 100, this.height / 2 + 60, -16777216);
      guiGraphics.drawCenteredString(this.font, this.previewString, this.width / 2, this.height / 2 + 50, -1);
   }

   private String getApplyDesc() {
      TppTranslateManager translateManager = ModMain.getTranslateManager();
      return translateManager.getI18N(
         "screen.position_locator.apply.desc",
         this.applyToOriginal
            ? translateManager.getI18N("screen.position_locator.apply.get_original")
            : translateManager.getI18N("screen.position_locator.apply.get_modified")
      );
   }
}
