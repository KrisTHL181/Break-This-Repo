package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.Heightmap;

public class HeightmapTypeArgument extends StringRepresentableArgument<Heightmap.Types> {
   private static final Codec<Heightmap.Types> LOWER_CASE_CODEC = StringRepresentable.fromEnumWithMapping(
      HeightmapTypeArgument::keptTypes, p_275334_ -> p_275334_.toLowerCase(Locale.ROOT)
   );

   private static Heightmap.Types[] keptTypes() {
      return Arrays.stream(Heightmap.Types.values()).filter(Heightmap.Types::keepAfterWorldgen).toArray(Heightmap.Types[]::new);
   }

   private HeightmapTypeArgument() {
      super(LOWER_CASE_CODEC, HeightmapTypeArgument::keptTypes);
   }

   public static HeightmapTypeArgument heightmap() {
      return new HeightmapTypeArgument();
   }

   public static Heightmap.Types getHeightmap(CommandContext<CommandSourceStack> p_275445_, String p_275449_) {
      return (Heightmap.Types)p_275445_.getArgument(p_275449_, Heightmap.Types.class);
   }

   @Override
   protected String convertId(String p_275666_) {
      return p_275666_.toLowerCase(Locale.ROOT);
   }
}
