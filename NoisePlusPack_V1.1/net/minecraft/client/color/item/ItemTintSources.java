package net.minecraft.client.color.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemTintSources {
   private static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper<>();
   public static final Codec<ItemTintSource> CODEC = ID_MAPPER.codec(Identifier.CODEC).dispatch(ItemTintSource::type, p_378183_ -> p_378183_);

   public static void bootstrap() {
      ID_MAPPER.put(Identifier.withDefaultNamespace("custom_model_data"), CustomModelDataSource.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("constant"), Constant.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("dye"), Dye.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("grass"), GrassColorSource.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("firework"), Firework.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("potion"), Potion.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("map_color"), MapColor.MAP_CODEC);
      ID_MAPPER.put(Identifier.withDefaultNamespace("team"), TeamColor.MAP_CODEC);
   }
}
