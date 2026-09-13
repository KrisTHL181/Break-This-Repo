package dev.miru.gui.screens.config;

import dev.miru.gui.screens.FaqScreen;
import dev.miru.gui.screens.MessageScreen;
import dev.miru.helper.FaqItem;
import dev.miru.helper.GuiLayoutHelper;
import dev.miru.helper.KitUtil;
import dev.miru.main.ModMain;
import dev.miru.options.TppSettings;
import dev.miru.options.base.Vec3Options;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TerrainArgumentTriggerScreen extends Screen {
   private final Screen parent;
   private EditBox inputBoxScaleX;
   private EditBox inputBoxScaleY;
   private EditBox inputBoxScaleZ;
   private EditBox inputBoxOffsetX;
   private EditBox inputBoxOffsetY;
   private EditBox inputBoxOffsetZ;
   private EditBox inputBoxAmplitude;
   private EditBox inputBoxFrequency;
   private EditBox inputBoxLerpRate;
   private EditBox inputBoxWrapDiv;
   private EditBox inputBoxMinNoiseDivision;
   private EditBox inputBoxMaxNoiseDivision;
   private Button btnSave;
   private Button btnCancel;
   private Button btnDetails;
   private TppSettings options;
   private final GuiLayoutHelper layoutHelper = new GuiLayoutHelper(this.minecraft);
   private static final int BTN_WIDTH = 300;
   private static final int BTN_WIDTH_HALF = 145;
   private static final int BTN_HEIGHT = 20;
   private static final int COLOR_TITLE = -1;
   private static final int COLOR_SUBTITLE = -5592406;

   public TerrainArgumentTriggerScreen(Screen parent, TppSettings options) {
      super(Component.literal(ModMain.getI18N("title.terrain.arguments")));
      this.parent = parent;
      this.options = options;
   }

   @Override
   public void render(GuiGraphics gg, int mx, int my, float t) {
      super.render(gg, mx, my, t);
      gg.drawCenteredString(this.font, ModMain.getI18N("title.terrain.arguments"), this.layoutHelper.centerX(), 20, -1);
      gg.drawCenteredString(this.font, ModMain.getI18N("subtitle.noise.scale"), this.layoutHelper.centerX(), 35, -5592406);
      gg.drawCenteredString(this.font, ModMain.getI18N("subtitle.noise.offset"), this.layoutHelper.centerX(), 135, -5592406);
      gg.drawCenteredString(this.font, ModMain.getI18N("options.advanced_arguments.title"), this.layoutHelper.centerX(), 235, -5592406);
   }

   @Override
   protected void init() {
      super.init();
      this.clearWidgets();
      int left = this.layoutHelper.centerXOffset(-150);
      int right = this.layoutHelper.centerXOffset(5);
      this.inputBoxScaleX = KitUtil.editbox(
         this.font,
         String.valueOf(this.options.getScalerX()),
         Component.literal(ModMain.getI18N("options.noise.scale", ModMain.getI18N("common.axis.x"))),
         300,
         20,
         left,
         50
      );
      this.inputBoxScaleY = KitUtil.editbox(
         this.font,
         String.valueOf(this.options.getScalerY()),
         Component.literal(ModMain.getI18N("options.noise.scale", ModMain.getI18N("common.axis.y"))),
         300,
         20,
         left,
         80
      );
      this.inputBoxScaleZ = KitUtil.editbox(
         this.font,
         String.valueOf(this.options.getScalerZ()),
         Component.literal(ModMain.getI18N("options.noise.scale", ModMain.getI18N("common.axis.z"))),
         300,
         20,
         left,
         110
      );
      this.inputBoxOffsetX = KitUtil.editbox(
         this.font,
         String.valueOf(this.options.getOffsetX()),
         Component.literal(ModMain.getI18N("options.noise.offset", ModMain.getI18N("common.axis.x"))),
         300,
         20,
         left,
         150
      );
      this.inputBoxOffsetY = KitUtil.editbox(
         this.font,
         String.valueOf(this.options.getOffsetY()),
         Component.literal(ModMain.getI18N("options.noise.offset", ModMain.getI18N("common.axis.y"))),
         300,
         20,
         left,
         180
      );
      this.inputBoxOffsetZ = KitUtil.editbox(
         this.font,
         String.valueOf(this.options.getOffsetZ()),
         Component.literal(ModMain.getI18N("options.noise.offset", ModMain.getI18N("common.axis.z"))),
         300,
         20,
         left,
         210
      );
      this.inputBoxAmplitude = KitUtil.editbox(
         this.font, String.valueOf(this.options.getWgen_amplitudeValue()), Component.literal(ModMain.getI18N("options.amplitude.title")), 145, 20, left, 250
      );
      this.inputBoxFrequency = KitUtil.editbox(
         this.font, String.valueOf(this.options.getWgen_frequencyValue()), Component.literal(ModMain.getI18N("options.frequency.title")), 145, 20, right, 250
      );
      this.inputBoxLerpRate = KitUtil.editbox(
         this.font, String.valueOf(this.options.getWgen_lerpRateValue()), Component.literal(ModMain.getI18N("options.lerp.rate.title")), 145, 20, left, 280
      );
      this.inputBoxWrapDiv = KitUtil.editbox(
         this.font,
         String.valueOf(this.options.getWgen_wrapDivisionValue()),
         Component.literal(ModMain.getI18N("options.wrap.division.title")),
         145,
         20,
         right,
         280
      );
      this.inputBoxMinNoiseDivision = KitUtil.editbox(
         this.font,
         String.valueOf(this.options.getWgen_minLimitNoiseCounterDivisionValue()),
         Component.literal(ModMain.getI18N("screen.terrain_arguments.min_noise_counter_division")),
         145,
         20,
         left,
         310
      );
      this.inputBoxMaxNoiseDivision = KitUtil.editbox(
         this.font,
         String.valueOf(this.options.getWgen_maxLimitNoiseCounterDivisionValue()),
         Component.literal(ModMain.getI18N("screen.terrain_arguments.max_noise_counter_division")),
         145,
         20,
         right,
         310
      );
      Predicate<String> doubleValidator = s -> {
         try {
            Double.parseDouble(s);
            return true;
         } catch (Exception e) {
            return false;
         }
      };

      for (EditBox editBox : new EditBox[]{
         this.inputBoxScaleX,
         this.inputBoxScaleY,
         this.inputBoxScaleZ,
         this.inputBoxOffsetX,
         this.inputBoxOffsetY,
         this.inputBoxOffsetZ,
         this.inputBoxAmplitude,
         this.inputBoxFrequency,
         this.inputBoxLerpRate,
         this.inputBoxWrapDiv,
         this.inputBoxMinNoiseDivision,
         this.inputBoxMaxNoiseDivision
      }) {
         editBox.setFilter(doubleValidator);
      }

      this.updateInputBox();
      this.btnSave = KitUtil.button(Component.literal(ModMain.getI18N("options.save")), null, b -> this.saveSettings(), 145, 20, left, this.height - 30);
      this.btnCancel = KitUtil.button(Component.literal(ModMain.getI18N("options.cancel")), null, b -> this.onClose(), 145, 20, right, this.height - 30);
      this.btnDetails = KitUtil.button(
         Component.literal(ModMain.getI18N("faq.terrain.title")),
         null,
         b -> this.minecraft
            .setScreen(
               new FaqScreen(
                  this,
                  List.of(
                     new FaqItem(ModMain.getI18N("faq.terrain.args.scale_offset.title"), ModMain.getI18N("faq.terrain.args.scale_offset.desc")),
                     new FaqItem(ModMain.getI18N("faq.terrain.args.frequency.title"), ModMain.getI18N("faq.terrain.args.frequency.desc")),
                     new FaqItem(ModMain.getI18N("faq.terrain.args.amplitude.title"), ModMain.getI18N("faq.terrain.args.amplitude.desc")),
                     new FaqItem(ModMain.getI18N("faq.terrain.args.lerp.rate.title"), ModMain.getI18N("faq.terrain.args.lerp.rate.desc")),
                     new FaqItem(ModMain.getI18N("faq.terrain.args.wrapper.division.title"), ModMain.getI18N("faq.terrain.args.wrapper.division.desc")),
                     new FaqItem(ModMain.getI18N("faq.terrain.args.limit.noise.division.title"), ModMain.getI18N("faq.terrain.args.limit.noise.division.desc"))
                  ),
                  20,
                  0,
                  200,
                  20,
                  false
               )
            ),
         300,
         20,
         left,
         this.height - 60
      );
      this.layoutHelper
         .addAllWidgets(
            this.btnSave,
            this.btnCancel,
            this.btnDetails,
            this.inputBoxScaleX,
            this.inputBoxScaleY,
            this.inputBoxScaleZ,
            this.inputBoxOffsetX,
            this.inputBoxOffsetY,
            this.inputBoxOffsetZ,
            this.inputBoxAmplitude,
            this.inputBoxFrequency,
            this.inputBoxLerpRate,
            this.inputBoxWrapDiv,
            this.inputBoxMinNoiseDivision,
            this.inputBoxMaxNoiseDivision
         );
   }

   @Override
   public void onClose() {
      if (this.saveSettings()) {
         this.minecraft.setScreen(this.parent);
      }
   }

   private boolean saveSettings() {
      try {
         this.options
            .setScaler(
               new Vec3Options<>(
                  Double.parseDouble(this.inputBoxScaleX.getValue()),
                  Double.parseDouble(this.inputBoxScaleY.getValue()),
                  Double.parseDouble(this.inputBoxScaleZ.getValue())
               )
            );
         this.options
            .setOffset(
               new Vec3Options<>(
                  Double.parseDouble(this.inputBoxOffsetX.getValue()),
                  Double.parseDouble(this.inputBoxOffsetY.getValue()),
                  Double.parseDouble(this.inputBoxOffsetZ.getValue())
               )
            );
         this.options.setWgen_amplitude(Double.parseDouble(this.inputBoxAmplitude.getValue()));
         this.options.setWgen_frequency(Double.parseDouble(this.inputBoxFrequency.getValue()));
         this.options.setWgen_lerpRate(Double.parseDouble(this.inputBoxLerpRate.getValue()));
         this.options.setWgen_wrapDivision(Double.parseDouble(this.inputBoxWrapDiv.getValue()));
         this.options.setWgen_minLimitNoiseCounterDivision(Double.parseDouble(this.inputBoxMinNoiseDivision.getValue()));
         this.options.setWgen_maxLimitNoiseCounterDivision(Double.parseDouble(this.inputBoxMaxNoiseDivision.getValue()));
         ModMain.writeSettings(this.options);
         return true;
      } catch (Exception e) {
         this.minecraft.setScreen(new MessageScreen(ModMain.getI18N("screen.exception_throw_test.error.caught", e), this));
         return false;
      }
   }

   private void updateInputBox() {
      for (EditBox editBox : new EditBox[]{this.inputBoxScaleX, this.inputBoxScaleY, this.inputBoxScaleZ}) {
         editBox.setEditable(this.options.getWgen_modifyModeValue().allowInputScale);
      }

      for (EditBox editBox : new EditBox[]{this.inputBoxOffsetX, this.inputBoxOffsetY, this.inputBoxOffsetZ}) {
         editBox.setEditable(this.options.getWgen_modifyModeValue().allowInputOffset);
      }

      this.inputBoxLerpRate.setEditable(this.options.getWgen_lerpModeValue().allowInputRate);
      this.inputBoxWrapDiv.setEditable(this.options.getWgen_wrapModeValue().allowInputDiv);
   }

   @Override
   public void tick() {
      super.tick();
   }
}
