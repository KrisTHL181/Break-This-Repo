package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class HangingSignEditScreen extends AbstractSignEditScreen {
   public static final float MAGIC_BACKGROUND_SCALE = 4.5F;
   private static final Vector3f TEXT_SCALE = new Vector3f(1.0F, 1.0F, 1.0F);
   private static final int TEXTURE_WIDTH = 16;
   private static final int TEXTURE_HEIGHT = 16;
   private final Identifier texture = Identifier.withDefaultNamespace("textures/gui/hanging_signs/" + this.woodType.name() + ".png");

   public HangingSignEditScreen(SignBlockEntity p_278017_, boolean p_277942_, boolean p_277778_) {
      super(p_278017_, p_277942_, p_277778_, Component.translatable("hanging_sign.edit"));
   }

   @Override
   protected float getSignYOffset() {
      return 125.0F;
   }

   @Override
   protected void renderSignBackground(GuiGraphics p_282580_) {
      p_282580_.pose().translate(0.0F, -13.0F);
      p_282580_.pose().scale(4.5F, 4.5F);
      p_282580_.blit(RenderPipelines.GUI_TEXTURED, this.texture, -8, -8, 0.0F, 0.0F, 16, 16, 16, 16);
   }

   @Override
   protected Vector3f getSignTextScale() {
      return TEXT_SCALE;
   }
}
