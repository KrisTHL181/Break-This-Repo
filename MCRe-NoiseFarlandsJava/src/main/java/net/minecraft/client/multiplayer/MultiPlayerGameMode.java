package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class MultiPlayerGameMode {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final ClientPacketListener connection;
    private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);
    private ItemStack destroyingItem = ItemStack.EMPTY;
    private float destroyProgress;
    private float destroyTicks;
    private int destroyDelay;
    private boolean isDestroying;
    private GameType localPlayerMode = GameType.DEFAULT_MODE;
    private @Nullable GameType previousLocalPlayerMode;
    private int carriedIndex;

    public MultiPlayerGameMode(final Minecraft minecraft, final ClientPacketListener connection) {
        this.minecraft = minecraft;
        this.connection = connection;
    }

    public void adjustPlayer(final Player player) {
        this.localPlayerMode.updatePlayerAbilities(player.getAbilities());
    }

    public void setLocalMode(final GameType mode, final @Nullable GameType previousMode) {
        this.localPlayerMode = mode;
        this.previousLocalPlayerMode = previousMode;
        this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
    }

    public void setLocalMode(final GameType mode) {
        if (mode != this.localPlayerMode) {
            this.previousLocalPlayerMode = this.localPlayerMode;
        }

        this.localPlayerMode = mode;
        this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
    }

    public boolean canHurtPlayer() {
        return this.localPlayerMode.isSurvival();
    }

    public boolean destroyBlock(final BlockPos pos) {
        if (this.minecraft.player.blockActionRestricted(this.minecraft.level, pos, this.localPlayerMode)) {
            return false;
        }

        Level level = this.minecraft.level;
        BlockState oldState = level.getBlockState(pos);
        if (!this.minecraft.player.getMainHandItem().canDestroyBlock(oldState, level, pos, this.minecraft.player)) {
            return false;
        }

        Block oldBlock = oldState.getBlock();
        if (oldBlock instanceof GameMasterBlock && !this.minecraft.player.canUseGameMasterBlocks()) {
            return false;
        }

        if (oldState.isAir()) {
            return false;
        }

        oldBlock.playerWillDestroy(level, pos, oldState, this.minecraft.player);
        FluidState fluidState = level.getFluidState(pos);
        boolean changed = level.setBlock(pos, fluidState.createLegacyBlock(), 11);
        if (changed) {
            oldBlock.destroy(level, pos, oldState);
        }

        if (SharedConstants.DEBUG_BLOCK_BREAK) {
            LOGGER.error("client broke {} {} -> {}", pos, oldState, level.getBlockState(pos));
        }

        return changed;
    }

    public boolean startDestroyBlock(final BlockPos pos, final Direction direction) {
        if (this.minecraft.player.blockActionRestricted(this.minecraft.level, pos, this.localPlayerMode)) {
            return false;
        }

        if (!this.minecraft.level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        if (this.minecraft.player.getAbilities().instabuild) {
            BlockState state = this.minecraft.level.getBlockState(pos);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pos, state, 1.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Creative start {} {}", pos, state);
            }

            this.startPrediction(this.minecraft.level, sequence -> {
                this.destroyBlock(pos);
                return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
            });
            this.destroyDelay = 5;
        } else if (!this.isDestroying || !this.sameDestroyTarget(pos)) {
            if (this.isDestroying) {
                if (SharedConstants.DEBUG_BLOCK_BREAK) {
                    LOGGER.info("Abort old break {} {}", pos, this.minecraft.level.getBlockState(pos));
                }

                this.connection
                    .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, direction));
            }

            BlockState state = this.minecraft.level.getBlockState(pos);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pos, state, 0.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Start break {} {}", pos, state);
            }

            this.startPrediction(this.minecraft.level, sequence -> {
                boolean notAir = !state.isAir();
                if (notAir && this.destroyProgress == 0.0F) {
                    state.attack(this.minecraft.level, pos, this.minecraft.player);
                }

                if (notAir && state.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), pos) >= 1.0F) {
                    this.destroyBlock(pos);
                } else {
                    this.isDestroying = true;
                    this.destroyBlockPos = pos;
                    this.destroyingItem = this.minecraft.player.getMainHandItem();
                    this.destroyProgress = 0.0F;
                    this.destroyTicks = 0.0F;
                    this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
                }

                return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
            });
        }

        return true;
    }

    public void stopDestroyBlock() {
        if (this.isDestroying) {
            BlockState state = this.minecraft.level.getBlockState(this.destroyBlockPos);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, this.destroyBlockPos, state, -1.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Stop dest {} {}", this.destroyBlockPos, state);
            }

            this.connection
                .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, Direction.DOWN));
            this.isDestroying = false;
            this.destroyProgress = 0.0F;
            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, -1);
            this.minecraft.player.resetAttackStrengthTicker();
        }
    }

    public boolean continueDestroyBlock(final BlockPos pos, final Direction direction) {
        this.ensureHasSentCarriedItem();
        if (this.destroyDelay > 0) {
            this.destroyDelay--;
            return true;
        }

        if (this.minecraft.player.getAbilities().instabuild && this.minecraft.level.getWorldBorder().isWithinBounds(pos)) {
            this.destroyDelay = 5;
            BlockState state = this.minecraft.level.getBlockState(pos);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pos, state, 1.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
                LOGGER.info("Creative cont {} {}", pos, state);
            }

            this.startPrediction(this.minecraft.level, sequence -> {
                this.destroyBlock(pos);
                return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, direction, sequence);
            });
            return true;
        } else if (this.sameDestroyTarget(pos)) {
            BlockState state = this.minecraft.level.getBlockState(pos);
            if (state.isAir()) {
                this.isDestroying = false;
                return false;
            }

            this.destroyProgress = this.destroyProgress + state.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), pos);
            if (this.destroyTicks % 4.0F == 0.0F) {
                SoundType soundType = state.getSoundType();
                this.minecraft
                    .getSoundManager()
                    .play(
                        new SimpleSoundInstance(
                            soundType.getHitSound(),
                            SoundSource.BLOCKS,
                            (soundType.getVolume() + 1.0F) / 8.0F,
                            soundType.getPitch() * 0.5F,
                            SoundInstance.createUnseededRandom(),
                            pos
                        )
                    );
            }

            this.destroyTicks++;
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pos, state, Mth.clamp(this.destroyProgress, 0.0F, 1.0F));
            if (this.destroyProgress >= 1.0F) {
                this.isDestroying = false;
                if (SharedConstants.DEBUG_BLOCK_BREAK) {
                    LOGGER.info("Finished breaking {} {}", pos, state);
                }

                this.startPrediction(this.minecraft.level, sequence -> {
                    this.destroyBlock(pos);
                    return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, direction, sequence);
                });
                this.destroyProgress = 0.0F;
                this.destroyTicks = 0.0F;
                this.destroyDelay = 5;
            }

            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
            return true;
        } else {
            return this.startDestroyBlock(pos, direction);
        }
    }

    private void startPrediction(final ClientLevel level, final PredictiveAction predictiveAction) {
        try (BlockStatePredictionHandler prediction = level.getBlockStatePredictionHandler().startPredicting()) {
            int sequence = prediction.currentSequence();
            Packet<ServerGamePacketListener> packetConcludingPrediction = predictiveAction.predict(sequence);
            this.connection.send(packetConcludingPrediction);
        }
    }

    public void tick() {
        this.ensureHasSentCarriedItem();
        if (this.connection.getConnection().isConnected()) {
            this.connection.getConnection().tick();
        } else {
            this.connection.getConnection().handleDisconnection();
        }
    }

    private boolean sameDestroyTarget(final BlockPos pos) {
        ItemStack selected = this.minecraft.player.getMainHandItem();
        return pos.equals(this.destroyBlockPos) && ItemStack.isSameItemSameComponents(selected, this.destroyingItem);
    }

    private void ensureHasSentCarriedItem() {
        int index = this.minecraft.player.getInventory().getSelectedSlot();
        if (index != this.carriedIndex) {
            this.carriedIndex = index;
            this.connection.send(new ServerboundSetCarriedItemPacket(this.carriedIndex));
        }
    }

    public InteractionResult useItemOn(final LocalPlayer player, final InteractionHand hand, final BlockHitResult blockHit) {
        this.ensureHasSentCarriedItem();
        if (!this.minecraft.level.getWorldBorder().isWithinBounds(blockHit.getBlockPos())) {
            return InteractionResult.FAIL;
        }

        MutableObject<InteractionResult> result = new MutableObject<>();
        this.startPrediction(this.minecraft.level, sequence -> {
            result.setValue(this.performUseItemOn(player, hand, blockHit));
            return new ServerboundUseItemOnPacket(hand, blockHit, sequence);
        });
        return result.get();
    }

    private InteractionResult performUseItemOn(final LocalPlayer player, final InteractionHand hand, final BlockHitResult blockHit) {
        BlockPos pos = blockHit.getBlockPos();
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.localPlayerMode == GameType.SPECTATOR) {
            return InteractionResult.CONSUME;
        }

        boolean haveSomethingInOurHands = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
        boolean suppressUsingBlock = player.isSecondaryUseActive() && haveSomethingInOurHands;
        if (!suppressUsingBlock) {
            BlockState blockState = this.minecraft.level.getBlockState(pos);
            if (!this.connection.isFeatureEnabled(blockState.getBlock().requiredFeatures())) {
                return InteractionResult.FAIL;
            }

            InteractionResult itemUse = blockState.useItemOn(player.getItemInHand(hand), this.minecraft.level, player, hand, blockHit);
            if (itemUse.consumesAction()) {
                return itemUse;
            }

            if (itemUse instanceof InteractionResult.TryEmptyHandInteraction && hand == InteractionHand.MAIN_HAND) {
                InteractionResult use = blockState.useWithoutItem(this.minecraft.level, player, blockHit);
                if (use.consumesAction()) {
                    return use;
                }
            }
        }

        if (!itemStack.isEmpty() && !player.getCooldowns().isOnCooldown(itemStack)) {
            UseOnContext context = new UseOnContext(player, hand, blockHit);
            InteractionResult success;
            if (player.hasInfiniteMaterials()) {
                int count = itemStack.getCount();
                success = itemStack.useOn(context);
                itemStack.setCount(count);
            } else {
                success = itemStack.useOn(context);
            }

            return success;
        } else {
            return InteractionResult.PASS;
        }
    }

    public InteractionResult useItem(final Player player, final InteractionHand hand) {
        if (this.localPlayerMode == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        }

        this.ensureHasSentCarriedItem();
        MutableObject<InteractionResult> interactionResult = new MutableObject<>();
        this.startPrediction(this.minecraft.level, sequence -> {
            ServerboundUseItemPacket packet = new ServerboundUseItemPacket(hand, sequence, player.getYRot(), player.getXRot());
            ItemStack itemStack = player.getItemInHand(hand);
            if (player.getCooldowns().isOnCooldown(itemStack)) {
                interactionResult.setValue(InteractionResult.PASS);
                return packet;
            }

            InteractionResult resultHolder = itemStack.use(this.minecraft.level, player, hand);
            ItemStack result;
            if (resultHolder instanceof InteractionResult.Success success) {
                result = Objects.requireNonNullElseGet(success.heldItemTransformedTo(), () -> player.getItemInHand(hand));
            } else {
                result = player.getItemInHand(hand);
            }

            if (result != itemStack) {
                player.setItemInHand(hand, result);
            }

            interactionResult.setValue(resultHolder);
            return packet;
        });
        return interactionResult.get();
    }

    public LocalPlayer createPlayer(final ClientLevel level, final StatsCounter stats, final ClientRecipeBook recipeBook) {
        return this.createPlayer(level, stats, recipeBook, Input.EMPTY, false);
    }

    public LocalPlayer createPlayer(
        final ClientLevel level, final StatsCounter stats, final ClientRecipeBook recipeBook, final Input lastSentInput, final boolean wasSprinting
    ) {
        return new LocalPlayer(this.minecraft, level, this.connection, stats, recipeBook, lastSentInput, wasSprinting, this.minecraft.computeChatAbilities());
    }

    public void attack(final Player player, final Entity entity) {
        this.ensureHasSentCarriedItem();
        this.connection.send(new ServerboundAttackPacket(entity.getId()));
        player.attack(entity);
        player.resetAttackStrengthTicker();
    }

    public void spectate(final Entity entity) {
        this.connection.send(new ServerboundSpectatorActionPacket(OptionalInt.of(entity.getId())));
    }

    public void spectatorNoAction() {
        this.connection.send(new ServerboundSpectatorActionPacket(OptionalInt.empty()));
    }

    public InteractionResult interact(final Player player, final Entity entity, final EntityHitResult hitResult, final InteractionHand hand) {
        this.ensureHasSentCarriedItem();
        Vec3 location = hitResult.getLocation().subtract(entity.getX(), entity.getY(), entity.getZ());
        this.connection.send(new ServerboundInteractPacket(entity.getId(), hand, location, player.isShiftKeyDown()));
        return this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : player.interactOn(entity, hand, location);
    }

    public void handleContainerInput(final int containerId, final int slotNum, final int buttonNum, final ContainerInput containerInput, final Player player) {
        AbstractContainerMenu containerMenu = player.containerMenu;
        if (containerId != containerMenu.containerId) {
            LOGGER.warn("Ignoring click in mismatching container. Click in {}, player has {}.", containerId, containerMenu.containerId);
        } else {
            NonNullList<Slot> slots = containerMenu.slots;
            int slotCount = slots.size();
            List<ItemStack> itemsBeforeClick = Lists.newArrayListWithCapacity(slotCount);

            for (Slot slot : slots) {
                itemsBeforeClick.add(slot.getItem().copy());
            }

            containerMenu.clicked(slotNum, buttonNum, containerInput, player);
            Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectOpenHashMap<>();

            for (int i = 0; i < slotCount; i++) {
                ItemStack before = itemsBeforeClick.get(i);
                ItemStack after = slots.get(i).getItem();
                if (!ItemStack.matches(before, after)) {
                    changedSlots.put(i, HashedStack.create(after, this.connection.decoratedHashOpsGenenerator()));
                }
            }

            HashedStack carriedItem = HashedStack.create(containerMenu.getCarried(), this.connection.decoratedHashOpsGenenerator());
            this.connection
                .send(
                    new ServerboundContainerClickPacket(
                        containerId,
                        containerMenu.getStateId(),
                        Shorts.checkedCast(slotNum),
                        SignedBytes.checkedCast(buttonNum),
                        containerInput,
                        changedSlots,
                        carriedItem
                    )
                );
        }
    }

    public void handlePlaceRecipe(final int containerId, final RecipeDisplayId recipe, final boolean useMaxItems) {
        this.connection.send(new ServerboundPlaceRecipePacket(containerId, recipe, useMaxItems));
    }

    public void handleInventoryButtonClick(final int containerId, final int buttonId) {
        this.connection.send(new ServerboundContainerButtonClickPacket(containerId, buttonId));
    }

    public void handleCreativeModeItemAdd(final ItemStack clicked, final int slot) {
        if (this.minecraft.player.hasInfiniteMaterials() && this.connection.isFeatureEnabled(clicked.getItem().requiredFeatures())) {
            this.connection.send(new ServerboundSetCreativeModeSlotPacket(slot, clicked));
        }
    }

    public void handleCreativeModeItemDrop(final ItemStack clicked) {
        boolean hasOtherInventoryOpen = this.minecraft.gui.screen() instanceof AbstractContainerScreen
            && !(this.minecraft.gui.screen() instanceof CreativeModeInventoryScreen);
        if (this.minecraft.player.hasInfiniteMaterials()
            && !hasOtherInventoryOpen
            && !clicked.isEmpty()
            && this.connection.isFeatureEnabled(clicked.getItem().requiredFeatures())) {
            this.connection.send(new ServerboundSetCreativeModeSlotPacket(-1, clicked));
            this.minecraft.player.getDropSpamThrottler().increment();
        }
    }

    public void releaseUsingItem(final Player player) {
        this.ensureHasSentCarriedItem();
        this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
        player.releaseUsingItem();
    }

    public void piercingAttack(final PiercingWeapon weapon) {
        this.ensureHasSentCarriedItem();
        this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STAB, BlockPos.ZERO, Direction.DOWN));
        this.minecraft.player.onAttack();
        this.minecraft.player.postPiercingAttack();
        weapon.makeSound(this.minecraft.player);
    }

    public boolean hasExperience() {
        return this.localPlayerMode.isSurvival();
    }

    public boolean hasMissTime() {
        return !this.localPlayerMode.isCreative();
    }

    public boolean isServerControlledInventory() {
        return this.minecraft.player.isPassenger() && this.minecraft.player.getVehicle() instanceof HasCustomInventoryScreen;
    }

    public boolean isSpectator() {
        return this.localPlayerMode == GameType.SPECTATOR;
    }

    public @Nullable GameType getPreviousPlayerMode() {
        return this.previousLocalPlayerMode;
    }

    public GameType getPlayerMode() {
        return this.localPlayerMode;
    }

    public boolean isDestroying() {
        return this.isDestroying;
    }

    public int getDestroyStage() {
        return this.destroyProgress > 0.0F ? (int)(this.destroyProgress * 10.0F) : -1;
    }

    public void handlePickItemFromBlock(final BlockPos pos, final boolean includeData) {
        this.connection.send(new ServerboundPickItemFromBlockPacket(pos, includeData));
    }

    public void handlePickItemFromEntity(final Entity entity, final boolean includeData) {
        this.connection.send(new ServerboundPickItemFromEntityPacket(entity.getId(), includeData));
    }

    public void handleSlotStateChanged(final int slotId, final int containerId, final boolean newState) {
        this.connection.send(new ServerboundContainerSlotStateChangedPacket(slotId, containerId, newState));
    }
}