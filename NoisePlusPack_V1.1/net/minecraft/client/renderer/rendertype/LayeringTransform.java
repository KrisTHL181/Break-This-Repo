package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Consumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LayeringTransform {
   private final String name;
   private final @Nullable Consumer<Matrix4fStack> modifier;
   public static final LayeringTransform NO_LAYERING = new LayeringTransform("no_layering", null);
   public static final LayeringTransform VIEW_OFFSET_Z_LAYERING = new LayeringTransform(
      "view_offset_z_layering", p_455029_ -> RenderSystem.getProjectionType().applyLayeringTransform(p_455029_, 1.0F)
   );
   public static final LayeringTransform VIEW_OFFSET_Z_LAYERING_FORWARD = new LayeringTransform(
      "view_offset_z_layering_forward", p_454468_ -> RenderSystem.getProjectionType().applyLayeringTransform(p_454468_, -1.0F)
   );

   public LayeringTransform(String p_450167_, @Nullable Consumer<Matrix4fStack> p_455646_) {
      this.name = p_450167_;
      this.modifier = p_455646_;
   }

   @Override
   public String toString() {
      return "LayeringTransform[" + this.name + "]";
   }

   public @Nullable Consumer<Matrix4fStack> getModifier() {
      return this.modifier;
   }
}
