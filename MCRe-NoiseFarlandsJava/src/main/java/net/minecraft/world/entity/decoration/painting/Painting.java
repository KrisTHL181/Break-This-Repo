package net.minecraft.world.entity.decoration.painting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Painting extends HangingEntity {
    private static final EntityDataAccessor<Holder<PaintingVariant>> DATA_PAINTING_VARIANT_ID = SynchedEntityData.defineId(
        Painting.class, EntityDataSerializers.PAINTING_VARIANT
    );
    public static final float DEPTH = 0.0625F;

    public Painting(final EntityType<? extends Painting> type, final Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_PAINTING_VARIANT_ID, VariantUtils.getAny(this.registryAccess(), Registries.PAINTING_VARIANT));
    }

    @Override
    public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_PAINTING_VARIANT_ID.equals(accessor)) {
            this.recalculateBoundingBox();
        }
    }

    private void setVariant(final Holder<PaintingVariant> variant) {
        this.entityData.set(DATA_PAINTING_VARIANT_ID, variant);
    }

    public Holder<PaintingVariant> getVariant() {
        return this.entityData.get(DATA_PAINTING_VARIANT_ID);
    }

    @Override
    public <T> @Nullable T get(final DataComponentType<? extends T> type) {
        return type == DataComponents.PAINTING_VARIANT ? castComponentValue((DataComponentType<T>)type, this.getVariant()) : super.get(type);
    }

    @Override
    protected void applyImplicitComponents(final DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.PAINTING_VARIANT);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(final DataComponentType<T> type, final T value) {
        if (type == DataComponents.PAINTING_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.PAINTING_VARIANT, value));
            return true;
        } else {
            return super.applyImplicitComponent(type, value);
        }
    }

    public static Optional<Painting> create(final Level level, final BlockPos pos, final Direction direction) {
        Painting candidate = new Painting(level, pos);
        List<Holder<PaintingVariant>> potentialVariants = new ArrayList<>();
        level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).getTagOrEmpty(PaintingVariantTags.PLACEABLE).forEach(potentialVariants::add);
        if (potentialVariants.isEmpty()) {
            return Optional.empty();
        }

        candidate.setDirection(direction);
        potentialVariants.removeIf(variant -> {
            candidate.setVariant((Holder<PaintingVariant>)variant);
            return !candidate.survives();
        });
        if (potentialVariants.isEmpty()) {
            return Optional.empty();
        }

        int largestPaintingAreaSize = potentialVariants.stream().mapToInt(Painting::variantArea).max().orElse(0);
        potentialVariants.removeIf(variant -> variantArea((Holder<PaintingVariant>)variant) < largestPaintingAreaSize);
        Optional<Holder<PaintingVariant>> selectedVariant = Util.getRandomSafe(potentialVariants, candidate.random);
        if (selectedVariant.isEmpty()) {
            return Optional.empty();
        }

        candidate.setVariant(selectedVariant.get());
        candidate.setDirection(direction);
        return Optional.of(candidate);
    }

    private static int variantArea(final Holder<PaintingVariant> variant) {
        return variant.value().area();
    }

    private Painting(final Level level, final BlockPos blockPos) {
        super(EntityTypes.PAINTING, level, blockPos);
    }

    public Painting(final Level level, final BlockPos blockPos, final Direction direction, final Holder<PaintingVariant> variant) {
        this(level, blockPos);
        this.setVariant(variant);
        this.setDirection(direction);
    }

    @Override
    protected void addAdditionalSaveData(final ValueOutput output) {
        output.store("facing", Direction.LEGACY_ID_CODEC_2D, this.getDirection());
        super.addAdditionalSaveData(output);
        VariantUtils.writeVariant(output, this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(final ValueInput input) {
        Direction direction = input.read("facing", Direction.LEGACY_ID_CODEC_2D).orElse(Direction.SOUTH);
        super.readAdditionalSaveData(input);
        this.setDirection(direction);
        VariantUtils.readVariant(input, Registries.PAINTING_VARIANT).ifPresent(this::setVariant);
    }

    @Override
    protected AABB calculateBoundingBox(final BlockPos pos, final Direction direction) {
        float shiftToBlockWall = 0.46875F;
        Vec3 attachedToWall = Vec3.atCenterOf(pos).relative(direction, -0.46875);
        PaintingVariant variant = this.getVariant().value();
        double horizontalOffset = this.offsetForPaintingSize(variant.width());
        double verticalOffset = this.offsetForPaintingSize(variant.height());
        Direction left = direction.getCounterClockWise();
        Vec3 position = attachedToWall.relative(left, horizontalOffset).relative(Direction.UP, verticalOffset);
        Direction.Axis axis = direction.getAxis();
        double xSize = axis == Direction.Axis.X ? 0.0625 : variant.width();
        double ySize = variant.height();
        double zSize = axis == Direction.Axis.Z ? 0.0625 : variant.width();
        return AABB.ofSize(position, xSize, ySize, zSize);
    }

    private double offsetForPaintingSize(final int size) {
        return size % 2 == 0 ? 0.5 : 0.0;
    }

    @Override
    public void dropItem(final ServerLevel level, final @Nullable Entity causedBy) {
        if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (!(causedBy instanceof Player player && player.hasInfiniteMaterials())) {
                this.spawnAtLocation(level, Items.PAINTING);
            }
        }
    }

    @Override
    public void playPlacementSound() {
        this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public void snapTo(final double x, final double y, final double z, final float yRot, final float xRot) {
        this.setPos(x, y, z);
    }

    @Override
    public Vec3 trackingPosition() {
        return Vec3.atLowerCornerOf(this.pos);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(final ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, this.getDirection().get3DDataValue(), this.getPos());
    }

    @Override
    public void recreateFromPacket(final ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDirection(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.PAINTING);
    }
}