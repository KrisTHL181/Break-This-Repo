package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SignEditScreen extends AbstractSignEditScreen {
   public static final float MAGIC_SCALE_NUMBER = 62.500004F;
   public static final float MAGIC_TEXT_SCALE = 0.9765628F;
   private static final Vector3f TEXT_SCALE = new Vector3f(0.9765628F, 0.9765628F, 0.9765628F);
   private Model.@Nullable Simple signModel;

   public SignEditScreen(SignBlockEntity p_277919_, boolean p_277579_, boolean p_277693_) {
      super(p_277919_, p_277579_, p_277693_);
   }

   @Override
   protected void init() {
      super.init();
      boolean flag = this.sign.getBlockState().getBlock() instanceof StandingSignBlock;
      this.signModel = SignRenderer.createSignModel(this.minecraft.getEntityModels(), this.woodType, flag);
   }

   @Override
   protected float getSignYOffset() {
      return 90.0F;
   }

   @Override
   protected void renderSignBackground(GuiGraphics p_281440_) {
      if (this.signModel != null) {
         int i = this.width / 2;
         int j = i - 48;
         int k = 66;
         int l = i + 48;
         int i1 = 168;
         p_281440_.submitSignRenderState(this.signModel, 62.500004F, this.woodType, j, 66, l, 168);
      }
   }

   @Override
   protected Vector3f getSignTextScale() {
      return TEXT_SCALE;
   }
}
