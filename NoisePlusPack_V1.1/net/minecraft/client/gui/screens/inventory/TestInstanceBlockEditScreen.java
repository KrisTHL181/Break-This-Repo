package net.minecraft.client.gui.screens.inventory;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.FittingMultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TestInstanceBlockEditScreen extends Screen {
   private static final Component ID_LABEL = Component.translatable("test_instance_block.test_id");
   private static final Component SIZE_LABEL = Component.translatable("test_instance_block.size");
   private static final Component INCLUDE_ENTITIES_LABEL = Component.translatable("test_instance_block.entities");
   private static final Component ROTATION_LABEL = Component.translatable("test_instance_block.rotation");
   private static final int BUTTON_PADDING = 8;
   private static final int WIDTH = 316;
   private final TestInstanceBlockEntity blockEntity;
   private @Nullable EditBox idEdit;
   private @Nullable EditBox sizeXEdit;
   private @Nullable EditBox sizeYEdit;
   private @Nullable EditBox sizeZEdit;
   private @Nullable FittingMultiLineTextWidget infoWidget;
   private @Nullable Button saveButton;
   private @Nullable Button exportButton;
   private @Nullable CycleButton<Boolean> includeEntitiesButton;
   private @Nullable CycleButton<Rotation> rotationButton;

   public TestInstanceBlockEditScreen(TestInstanceBlockEntity p_396495_) {
      super(p_396495_.getBlockState().getBlock().getName());
      this.blockEntity = p_396495_;
   }

   @Override
   protected void init() {
      int i = this.width / 2 - 158;
      boolean flag = SharedConstants.IS_RUNNING_IN_IDE;
      int j = flag ? 3 : 2;
      int k = widgetSize(j);
      this.idEdit = new EditBox(this.font, i, 40, 316, 20, Component.translatable("test_instance_block.test_id"));
      this.idEdit.setMaxLength(128);
      Optional<ResourceKey<GameTestInstance>> optional = this.blockEntity.test();
      if (optional.isPresent()) {
         this.idEdit.setValue(optional.get().identifier().toString());
      }

      this.idEdit.setResponder(p_391321_ -> this.updateTestInfo(false));
      this.addRenderableWidget(this.idEdit);
      this.infoWidget = new FittingMultiLineTextWidget(i, 70, 316, 72, Component.literal(""), this.font);
      this.addRenderableWidget(this.infoWidget);
      Vec3i vec3i = this.blockEntity.getSize();
      int l = 0;
      this.sizeXEdit = new EditBox(this.font, this.widgetX(l++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.x"));
      this.sizeXEdit.setMaxLength(15);
      this.addRenderableWidget(this.sizeXEdit);
      this.sizeYEdit = new EditBox(this.font, this.widgetX(l++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.y"));
      this.sizeYEdit.setMaxLength(15);
      this.addRenderableWidget(this.sizeYEdit);
      this.sizeZEdit = new EditBox(this.font, this.widgetX(l++, 5), 160, widgetSize(5), 20, Component.translatable("structure_block.size.z"));
      this.sizeZEdit.setMaxLength(15);
      this.addRenderableWidget(this.sizeZEdit);
      this.setSize(vec3i);
      this.rotationButton = this.addRenderableWidget(
         CycleButton.builder(TestInstanceBlockEditScreen::rotationDisplay, this.blockEntity.getRotation())
            .withValues(Rotation.values())
            .displayOnlyValue()
            .create(this.widgetX(l++, 5), 160, widgetSize(5), 20, ROTATION_LABEL, (p_392108_, p_391849_) -> this.updateSaveState())
      );
      this.includeEntitiesButton = this.addRenderableWidget(
         CycleButton.onOffBuilder(!this.blockEntity.ignoreEntities())
            .displayOnlyValue()
            .create(this.widgetX(l++, 5), 160, widgetSize(5), 20, INCLUDE_ENTITIES_LABEL)
      );
      l = 0;
      this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.reset"), p_396439_ -> {
         this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.RESET);
         this.minecraft.setScreen(null);
      }).bounds(this.widgetX(l++, j), 185, k, 20).build());
      this.saveButton = this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.save"), p_396396_ -> {
         this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.SAVE);
         this.minecraft.setScreen(null);
      }).bounds(this.widgetX(l++, j), 185, k, 20).build());
      if (flag) {
         this.exportButton = this.addRenderableWidget(Button.builder(Component.literal("Export Structure"), p_391232_ -> {
            this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.EXPORT);
            this.minecraft.setScreen(null);
         }).bounds(this.widgetX(l++, j), 185, k, 20).build());
      }

      this.addRenderableWidget(Button.builder(Component.translatable("test_instance.action.run"), p_392804_ -> {
         this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.RUN);
         this.minecraft.setScreen(null);
      }).bounds(this.widgetX(0, 3), 210, widgetSize(3), 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_394394_ -> this.onDone()).bounds(this.widgetX(1, 3), 210, widgetSize(3), 20).build());
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, p_395297_ -> this.onCancel()).bounds(this.widgetX(2, 3), 210, widgetSize(3), 20).build()
      );
      this.updateTestInfo(true);
   }

   private void updateSaveState() {
      boolean flag = this.rotationButton.getValue() == Rotation.NONE && Identifier.tryParse(this.idEdit.getValue()) != null;
      this.saveButton.active = flag;
      if (this.exportButton != null) {
         this.exportButton.active = flag;
      }
   }

   private static Component rotationDisplay(Rotation p_395172_) {
      return Component.literal(switch (p_395172_) {
         case NONE -> "0";
         case CLOCKWISE_90 -> "90";
         case CLOCKWISE_180 -> "180";
         case COUNTERCLOCKWISE_90 -> "270";
      });
   }

   private void setSize(Vec3i p_391542_) {
      this.sizeXEdit.setValue(Integer.toString(p_391542_.getX()));
      this.sizeYEdit.setValue(Integer.toString(p_391542_.getY()));
      this.sizeZEdit.setValue(Integer.toString(p_391542_.getZ()));
   }

   private int widgetX(int p_393294_, int p_396760_) {
      int i = this.width / 2 - 158;
      float f = exactWidgetSize(p_396760_);
      return (int)(i + p_393294_ * (8.0F + f));
   }

   private static int widgetSize(int p_397108_) {
      return (int)exactWidgetSize(p_397108_);
   }

   private static float exactWidgetSize(int p_392601_) {
      return (float)(316 - (p_392601_ - 1) * 8) / p_392601_;
   }

   @Override
   public void render(GuiGraphics p_395173_, int p_394172_, int p_391737_, float p_396675_) {
      super.render(p_395173_, p_394172_, p_391737_, p_396675_);
      int i = this.width / 2 - 158;
      p_395173_.drawCenteredString(this.font, this.title, this.width / 2, 10, -1);
      p_395173_.drawString(this.font, ID_LABEL, i, 30, -6250336);
      p_395173_.drawString(this.font, SIZE_LABEL, i, 150, -6250336);
      p_395173_.drawString(this.font, ROTATION_LABEL, this.rotationButton.getX(), 150, -6250336);
      p_395173_.drawString(this.font, INCLUDE_ENTITIES_LABEL, this.includeEntitiesButton.getX(), 150, -6250336);
   }

   private void updateTestInfo(boolean p_394168_) {
      boolean flag = this.sendToServer(p_394168_ ? ServerboundTestInstanceBlockActionPacket.Action.INIT : ServerboundTestInstanceBlockActionPacket.Action.QUERY);
      if (!flag) {
         this.infoWidget.setMessage(Component.translatable("test_instance.description.invalid_id").withStyle(ChatFormatting.RED));
      }

      this.updateSaveState();
   }

   private void onDone() {
      this.sendToServer(ServerboundTestInstanceBlockActionPacket.Action.SET);
      this.onClose();
   }

   private boolean sendToServer(ServerboundTestInstanceBlockActionPacket.Action p_392487_) {
      Optional<Identifier> optional = Optional.ofNullable(Identifier.tryParse(this.idEdit.getValue()));
      Optional<ResourceKey<GameTestInstance>> optional1 = optional.map(p_448057_ -> ResourceKey.create(Registries.TEST_INSTANCE, p_448057_));
      Vec3i vec3i = new Vec3i(parseSize(this.sizeXEdit.getValue()), parseSize(this.sizeYEdit.getValue()), parseSize(this.sizeZEdit.getValue()));
      boolean flag = !this.includeEntitiesButton.getValue();
      this.minecraft
         .getConnection()
         .send(new ServerboundTestInstanceBlockActionPacket(this.blockEntity.getBlockPos(), p_392487_, optional1, vec3i, this.rotationButton.getValue(), flag));
      return optional.isPresent();
   }

   public void setStatus(Component p_391305_, Optional<Vec3i> p_396967_) {
      MutableComponent mutablecomponent = Component.empty();
      this.blockEntity
         .errorMessage()
         .ifPresent(
            p_391230_ -> mutablecomponent.append(
                  Component.translatable("test_instance.description.failed", Component.empty().withStyle(ChatFormatting.RED).append(p_391230_))
               )
               .append("\n\n")
         );
      mutablecomponent.append(p_391305_);
      this.infoWidget.setMessage(mutablecomponent);
      p_396967_.ifPresent(this::setSize);
   }

   private void onCancel() {
      this.onClose();
   }

   private static int parseSize(String p_396257_) {
      try {
         return Mth.clamp(Integer.parseInt(p_396257_), 1, 48);
      } catch (NumberFormatException numberformatexception) {
         return 1;
      }
   }

   @Override
   public boolean isInGameUi() {
      return true;
   }
}
