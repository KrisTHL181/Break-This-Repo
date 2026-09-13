package net.minecraft.network.syncher;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class EntityDataSerializers {
   private static final CrudeIncrementalIntIdentityHashBiMap<EntityDataSerializer<?>> SERIALIZERS = CrudeIncrementalIntIdentityHashBiMap.create(16);
   public static final EntityDataSerializer<Byte> BYTE = EntityDataSerializer.forValueType(ByteBufCodecs.BYTE);
   public static final EntityDataSerializer<Integer> INT = EntityDataSerializer.forValueType(ByteBufCodecs.VAR_INT);
   public static final EntityDataSerializer<Long> LONG = EntityDataSerializer.forValueType(ByteBufCodecs.VAR_LONG);
   public static final EntityDataSerializer<Float> FLOAT = EntityDataSerializer.forValueType(ByteBufCodecs.FLOAT);
   public static final EntityDataSerializer<String> STRING = EntityDataSerializer.forValueType(ByteBufCodecs.STRING_UTF8);
   public static final EntityDataSerializer<Component> COMPONENT = EntityDataSerializer.forValueType(ComponentSerialization.TRUSTED_STREAM_CODEC);
   public static final EntityDataSerializer<Optional<Component>> OPTIONAL_COMPONENT = EntityDataSerializer.forValueType(
      ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC
   );
   public static final EntityDataSerializer<ItemStack> ITEM_STACK = new EntityDataSerializer<ItemStack>() {
      @Override
      public StreamCodec<? super RegistryFriendlyByteBuf, ItemStack> codec() {
         return ItemStack.OPTIONAL_STREAM_CODEC;
      }

      public ItemStack copy(ItemStack p_238121_) {
         return p_238121_.copy();
      }
   };
   public static final EntityDataSerializer<BlockState> BLOCK_STATE = EntityDataSerializer.forValueType(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY));
   private static final StreamCodec<ByteBuf, Optional<BlockState>> OPTIONAL_BLOCK_STATE_CODEC = new StreamCodec<ByteBuf, Optional<BlockState>>() {
      public void encode(ByteBuf p_329740_, Optional<BlockState> p_331636_) {
         if (p_331636_.isPresent()) {
            VarInt.write(p_329740_, Block.getId(p_331636_.get()));
         } else {
            VarInt.write(p_329740_, 0);
         }
      }

      public Optional<BlockState> decode(ByteBuf p_334256_) {
         int i = VarInt.read(p_334256_);
         return i == 0 ? Optional.empty() : Optional.of(Block.stateById(i));
      }
   };
   public static final EntityDataSerializer<Optional<BlockState>> OPTIONAL_BLOCK_STATE = EntityDataSerializer.forValueType(OPTIONAL_BLOCK_STATE_CODEC);
   public static final EntityDataSerializer<Boolean> BOOLEAN = EntityDataSerializer.forValueType(ByteBufCodecs.BOOL);
   public static final EntityDataSerializer<ParticleOptions> PARTICLE = EntityDataSerializer.forValueType(ParticleTypes.STREAM_CODEC);
   public static final EntityDataSerializer<List<ParticleOptions>> PARTICLES = EntityDataSerializer.forValueType(
      ParticleTypes.STREAM_CODEC.apply(ByteBufCodecs.list())
   );
   public static final EntityDataSerializer<Rotations> ROTATIONS = EntityDataSerializer.forValueType(Rotations.STREAM_CODEC);
   public static final EntityDataSerializer<BlockPos> BLOCK_POS = EntityDataSerializer.forValueType(BlockPos.STREAM_CODEC);
   public static final EntityDataSerializer<Optional<BlockPos>> OPTIONAL_BLOCK_POS = EntityDataSerializer.forValueType(
      BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional)
   );
   public static final EntityDataSerializer<Direction> DIRECTION = EntityDataSerializer.forValueType(Direction.STREAM_CODEC);
   public static final EntityDataSerializer<Optional<EntityReference<LivingEntity>>> OPTIONAL_LIVING_ENTITY_REFERENCE = EntityDataSerializer.forValueType(
      EntityReference.streamCodec().apply(ByteBufCodecs::optional)
   );
   public static final EntityDataSerializer<Optional<GlobalPos>> OPTIONAL_GLOBAL_POS = EntityDataSerializer.forValueType(
      GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional)
   );
   public static final EntityDataSerializer<VillagerData> VILLAGER_DATA = EntityDataSerializer.forValueType(VillagerData.STREAM_CODEC);
   private static final StreamCodec<ByteBuf, OptionalInt> OPTIONAL_UNSIGNED_INT_CODEC = new StreamCodec<ByteBuf, OptionalInt>() {
      public OptionalInt decode(ByteBuf p_428333_) {
         int i = VarInt.read(p_428333_);
         return i == 0 ? OptionalInt.empty() : OptionalInt.of(i - 1);
      }

      public void encode(ByteBuf p_430607_, OptionalInt p_422537_) {
         VarInt.write(p_430607_, p_422537_.orElse(-1) + 1);
      }
   };
   public static final EntityDataSerializer<OptionalInt> OPTIONAL_UNSIGNED_INT = EntityDataSerializer.forValueType(OPTIONAL_UNSIGNED_INT_CODEC);
   public static final EntityDataSerializer<Pose> POSE = EntityDataSerializer.forValueType(Pose.STREAM_CODEC);
   public static final EntityDataSerializer<Holder<CatVariant>> CAT_VARIANT = EntityDataSerializer.forValueType(CatVariant.STREAM_CODEC);
   public static final EntityDataSerializer<Holder<ChickenVariant>> CHICKEN_VARIANT = EntityDataSerializer.forValueType(ChickenVariant.STREAM_CODEC);
   public static final EntityDataSerializer<Holder<CowVariant>> COW_VARIANT = EntityDataSerializer.forValueType(CowVariant.STREAM_CODEC);
   public static final EntityDataSerializer<Holder<WolfVariant>> WOLF_VARIANT = EntityDataSerializer.forValueType(WolfVariant.STREAM_CODEC);
   public static final EntityDataSerializer<Holder<WolfSoundVariant>> WOLF_SOUND_VARIANT = EntityDataSerializer.forValueType(WolfSoundVariant.STREAM_CODEC);
   public static final EntityDataSerializer<Holder<FrogVariant>> FROG_VARIANT = EntityDataSerializer.forValueType(FrogVariant.STREAM_CODEC);
   public static final EntityDataSerializer<Holder<PigVariant>> PIG_VARIANT = EntityDataSerializer.forValueType(PigVariant.STREAM_CODEC);
   public static final EntityDataSerializer<Holder<ZombieNautilusVariant>> ZOMBIE_NAUTILUS_VARIANT = EntityDataSerializer.forValueType(
      ZombieNautilusVariant.STREAM_CODEC
   );
   public static final EntityDataSerializer<Holder<PaintingVariant>> PAINTING_VARIANT = EntityDataSerializer.forValueType(PaintingVariant.STREAM_CODEC);
   public static final EntityDataSerializer<Armadillo.ArmadilloState> ARMADILLO_STATE = EntityDataSerializer.forValueType(Armadillo.ArmadilloState.STREAM_CODEC);
   public static final EntityDataSerializer<Sniffer.State> SNIFFER_STATE = EntityDataSerializer.forValueType(Sniffer.State.STREAM_CODEC);
   public static final EntityDataSerializer<WeatheringCopper.WeatherState> WEATHERING_COPPER_STATE = EntityDataSerializer.forValueType(
      WeatheringCopper.WeatherState.STREAM_CODEC
   );
   public static final EntityDataSerializer<CopperGolemState> COPPER_GOLEM_STATE = EntityDataSerializer.forValueType(CopperGolemState.STREAM_CODEC);
   public static final EntityDataSerializer<Vector3fc> VECTOR3 = EntityDataSerializer.forValueType(ByteBufCodecs.VECTOR3F);
   public static final EntityDataSerializer<Quaternionfc> QUATERNION = EntityDataSerializer.forValueType(ByteBufCodecs.QUATERNIONF);
   public static final EntityDataSerializer<ResolvableProfile> RESOLVABLE_PROFILE = EntityDataSerializer.forValueType(ResolvableProfile.STREAM_CODEC);
   public static final EntityDataSerializer<HumanoidArm> HUMANOID_ARM = EntityDataSerializer.forValueType(HumanoidArm.STREAM_CODEC);

   public static void registerSerializer(EntityDataSerializer<?> p_135051_) {
      SERIALIZERS.add(p_135051_);
   }

   public static @Nullable EntityDataSerializer<?> getSerializer(int p_135049_) {
      return SERIALIZERS.byId(p_135049_);
   }

   public static int getSerializedId(EntityDataSerializer<?> p_135053_) {
      return SERIALIZERS.getId(p_135053_);
   }

   private EntityDataSerializers() {
   }

   static {
      registerSerializer(BYTE);
      registerSerializer(INT);
      registerSerializer(LONG);
      registerSerializer(FLOAT);
      registerSerializer(STRING);
      registerSerializer(COMPONENT);
      registerSerializer(OPTIONAL_COMPONENT);
      registerSerializer(ITEM_STACK);
      registerSerializer(BOOLEAN);
      registerSerializer(ROTATIONS);
      registerSerializer(BLOCK_POS);
      registerSerializer(OPTIONAL_BLOCK_POS);
      registerSerializer(DIRECTION);
      registerSerializer(OPTIONAL_LIVING_ENTITY_REFERENCE);
      registerSerializer(BLOCK_STATE);
      registerSerializer(OPTIONAL_BLOCK_STATE);
      registerSerializer(PARTICLE);
      registerSerializer(PARTICLES);
      registerSerializer(VILLAGER_DATA);
      registerSerializer(OPTIONAL_UNSIGNED_INT);
      registerSerializer(POSE);
      registerSerializer(CAT_VARIANT);
      registerSerializer(COW_VARIANT);
      registerSerializer(WOLF_VARIANT);
      registerSerializer(WOLF_SOUND_VARIANT);
      registerSerializer(FROG_VARIANT);
      registerSerializer(PIG_VARIANT);
      registerSerializer(CHICKEN_VARIANT);
      registerSerializer(ZOMBIE_NAUTILUS_VARIANT);
      registerSerializer(OPTIONAL_GLOBAL_POS);
      registerSerializer(PAINTING_VARIANT);
      registerSerializer(SNIFFER_STATE);
      registerSerializer(ARMADILLO_STATE);
      registerSerializer(COPPER_GOLEM_STATE);
      registerSerializer(WEATHERING_COPPER_STATE);
      registerSerializer(VECTOR3);
      registerSerializer(QUATERNION);
      registerSerializer(RESOLVABLE_PROFILE);
      registerSerializer(HUMANOID_ARM);
   }
}
