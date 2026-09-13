package net.minecraft.network.chat.contents.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public record EntityDataSource(String selectorPattern, @Nullable EntitySelector compiledSelector) implements DataSource {
   public static final MapCodec<EntityDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_424041_ -> p_424041_.group(Codec.STRING.fieldOf("entity").forGetter(EntityDataSource::selectorPattern)).apply(p_424041_, EntityDataSource::new)
   );

   public EntityDataSource(String p_430549_) {
      this(p_430549_, compileSelector(p_430549_));
   }

   private static @Nullable EntitySelector compileSelector(String p_430438_) {
      try {
         EntitySelectorParser entityselectorparser = new EntitySelectorParser(new StringReader(p_430438_), true);
         return entityselectorparser.parse();
      } catch (CommandSyntaxException commandsyntaxexception) {
         return null;
      }
   }

   @Override
   public Stream<CompoundTag> getData(CommandSourceStack p_429692_) throws CommandSyntaxException {
      if (this.compiledSelector != null) {
         List<? extends Entity> list = this.compiledSelector.findEntities(p_429692_);
         return list.stream().map(NbtPredicate::getEntityTagToCompare);
      } else {
         return Stream.empty();
      }
   }

   @Override
   public MapCodec<EntityDataSource> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return "entity=" + this.selectorPattern;
   }

   @Override
   public boolean equals(Object p_423577_) {
      return this == p_423577_ ? true : p_423577_ instanceof EntityDataSource entitydatasource && this.selectorPattern.equals(entitydatasource.selectorPattern);
   }

   @Override
   public int hashCode() {
      return this.selectorPattern.hashCode();
   }
}
