package net.minecraft.world.item.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

public final class TypedEntityData<IdType> implements TooltipProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TYPE_TAG = "id";
   final IdType type;
   final CompoundTag tag;

   public static <T> Codec<TypedEntityData<T>> codec(final Codec<T> p_429781_) {
      return new Codec<TypedEntityData<T>>() {
         public <V> DataResult<Pair<TypedEntityData<T>, V>> decode(DynamicOps<V> p_422773_, V p_425689_) {
            return CustomData.COMPOUND_TAG_CODEC
               .decode(p_422773_, p_425689_)
               .flatMap(
                  p_449827_ -> {
                     CompoundTag compoundtag = ((CompoundTag)p_449827_.getFirst()).copy();
                     Tag tag = compoundtag.remove("id");
                     return tag == null
                        ? DataResult.error(() -> "Expected 'id' field in " + p_425689_)
                        : p_429781_.parse(asNbtOps((DynamicOps<T>)p_422773_), tag)
                           .map(p_449823_ -> Pair.of(new TypedEntityData<>(p_449823_, compoundtag), p_449827_.getSecond()));
                  }
               );
         }

         public <V> DataResult<V> encode(TypedEntityData<T> p_427035_, DynamicOps<V> p_430611_, V p_427633_) {
            return p_429781_.encodeStart(asNbtOps((DynamicOps<T>)p_430611_), p_427035_.type).flatMap(p_424159_ -> {
               CompoundTag compoundtag = p_427035_.tag.copy();
               compoundtag.put("id", p_424159_);
               return CustomData.COMPOUND_TAG_CODEC.encode(compoundtag, p_430611_, p_427633_);
            });
         }

         private static <T> DynamicOps<Tag> asNbtOps(DynamicOps<T> p_427526_) {
            return (DynamicOps<Tag>)(p_427526_ instanceof RegistryOps<T> registryops ? registryops.withParent(NbtOps.INSTANCE) : NbtOps.INSTANCE);
         }
      };
   }

   public static <B extends ByteBuf, T> StreamCodec<B, TypedEntityData<T>> streamCodec(StreamCodec<B, T> p_428134_) {
      return StreamCodec.composite(
         p_428134_,
         (Function<TypedEntityData<T>, T>)(TypedEntityData::type),
         ByteBufCodecs.COMPOUND_TAG,
         TypedEntityData::tag,
         (BiFunction<T, CompoundTag, TypedEntityData<T>>)(TypedEntityData::new)
      );
   }

   TypedEntityData(IdType p_430628_, CompoundTag p_431545_) {
      this.type = p_430628_;
      this.tag = stripId(p_431545_);
   }

   public static <T> TypedEntityData<T> of(T p_430418_, CompoundTag p_423511_) {
      return new TypedEntityData<>(p_430418_, p_423511_);
   }

   private static CompoundTag stripId(CompoundTag p_427219_) {
      if (p_427219_.contains("id")) {
         CompoundTag compoundtag = p_427219_.copy();
         compoundtag.remove("id");
         return compoundtag;
      } else {
         return p_427219_;
      }
   }

   public IdType type() {
      return this.type;
   }

   public boolean contains(String p_422813_) {
      return this.tag.contains(p_422813_);
   }

   @Override
   public boolean equals(Object p_431357_) {
      if (p_431357_ == this) {
         return true;
      } else {
         return p_431357_ instanceof TypedEntityData<?> typedentitydata ? this.type == typedentitydata.type && this.tag.equals(typedentitydata.tag) : false;
      }
   }

   @Override
   public int hashCode() {
      return 31 * this.type.hashCode() + this.tag.hashCode();
   }

   @Override
   public String toString() {
      return this.type + " " + this.tag;
   }

   public void loadInto(Entity p_428590_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_428590_.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_428590_.registryAccess());
         p_428590_.saveWithoutId(tagvalueoutput);
         CompoundTag compoundtag = tagvalueoutput.buildResult();
         UUID uuid = p_428590_.getUUID();
         compoundtag.merge(this.getUnsafe());
         p_428590_.load(TagValueInput.create(problemreporter$scopedcollector, p_428590_.registryAccess(), compoundtag));
         p_428590_.setUUID(uuid);
      }
   }

   public boolean loadInto(BlockEntity p_428879_, HolderLookup.Provider p_424039_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_428879_.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_424039_);
         p_428879_.saveCustomOnly(tagvalueoutput);
         CompoundTag compoundtag = tagvalueoutput.buildResult();
         CompoundTag compoundtag1 = compoundtag.copy();
         compoundtag.merge(this.getUnsafe());
         if (!compoundtag.equals(compoundtag1)) {
            try {
               p_428879_.loadCustomOnly(TagValueInput.create(problemreporter$scopedcollector, p_424039_, compoundtag));
               p_428879_.setChanged();
               return true;
            } catch (Exception exception1) {
               LOGGER.warn("Failed to apply custom data to block entity at {}", p_428879_.getBlockPos(), exception1);

               try {
                  p_428879_.loadCustomOnly(TagValueInput.create(problemreporter$scopedcollector.forChild(() -> "(rollback)"), p_424039_, compoundtag1));
               } catch (Exception exception) {
                  LOGGER.warn("Failed to rollback block entity at {} after failure", p_428879_.getBlockPos(), exception);
               }
            }
         }

         return false;
      }
   }

   private CompoundTag tag() {
      return this.tag;
   }

   @Deprecated
   public CompoundTag getUnsafe() {
      return this.tag;
   }

   public CompoundTag copyTagWithoutId() {
      return this.tag.copy();
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_423064_, Consumer<Component> p_427900_, TooltipFlag p_426496_, DataComponentGetter p_426173_) {
      if (this.type.getClass() == EntityType.class) {
         EntityType<?> entitytype = (EntityType<?>)this.type;
         if (p_423064_.isPeaceful() && !entitytype.isAllowedInPeaceful()) {
            p_427900_.accept(Component.translatable("item.spawn_egg.peaceful").withStyle(ChatFormatting.RED));
         }
      }
   }
}
