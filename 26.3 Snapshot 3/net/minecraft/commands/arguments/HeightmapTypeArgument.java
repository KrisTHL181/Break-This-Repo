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
      HeightmapTypeArgument::keptTypes, s -> s.toLowerCase(Locale.ROOT)
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

   public static Heightmap.Types getHeightmap(final CommandContext<CommandSourceStack> context, final String name) {
      return (Heightmap.Types)context.getArgument(name, Heightmap.Types.class);
   }

   @Override
   protected String convertId(final String id) {
      return id.toLowerCase(Locale.ROOT);
   }
}
