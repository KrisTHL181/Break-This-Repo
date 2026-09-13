package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.PlainSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class SignEditScreen extends AbstractSignEditScreen {
   public static final float MAGIC_BACKGROUND_SCALE = 3.9F;
   public static final float MAGIC_TEXT_SCALE = 0.9765628F;
   private static final int TEXTURE_WIDTH = 24;
   private static final int TEXTURE_HEIGHT = 26;
   private static final int POST_HEIGHT = 14;
   private static final Vector3fc TEXT_SCALE = new Vector3f(0.9765628F, 0.9765628F, 0.9765628F);
   private final int displayedHeight;
   private final Identifier texture = Identifier.withDefaultNamespace("textures/gui/signs/" + this.woodType.name() + ".png");

   public SignEditScreen(final SignBlockEntity sign, final boolean isFrontText, final boolean shouldFilter) {
      super(sign, isFrontText, shouldFilter);
      boolean isWallSign = PlainSignBlock.getAttachmentPoint(sign.getBlockState()) == PlainSignBlock.Attachment.WALL;
      this.displayedHeight = isWallSign ? 12 : 26;
   }

   @Override
   protected float getSignYOffset() {
      return 90.0F;
   }

   @Override
   protected void extractSignBackground(final GuiGraphicsExtractor graphics) {
      graphics.pose().translate(0.0F, 27.0F);
      graphics.pose().scale(3.9F, 3.9F);
      graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, -12, -13, 0.0F, 0.0F, 24, this.displayedHeight, 24, 26);
   }

   @Override
   protected Vector3fc getSignTextScale() {
      return TEXT_SCALE;
   }
}
