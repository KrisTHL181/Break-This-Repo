package net.minecraft.client.renderer.blockentity.state;

import com.mojang.math.Transformation;
import net.minecraft.world.level.block.entity.SignText;
import org.jspecify.annotations.Nullable;

public class SignRenderState extends BlockEntityRenderState {
   public @Nullable SignText frontText;
   public @Nullable SignText backText;
   public int textLineHeight;
   public int maxTextLineWidth;
   public boolean isTextFilteringEnabled;
   public boolean drawOutline;
   public SignRenderState.SignTransformations transformations = SignRenderState.SignTransformations.IDENTITY;

   public record SignTransformations(Transformation frontText, Transformation backText) {
      public static final SignRenderState.SignTransformations IDENTITY = new SignRenderState.SignTransformations(
         Transformation.IDENTITY, Transformation.IDENTITY
      );
   }
}
