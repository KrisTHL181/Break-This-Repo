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
    private final IdType type;
    private final CompoundTag tag;

    public static <T> Codec<TypedEntityData<T>> codec(final Codec<T> typeCodec) {
        return new Codec<TypedEntityData<T>>() {
            @Override
            public <V> DataResult<Pair<TypedEntityData<T>, V>> decode(final DynamicOps<V> ops, final V input) {
                return CustomData.COMPOUND_TAG_CODEC
                    .decode(ops, input)
                    .flatMap(
                        pair -> {
                            CompoundTag tagWithoutType = pair.getFirst().copy();
                            Tag typeTag = tagWithoutType.remove("id");
                            return typeTag == null
                                ? DataResult.error(() -> "Expected 'id' field in " + input)
                                : typeCodec.parse(asNbtOps((DynamicOps<T>)ops), typeTag)
                                    .map(type -> Pair.of((TypedEntityData<T>)(new TypedEntityData<>(type, tagWithoutType)), (V)pair.getSecond()));
                        }
                    );
            }

            public <V> DataResult<V> encode(final TypedEntityData<T> input, final DynamicOps<V> ops, final V prefix) {
                return typeCodec.encodeStart(asNbtOps((DynamicOps<T>)ops), input.type).flatMap(typeTag -> {
                    CompoundTag tag = input.tag.copy();
                    tag.put("id", typeTag);
                    return CustomData.COMPOUND_TAG_CODEC.encode(tag, ops, prefix);
                });
            }

            private static <T> DynamicOps<Tag> asNbtOps(final DynamicOps<T> ops) {
                return ops instanceof RegistryOps<T> registryOps ? registryOps.withParent(NbtOps.INSTANCE) : NbtOps.INSTANCE;
            }
        };
    }

    public static <B extends ByteBuf, T> StreamCodec<B, TypedEntityData<T>> streamCodec(final StreamCodec<B, T> typeCodec) {
        return StreamCodec.composite(
            typeCodec,
            (Function<TypedEntityData<T>, T>)(TypedEntityData::type),
            ByteBufCodecs.COMPOUND_TAG,
            TypedEntityData::tag,
            (BiFunction<T, CompoundTag, TypedEntityData<T>>)(TypedEntityData::new)
        );
    }

    private TypedEntityData(final IdType type, final CompoundTag data) {
        this.type = type;
        this.tag = stripId(data);
    }

    public static <T> TypedEntityData<T> of(final T type, final CompoundTag data) {
        return new TypedEntityData<>(type, data);
    }

    private static CompoundTag stripId(final CompoundTag tag) {
        if (tag.contains("id")) {
            CompoundTag copy = tag.copy();
            copy.remove("id");
            return copy;
        } else {
            return tag;
        }
    }

    public IdType type() {
        return this.type;
    }

    public boolean contains(final String name) {
        return this.tag.contains(name);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else {
            return !(obj instanceof TypedEntityData<?> customData) ? false : this.type == customData.type && this.tag.equals(customData.tag);
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

    public void loadInto(final Entity entity) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
            entity.saveWithoutId(output);
            CompoundTag entityData = output.buildResult();
            UUID uuid = entity.getUUID();
            entityData.merge(this.getUnsafe());
            entity.load(TagValueInput.create(reporter, entity.registryAccess(), entityData));
            entity.setUUID(uuid);
        }
    }

    public boolean loadInto(final BlockEntity blockEntity, final HolderLookup.Provider registries) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            blockEntity.saveCustomOnly(output);
            CompoundTag entityTag = output.buildResult();
            CompoundTag oldTag = entityTag.copy();
            entityTag.merge(this.getUnsafe());
            if (!entityTag.equals(oldTag)) {
                try {
                    blockEntity.loadCustomOnly(TagValueInput.create(reporter, registries, entityTag));
                    blockEntity.setChanged();
                    return true;
                } catch (Exception e) {
                    LOGGER.warn("Failed to apply custom data to block entity at {}", blockEntity.getBlockPos(), e);

                    try {
                        blockEntity.loadCustomOnly(TagValueInput.create(reporter.forChild(() -> "(rollback)"), registries, oldTag));
                    } catch (Exception e2) {
                        LOGGER.warn("Failed to rollback block entity at {} after failure", blockEntity.getBlockPos(), e2);
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
    public void addToTooltip(
        final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components
    ) {
        if (this.type.getClass() == EntityType.class) {
            EntityType<?> type = (EntityType<?>)this.type;
            if (context.isPeaceful() && !type.isAllowedInPeaceful()) {
                consumer.accept(Component.translatable("item.spawn_egg.peaceful").withStyle(ChatFormatting.RED));
            }
        }
    }
}