package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;

public record StorageValue(Identifier storage, NbtPathArgument.NbtPath path) implements NumberProvider {
   public static final MapCodec<StorageValue> CODEC = RecordCodecBuilder.mapCodec(
      p_450121_ -> p_450121_.group(
            Identifier.CODEC.fieldOf("storage").forGetter(StorageValue::storage), NbtPathArgument.NbtPath.CODEC.fieldOf("path").forGetter(StorageValue::path)
         )
         .apply(p_450121_, StorageValue::new)
   );

   @Override
   public LootNumberProviderType getType() {
      return NumberProviders.STORAGE;
   }

   private Number getNumericTag(LootContext p_329012_, Number p_393342_) {
      CompoundTag compoundtag = p_329012_.getLevel().getServer().getCommandStorage().get(this.storage);

      try {
         List<Tag> list = this.path.get(compoundtag);
         if (list.size() == 1 && list.getFirst() instanceof NumericTag numerictag) {
            return numerictag.box();
         }
      } catch (CommandSyntaxException var7) {
      }

      return p_393342_;
   }

   @Override
   public float getFloat(LootContext p_334554_) {
      return this.getNumericTag(p_334554_, 0.0F).floatValue();
   }

   @Override
   public int getInt(LootContext p_329755_) {
      return this.getNumericTag(p_329755_, 0).intValue();
   }
}
