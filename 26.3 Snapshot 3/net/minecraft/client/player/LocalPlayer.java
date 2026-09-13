package net.minecraft.client.player;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.TestBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.TestInstanceBlockEditScreen;
import net.minecraft.client.gui.screens.options.HasGamemasterPermissionReaction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.chat.ChatAbilities;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.client.resources.sounds.BubbleColumnAmbientSoundHandler;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.RidingEntitySoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.UnderLiquidAmbientSoundInstance;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.TickThrottler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LocalPlayer extends AbstractClientPlayer {
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final int POSITION_REMINDER_INTERVAL = 20;
   private static final int WATER_VISION_MAX_TIME = 600;
   private static final int WATER_VISION_QUICK_TIME = 100;
   private static final float WATER_VISION_QUICK_PERCENT = 0.6F;
   private static final double SUFFOCATING_COLLISION_CHECK_SCALE = 0.35;
   private static final double MINOR_COLLISION_ANGLE_THRESHOLD_RADIAN = 0.13962634F;
   public final ClientPacketListener connection;
   private final StatsCounter stats;
   private final ClientRecipeBook recipeBook;
   private final TickThrottler dropSpamThrottler = new TickThrottler(20, 1280);
   private final List<AmbientSoundHandler> ambientSoundHandlers = Lists.newArrayList();
   private PermissionSet permissions = PermissionSet.NO_PERMISSIONS;
   private ChatAbilities chatAbilities;
   private double xLast;
   private double yLast;
   private double zLast;
   private float yRotLast;
   private float xRotLast;
   private boolean lastOnGround;
   private boolean lastHorizontalCollision;
   private boolean crouching;
   private boolean wasSprinting;
   private int positionReminder;
   private boolean flashOnSetHealth;
   public ClientInput input = new ClientInput();
   private Input lastSentInput;
   protected final Minecraft minecraft;
   protected int sprintTriggerTime;
   private static final int EXPERIENCE_DISPLAY_UNREADY_TO_SET = Integer.MIN_VALUE;
   private static final int EXPERIENCE_DISPLAY_READY_TO_SET = -2147483647;
   public int experienceDisplayStartTick = Integer.MIN_VALUE;
   public float yBob;
   public float xBob;
   public float yBobO;
   public float xBobO;
   private int jumpRidingTicks;
   private float jumpRidingScale;
   public float portalEffectIntensity;
   public float oPortalEffectIntensity;
   private boolean startedUsingItem;
   private @Nullable InteractionHand usingItemHand;
   private boolean handsBusy;
   private boolean autoJumpEnabled = true;
   private int autoJumpTime;
   private boolean wasFallFlying;
   private int waterVisionTime;
   private boolean showDeathScreen = true;
   private boolean doLimitedCrafting = false;

   public LocalPlayer(
      final Minecraft minecraft,
      final ClientLevel level,
      final ClientPacketListener connection,
      final StatsCounter stats,
      final ClientRecipeBook recipeBook,
      final Input lastSentInput,
      final boolean wasSprinting,
      final ChatAbilities chatAbilities
   ) {
      super(level, connection.getLocalGameProfile());
      this.minecraft = minecraft;
      this.connection = connection;
      this.stats = stats;
      this.recipeBook = recipeBook;
      this.lastSentInput = lastSentInput;
      this.wasSprinting = wasSprinting;
      this.ambientSoundHandlers.add(new UnderwaterAmbientSoundHandler(this, minecraft.getSoundManager()));
      this.ambientSoundHandlers.add(new BubbleColumnAmbientSoundHandler(this));
      this.ambientSoundHandlers.add(new BiomeAmbientSoundsHandler(this, minecraft.getSoundManager()));
      this.chatAbilities = chatAbilities;
   }

   @Override
   public void heal(final float heal) {
   }

   @Override
   public boolean startRiding(final Entity entity, final boolean force, final boolean sendEventAndTriggers) {
      if (!super.startRiding(entity, force, sendEventAndTriggers)) {
         return false;
      }

      if (entity instanceof AbstractMinecart minecart) {
         this.minecraft
            .getSoundManager()
            .play(new RidingMinecartSoundInstance(this, minecart, true, SoundEvents.MINECART_INSIDE_UNDERWATER, 0.0F, 0.75F, 1.0F));
         this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, minecart, false, SoundEvents.MINECART_INSIDE, 0.0F, 0.75F, 1.0F));
      } else if (entity instanceof HappyGhast happyGhast) {
         this.minecraft
            .getSoundManager()
            .play(new RidingEntitySoundInstance(this, happyGhast, false, SoundEvents.HAPPY_GHAST_RIDING, happyGhast.getSoundSource(), 0.0F, 1.0F, 5.0F));
      } else if (entity instanceof AbstractNautilus nautilus) {
         this.minecraft
            .getSoundManager()
            .play(new RidingEntitySoundInstance(this, nautilus, true, SoundEvents.NAUTILUS_RIDING, nautilus.getSoundSource(), 0.0F, 1.0F, 5.0F));
      }

      return true;
   }

   @Override
   public void removeVehicle() {
      super.removeVehicle();
      this.handsBusy = false;
   }

   @Override
   public float getViewXRot(final float a) {
      return this.getXRot();
   }

   @Override
   public float getViewYRot(final float a) {
      return this.isPassenger() ? super.getViewYRot(a) : this.getYRot();
   }

   @Override
   public void tick() {
      if (this.connection.hasClientLoaded()) {
         this.dropSpamThrottler.tick();
         super.tick();

         for (AmbientSoundHandler soundHandler : this.ambientSoundHandlers) {
            soundHandler.tick();
         }
      }
   }

   public void sendChanges() {
      if (this.connection.hasClientLoaded()) {
         if (!this.lastSentInput.equals(this.input.keyPresses)) {
            this.connection.send(new ServerboundPlayerInputPacket(this.input.keyPresses));
            this.lastSentInput = this.input.keyPresses;
         }

         if (this.isPassenger()) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision));
            Entity vehicle = this.getRootVehicle();
            if (vehicle != this && vehicle.isLocalInstanceAuthoritative()) {
               this.connection.send(ServerboundMoveVehiclePacket.fromEntity(vehicle));
               this.sendIsSprintingIfNeeded();
            }
         } else {
            this.sendPosition();
         }
      }
   }

   public float getCurrentMood() {
      for (AmbientSoundHandler ambientSoundHandler : this.ambientSoundHandlers) {
         if (ambientSoundHandler instanceof BiomeAmbientSoundsHandler biomeAmbientSoundsHandler) {
            return biomeAmbientSoundsHandler.getMoodiness();
         }
      }

      return 0.0F;
   }

   private void sendPosition() {
      this.sendIsSprintingIfNeeded();
      if (this.isControlledCamera()) {
         double deltaX = this.getX() - this.xLast;
         double deltaY = this.getY() - this.yLast;
         double deltaZ = this.getZ() - this.zLast;
         double deltaYRot = this.getYRot() - this.yRotLast;
         double deltaXRot = this.getXRot() - this.xRotLast;
         this.positionReminder++;
         boolean move = Mth.lengthSquared(deltaX, deltaY, deltaZ) > Mth.square(2.0E-4) || this.positionReminder >= 20;
         boolean rot = deltaYRot != 0.0 || deltaXRot != 0.0;
         if (move && rot) {
            this.connection
               .send(new ServerboundMovePlayerPacket.PosRot(this.position(), this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision));
         } else if (move) {
            this.connection.send(new ServerboundMovePlayerPacket.Pos(this.position(), this.onGround(), this.horizontalCollision));
         } else if (rot) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision));
         } else if (this.lastOnGround != this.onGround() || this.lastHorizontalCollision != this.horizontalCollision) {
            this.connection.send(new ServerboundMovePlayerPacket.StatusOnly(this.onGround(), this.horizontalCollision));
         }

         if (move) {
            this.xLast = this.getX();
            this.yLast = this.getY();
            this.zLast = this.getZ();
            this.positionReminder = 0;
         }

         if (rot) {
            this.yRotLast = this.getYRot();
            this.xRotLast = this.getXRot();
         }

         this.lastOnGround = this.onGround();
         this.lastHorizontalCollision = this.horizontalCollision;
         this.autoJumpEnabled = this.minecraft.options.autoJump().get();
      }
   }

   private void sendIsSprintingIfNeeded() {
      boolean isSprinting = this.isSprinting();
      if (isSprinting != this.wasSprinting) {
         ServerboundPlayerCommandPacket.Action action = isSprinting
            ? ServerboundPlayerCommandPacket.Action.START_SPRINTING
            : ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
         this.connection.send(new ServerboundPlayerCommandPacket(this, action));
         this.wasSprinting = isSprinting;
      }
   }

   public boolean drop(final boolean all) {
      ServerboundPlayerActionPacket.Action action = all ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS : ServerboundPlayerActionPacket.Action.DROP_ITEM;
      ItemStack prediction = this.getInventory().removeFromSelected(all);
      this.connection.send(new ServerboundPlayerActionPacket(action, BlockPos.ZERO, Direction.DOWN));
      return !prediction.isEmpty();
   }

   @Override
   public void swing(final InteractionHand hand) {
      super.swing(hand);
      this.connection.send(new ServerboundSwingPacket(hand));
   }

   public void respawn() {
      this.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
      KeyMapping.resetToggleKeys();
   }

   @Override
   public void closeContainer() {
      this.connection.send(new ServerboundContainerClosePacket(this.containerMenu.containerId));
      this.clientSideCloseContainer();
   }

   public void clientSideCloseContainer() {
      super.closeContainer();
      this.minecraft.gui.setScreen(null);
   }

   public void hurtTo(final float newHealth) {
      if (this.flashOnSetHealth) {
         float dmg = this.getHealth() - newHealth;
         if (dmg <= 0.0F) {
            this.setHealth(newHealth);
            if (dmg < 0.0F) {
               this.invulnerableTime = 10;
            }
         } else {
            this.lastHurt = dmg;
            this.invulnerableTime = 20;
            this.setHealth(newHealth);
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
         }
      } else {
         this.setHealth(newHealth);
         this.flashOnSetHealth = true;
      }
   }

   @Override
   public void onUpdateAbilities() {
      this.connection.send(new ServerboundPlayerAbilitiesPacket(this.getAbilities()));
   }

   @Override
   public void setReducedDebugInfo(final boolean reducedDebugInfo) {
      super.setReducedDebugInfo(reducedDebugInfo);
      this.minecraft.debugEntries.rebuildCurrentList();
   }

   @Override
   public boolean isLocalPlayer() {
      return true;
   }

   @Override
   public boolean isSuppressingSlidingDownLadder() {
      return !this.getAbilities().flying && super.isSuppressingSlidingDownLadder();
   }

   @Override
   public boolean canSpawnSprintParticle() {
      return !this.getAbilities().flying && super.canSpawnSprintParticle();
   }

   protected void sendRidingJump() {
      this.connection
         .send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP, Mth.floor(this.getJumpRidingScale() * 100.0F)));
   }

   public void sendOpenInventory() {
      this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
   }

   public StatsCounter getStats() {
      return this.stats;
   }

   public ClientRecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   public void removeRecipeHighlight(final RecipeDisplayId recipe) {
      if (this.recipeBook.willHighlight(recipe)) {
         this.recipeBook.removeHighlight(recipe);
         this.connection.send(new ServerboundRecipeBookSeenRecipePacket(recipe));
      }
   }

   @Override
   public PermissionSet permissions() {
      return this.permissions;
   }

   public void setPermissions(final PermissionSet newPermissions) {
      boolean previousGamemasterPermission = this.permissions.hasPermission(Permissions.COMMANDS_GAMEMASTER);
      boolean newGamemasterPermission = newPermissions.hasPermission(Permissions.COMMANDS_GAMEMASTER);
      this.permissions = newPermissions;
      if (previousGamemasterPermission != newGamemasterPermission && this.minecraft.gui.screen() instanceof HasGamemasterPermissionReaction screen) {
         screen.onGamemasterPermissionChanged(newGamemasterPermission);
      }
   }

   public ChatAbilities chatAbilities() {
      return this.chatAbilities;
   }

   public void refreshChatAbilities() {
      this.chatAbilities = this.minecraft.computeChatAbilities();
      this.minecraft.gui.hud.getChat().setVisibleMessageFilter(this.chatAbilities.visibleMessagesFilter());
   }

   @Override
   public void sendSystemMessage(final Component message) {
      this.minecraft.gui.chatListener().handleSystemMessage(message, true);
   }

   @Override
   public void sendOverlayMessage(final Component message) {
      this.minecraft.gui.chatListener().handleOverlay(message);
   }

   private void moveTowardsClosestSpace(final double x, final double z) {
      BlockPos pos = BlockPos.containing(x, this.getY(), z);
      if (this.suffocatesAt(pos)) {
         double xd = x - pos.getX();
         double zd = z - pos.getZ();
         Direction dir = null;
         double closest = Double.MAX_VALUE;
         Direction[] directions = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

         for (Direction direction : directions) {
            double axisDistance = direction.getAxis().choose(xd, 0.0, zd);
            double distanceToEdge = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - axisDistance : axisDistance;
            if (distanceToEdge < closest && !this.suffocatesAt(pos.relative(direction))) {
               closest = distanceToEdge;
               dir = direction;
            }
         }

         if (dir != null) {
            Vec3 oldMovement = this.getDeltaMovement();
            if (dir.getAxis() == Direction.Axis.X) {
               this.setDeltaMovement(0.1 * dir.getStepX(), oldMovement.y, oldMovement.z);
            } else {
               this.setDeltaMovement(oldMovement.x, oldMovement.y, 0.1 * dir.getStepZ());
            }
         }
      }
   }

   private boolean suffocatesAt(final BlockPos pos) {
      AABB boundingBox = this.getBoundingBox();
      AABB testArea = new AABB(pos.getX(), boundingBox.minY, pos.getZ(), pos.getX() + 1.0, boundingBox.maxY, pos.getZ() + 1.0).deflate(1.0E-7);
      return this.level().collidesWithSuffocatingBlock(this, testArea);
   }

   public void setExperienceValues(final float experienceProgress, final int totalExp, final int experienceLevel) {
      if (experienceProgress != this.experienceProgress) {
         this.setExperienceDisplayStartTickToTickCount();
      }

      this.experienceProgress = experienceProgress;
      this.totalExperience = totalExp;
      this.experienceLevel = experienceLevel;
   }

   private void setExperienceDisplayStartTickToTickCount() {
      if (this.experienceDisplayStartTick == Integer.MIN_VALUE) {
         this.experienceDisplayStartTick = -2147483647;
      } else {
         this.experienceDisplayStartTick = this.tickCount;
      }
   }

   @Override
   public void handleEntityEvent(final byte id) {
      switch (id) {
         case 24:
            this.setPermissions(PermissionSet.NO_PERMISSIONS);
            break;
         case 25:
            this.setPermissions(LevelBasedPermissionSet.MODERATOR);
            break;
         case 26:
            this.setPermissions(LevelBasedPermissionSet.GAMEMASTER);
            break;
         case 27:
            this.setPermissions(LevelBasedPermissionSet.ADMIN);
            break;
         case 28:
            this.setPermissions(LevelBasedPermissionSet.OWNER);
            break;
         default:
            super.handleEntityEvent(id);
      }
   }

   public void setShowDeathScreen(final boolean show) {
      this.showDeathScreen = show;
   }

   public boolean shouldShowDeathScreen() {
      return this.showDeathScreen;
   }

   public void setDoLimitedCrafting(final boolean value) {
      this.doLimitedCrafting = value;
   }

   public boolean getDoLimitedCrafting() {
      return this.doLimitedCrafting;
   }

   @Override
   public void playSound(final SoundEvent sound, final float volume, final float pitch) {
      this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch, false);
   }

   @Override
   public void startUsingItem(final InteractionHand hand) {
      ItemStack itemStack = this.getItemInHand(hand);
      if (!itemStack.isEmpty() && !this.isUsingItem()) {
         super.startUsingItem(hand);
         this.startedUsingItem = true;
         this.usingItemHand = hand;
      }
   }

   @Override
   public boolean isUsingItem() {
      return this.startedUsingItem;
   }

   private boolean isSlowDueToUsingItem() {
      return this.isUsingItem() && !this.useItem.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).canSprint();
   }

   private float itemUseSpeedMultiplier() {
      return this.useItem.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).speedMultiplier();
   }

   @Override
   public void stopUsingItem() {
      super.stopUsingItem();
      this.startedUsingItem = false;
   }

   @Override
   public InteractionHand getUsedItemHand() {
      return Objects.requireNonNullElse(this.usingItemHand, InteractionHand.MAIN_HAND);
   }

   @Override
   public void onSyncedDataUpdated(final EntityDataAccessor<?> accessor) {
      super.onSyncedDataUpdated(accessor);
      if (DATA_LIVING_ENTITY_FLAGS.equals(accessor)) {
         boolean serverUsingItem = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
         InteractionHand serverUsingHand = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         if (serverUsingItem && !this.startedUsingItem) {
            this.startUsingItem(serverUsingHand);
         } else if (!serverUsingItem && this.startedUsingItem) {
            this.stopUsingItem();
         }
      }

      if (DATA_SHARED_FLAGS_ID.equals(accessor) && this.isFallFlying() && !this.wasFallFlying) {
         this.minecraft.getSoundManager().play(new ElytraOnPlayerSoundInstance(this));
      }
   }

   public @Nullable PlayerRideableJumping jumpableVehicle() {
      return this.getControlledVehicle() instanceof PlayerRideableJumping playerRideableJumping && playerRideableJumping.canJump()
         ? playerRideableJumping
         : null;
   }

   public float getJumpRidingScale() {
      return this.jumpRidingScale;
   }

   @Override
   public boolean isTextFilteringEnabled() {
      return this.minecraft.isTextFilteringEnabled();
   }

   @Override
   public void openTextEdit(final SignBlockEntity sign, final boolean isFrontText) {
      if (sign instanceof HangingSignBlockEntity hangingSign) {
         this.minecraft.gui.setScreen(new HangingSignEditScreen(hangingSign, isFrontText, this.minecraft.isTextFilteringEnabled()));
      } else {
         this.minecraft.gui.setScreen(new SignEditScreen(sign, isFrontText, this.minecraft.isTextFilteringEnabled()));
      }
   }

   @Override
   public void openMinecartCommandBlock(final MinecartCommandBlock commandBlock) {
      this.minecraft.gui.setScreen(new MinecartCommandBlockEditScreen(commandBlock));
   }

   @Override
   public void openCommandBlock(final CommandBlockEntity commandBlock) {
      this.minecraft.gui.setScreen(new CommandBlockEditScreen(commandBlock));
   }

   @Override
   public void openStructureBlock(final StructureBlockEntity structureBlock) {
      this.minecraft.gui.setScreen(new StructureBlockEditScreen(structureBlock));
   }

   @Override
   public void openTestBlock(final TestBlockEntity testBlock) {
      this.minecraft.gui.setScreen(new TestBlockEditScreen(testBlock));
   }

   @Override
   public void openTestInstanceBlock(final TestInstanceBlockEntity testInstanceBlock) {
      this.minecraft.gui.setScreen(new TestInstanceBlockEditScreen(testInstanceBlock));
   }

   @Override
   public void openJigsawBlock(final JigsawBlockEntity jigsawBlock) {
      this.minecraft.gui.setScreen(new JigsawBlockEditScreen(jigsawBlock));
   }

   @Override
   public void openDialog(final Holder<Dialog> dialog) {
      this.connection.showDialog(dialog, this.minecraft.gui.screen());
   }

   @Override
   public void openItemGui(final ItemStack itemStack, final InteractionHand hand) {
      WritableBookContent content = itemStack.get(DataComponents.WRITABLE_BOOK_CONTENT);
      if (content != null) {
         this.minecraft.gui.setScreen(new BookEditScreen(this, itemStack, hand, content));
      }
   }

   @Override
   public void crit(final Entity entity) {
      this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.CRIT);
   }

   @Override
   public void magicCrit(final Entity entity) {
      this.minecraft.particleEngine.createTrackingEmitter(entity, ParticleTypes.ENCHANTED_HIT);
   }

   @Override
   public boolean isShiftKeyDown() {
      return this.input.keyPresses.shift();
   }

   @Override
   public boolean isCrouching() {
      return this.crouching;
   }

   public boolean isMovingSlowly() {
      return this.isCrouching() || this.isVisuallyCrawling();
   }

   @Override
   public void applyInput() {
      if (this.isControlledCamera()) {
         Vec2 modifiedInput = this.modifyInput(this.input.getMoveVector());
         this.xxa = modifiedInput.x;
         this.zza = modifiedInput.y;
         this.jumping = this.input.keyPresses.jump();
         this.yBobO = this.yBob;
         this.xBobO = this.xBob;
         this.xBob = this.xBob + (this.getXRot() - this.xBob) * 0.5F;
         this.yBob = this.yBob + (this.getYRot() - this.yBob) * 0.5F;
      } else {
         super.applyInput();
      }
   }

   private Vec2 modifyInput(final Vec2 input) {
      if (input.lengthSquared() == 0.0F) {
         return input;
      }

      Vec2 newInput = input.scale(0.98F);
      if (this.isUsingItem() && !this.isPassenger()) {
         newInput = newInput.scale(this.itemUseSpeedMultiplier());
      }

      if (this.isMovingSlowly()) {
         float sneakingMovementFactor = (float)this.getAttributeValue(Attributes.SNEAKING_SPEED);
         newInput = newInput.scale(sneakingMovementFactor);
      }

      return modifyInputSpeedForSquareMovement(newInput);
   }

   private static Vec2 modifyInputSpeedForSquareMovement(final Vec2 input) {
      float length = input.length();
      if (length <= 0.0F) {
         return input;
      }

      Vec2 direction = input.scale(1.0F / length);
      float distanceToUnitSquare = distanceToUnitSquare(direction);
      float modifiedLength = Math.min(length * distanceToUnitSquare, 1.0F);
      return direction.scale(modifiedLength);
   }

   private static float distanceToUnitSquare(final Vec2 direction) {
      float directionX = Math.abs(direction.x);
      float directionY = Math.abs(direction.y);
      float tan = directionY > directionX ? directionX / directionY : directionY / directionX;
      return Mth.sqrt(1.0F + Mth.square(tan));
   }

   protected boolean isControlledCamera() {
      return this.minecraft.getCameraEntity() == this;
   }

   public void resetPos() {
      this.setPose(Pose.STANDING);
      if (this.level() != null) {
         for (double testY = this.getY(); testY > this.level().getMinY() && testY <= this.level().getMaxY(); testY++) {
            this.setPos(this.getX(), testY, this.getZ());
            if (this.level().noCollision(this)) {
               break;
            }
         }

         this.setDeltaMovement(Vec3.ZERO);
         this.setXRot(0.0F);
      }

      this.setHealth(this.getMaxHealth());
      this.deathTime = 0;
   }

   @Override
   public void aiStep() {
      if (this.sprintTriggerTime > 0) {
         this.sprintTriggerTime--;
      }

      if (!(this.minecraft.gui.screen() instanceof LevelLoadingScreen)) {
         this.handlePortalTransitionEffect(this.getActivePortalLocalTransition() == Portal.Transition.CONFUSION);
         this.processPortalCooldown();
      }

      boolean wasJumping = this.input.keyPresses.jump();
      boolean wasShiftKeyDown = this.input.keyPresses.shift();
      boolean hasForwardImpulse = this.input.hasForwardImpulse();
      Abilities abilities = this.getAbilities();
      this.crouching = !abilities.flying
         && !this.isSwimming()
         && !this.isPassenger()
         && this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)
         && (this.isShiftKeyDown() || !this.isSleeping() && !this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING));
      this.input.tick();
      this.minecraft.getTutorial().onInput(this.input);
      boolean wasAutoJump = false;
      if (this.autoJumpTime > 0) {
         this.autoJumpTime--;
         wasAutoJump = true;
         this.input.makeJump();
      }

      if (!this.noPhysics) {
         this.moveTowardsClosestSpace(this.getX() - this.getBbWidth() * 0.35, this.getZ() + this.getBbWidth() * 0.35);
         this.moveTowardsClosestSpace(this.getX() - this.getBbWidth() * 0.35, this.getZ() - this.getBbWidth() * 0.35);
         this.moveTowardsClosestSpace(this.getX() + this.getBbWidth() * 0.35, this.getZ() - this.getBbWidth() * 0.35);
         this.moveTowardsClosestSpace(this.getX() + this.getBbWidth() * 0.35, this.getZ() + this.getBbWidth() * 0.35);
      }

      if (wasShiftKeyDown || this.isSlowDueToUsingItem() && !this.isPassenger() || this.input.keyPresses.backward()) {
         this.sprintTriggerTime = 0;
      }

      if (this.canStartSprinting()) {
         if (!hasForwardImpulse) {
            if (this.sprintTriggerTime > 0) {
               this.setSprinting(true);
            } else {
               this.sprintTriggerTime = this.minecraft.options.sprintWindow().get();
            }
         }

         if (this.input.keyPresses.sprint()) {
            this.setSprinting(true);
         }
      }

      if (this.isSprinting()) {
         if (this.isSwimming()) {
            if (this.shouldStopSwimSprinting()) {
               this.setSprinting(false);
            }
         } else if (this.shouldStopRunSprinting()) {
            this.setSprinting(false);
         }
      }

      boolean justToggledCreativeFlight = false;
      if (abilities.mayfly) {
         if (this.minecraft.gameMode.isSpectator()) {
            if (!abilities.flying) {
               abilities.flying = true;
               justToggledCreativeFlight = true;
               this.onUpdateAbilities();
            }
         } else if (!wasJumping && this.input.keyPresses.jump() && !wasAutoJump) {
            if (this.jumpTriggerTime == 0) {
               this.jumpTriggerTime = 7;
            } else if (!this.isSwimming() && (this.getVehicle() == null || this.jumpableVehicle() != null)) {
               abilities.flying = !abilities.flying;
               if (abilities.flying && this.onGround()) {
                  this.jumpFromGround();
               }

               justToggledCreativeFlight = true;
               this.onUpdateAbilities();
               this.jumpTriggerTime = 0;
            }
         }
      }

      if (this.input.keyPresses.jump() && !justToggledCreativeFlight && !wasJumping && !this.onClimbable() && this.tryToStartFallFlying()) {
         this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
      }

      this.wasFallFlying = this.isFallFlying();
      if (this.isInWater() && this.input.keyPresses.shift() && this.isAffectedByFluids()) {
         this.goDownInWater();
      }

      if (this.isEyeInFluid(FluidTags.WATER)) {
         int speed = this.isSpectator() ? 10 : 1;
         this.waterVisionTime = Mth.clamp(this.waterVisionTime + speed, 0, 600);
      } else if (this.waterVisionTime > 0) {
         this.isEyeInFluid(FluidTags.WATER);
         this.waterVisionTime = Mth.clamp(this.waterVisionTime - 10, 0, 600);
      }

      if (abilities.flying && this.isControlledCamera()) {
         int inputYa = 0;
         if (this.input.keyPresses.shift()) {
            inputYa--;
         }

         if (this.input.keyPresses.jump()) {
            inputYa++;
         }

         if (inputYa != 0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, inputYa * abilities.getFlyingSpeed() * 3.0F, 0.0));
         }
      }

      PlayerRideableJumping jumpableVehicle = this.jumpableVehicle();
      if (jumpableVehicle != null && jumpableVehicle.getJumpCooldown() == 0) {
         if (this.jumpRidingTicks < 0) {
            this.jumpRidingTicks++;
            if (this.jumpRidingTicks == 0) {
               this.jumpRidingScale = 0.0F;
            }
         }

         if (wasJumping && !this.input.keyPresses.jump()) {
            this.jumpRidingTicks = -10;
            jumpableVehicle.onPlayerJump(Mth.floor(this.getJumpRidingScale() * 100.0F));
            this.sendRidingJump();
         } else if (!wasJumping && this.input.keyPresses.jump()) {
            this.jumpRidingTicks = 0;
            this.jumpRidingScale = 0.0F;
         } else if (wasJumping) {
            this.jumpRidingTicks++;
            if (this.jumpRidingTicks < 10) {
               this.jumpRidingScale = this.jumpRidingTicks * 0.1F;
            } else {
               this.jumpRidingScale = 0.8F + 2.0F / (this.jumpRidingTicks - 9) * 0.1F;
            }
         }
      } else {
         this.jumpRidingScale = 0.0F;
      }

      super.aiStep();
      if (this.onGround() && abilities.flying && !this.minecraft.gameMode.isSpectator()) {
         abilities.flying = false;
         this.onUpdateAbilities();
      }
   }

   private boolean shouldStopRunSprinting() {
      return !this.isSprintingPossible(this.getAbilities().flying)
         || !this.input.hasForwardImpulse()
         || this.horizontalCollision && !this.minorHorizontalCollision;
   }

   private boolean shouldStopSwimSprinting() {
      return !this.isSprintingPossible(true) || !this.isInWater() || !this.input.hasForwardImpulse() && !this.onGround() && !this.input.keyPresses.shift();
   }

   public Portal.Transition getActivePortalLocalTransition() {
      return this.portalProcess == null ? Portal.Transition.NONE : this.portalProcess.getPortalLocalTransition();
   }

   @Override
   protected void tickDeath() {
      this.deathTime++;
      if (this.deathTime == 20) {
         this.remove(Entity.RemovalReason.KILLED);
      }
   }

   private void handlePortalTransitionEffect(final boolean active) {
      this.oPortalEffectIntensity = this.portalEffectIntensity;
      float step = 0.0F;
      if (active && this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
         if (this.minecraft.gui.screen() != null && !this.minecraft.gui.screen().isAllowedInPortal()) {
            if (this.minecraft.gui.screen() instanceof AbstractContainerScreen) {
               this.closeContainer();
            }

            this.minecraft.gui.setScreen(null);
         }

         if (this.portalEffectIntensity == 0.0F) {
            this.minecraft
               .getSoundManager()
               .play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRIGGER, this.random.nextFloat() * 0.4F + 0.8F, 0.25F));
         }

         step = 0.0125F;
         this.portalProcess.setAsInsidePortalThisTick(false);
      } else if (this.portalEffectIntensity > 0.0F) {
         step = -0.05F;
      }

      this.portalEffectIntensity = Mth.clamp(this.portalEffectIntensity + step, 0.0F, 1.0F);
   }

   @Override
   public void rideTick() {
      super.rideTick();
      this.handsBusy = false;
      if (this.getControlledVehicle() instanceof AbstractBoat boat) {
         boat.setInput(this.input.keyPresses.left(), this.input.keyPresses.right(), this.input.keyPresses.forward(), this.input.keyPresses.backward());
         this.handsBusy = this.handsBusy
            | (this.input.keyPresses.left() || this.input.keyPresses.right() || this.input.keyPresses.forward() || this.input.keyPresses.backward());
      }
   }

   public boolean isHandsBusy() {
      return this.handsBusy;
   }

   @Override
   public void move(final MoverType moverType, final Vec3 delta) {
      double prevX = this.getX();
      double prevZ = this.getZ();
      super.move(moverType, delta);
      float deltaX = (float)(this.getX() - prevX);
      float deltaZ = (float)(this.getZ() - prevZ);
      this.updateAutoJump(deltaX, deltaZ);
      this.addWalkedDistance(Mth.length(deltaX, deltaZ) * 0.6F);
   }

   public boolean isAutoJumpEnabled() {
      return this.autoJumpEnabled;
   }

   @Override
   public boolean shouldRotateWithMinecart() {
      return this.minecraft.options.rotateWithMinecart().get();
   }

   protected void updateAutoJump(final float xa, final float za) {
      if (this.canAutoJump()) {
         Vec3 moveBegin = this.position();
         Vec3 moveEnd = moveBegin.add(xa, 0.0, za);
         Vec3 moveDiff = new Vec3(xa, 0.0, za);
         float currentSpeed = this.getSpeed();
         float moveDistSq = (float)moveDiff.lengthSqr();
         if (moveDistSq <= 0.001F) {
            Vec2 move = this.input.getMoveVector();
            float inputXa = currentSpeed * move.x;
            float inputZa = currentSpeed * move.y;
            float sin = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
            float cos = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
            moveDiff = new Vec3(inputXa * cos - inputZa * sin, moveDiff.y, inputZa * cos + inputXa * sin);
            moveDistSq = (float)moveDiff.lengthSqr();
            if (moveDistSq <= 0.001F) {
               return;
            }
         }

         float moveDistInverted = Mth.invSqrt(moveDistSq);
         Vec3 moveDir = moveDiff.scale(moveDistInverted);
         Vec3 facingDir3 = this.getForward();
         float facingVsMovingDotProduct2 = (float)(facingDir3.x * moveDir.x + facingDir3.z * moveDir.z);
         if (!(facingVsMovingDotProduct2 < -0.15F)) {
            CollisionContext context = CollisionContext.of(this);
            BlockPos ceilingPos = BlockPos.containing(this.getX(), this.getBoundingBox().maxY, this.getZ());
            BlockState aboveBlock1 = this.level().getBlockState(ceilingPos);
            if (aboveBlock1.getCollisionShape(this.level(), ceilingPos, context).isEmpty()) {
               ceilingPos = ceilingPos.above();
               BlockState aboveBlock2 = this.level().getBlockState(ceilingPos);
               if (aboveBlock2.getCollisionShape(this.level(), ceilingPos, context).isEmpty()) {
                  float lookAheadSteps = 7.0F;
                  float jumpHeight = 1.2F;
                  if (this.hasEffect(MobEffects.JUMP_BOOST)) {
                     jumpHeight += (this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.75F;
                  }

                  float lookAheadDist = Math.max(currentSpeed * 7.0F, 1.0F / moveDistInverted);
                  Vec3 segBegin = moveBegin;
                  Vec3 segEnd = moveEnd.add(moveDir.scale(lookAheadDist));
                  float playerWidth = this.getBbWidth();
                  float playerHeight = this.getBbHeight();
                  AABB testBox = new AABB(segBegin, segEnd.add(0.0, playerHeight, 0.0)).inflate(playerWidth, 0.0, playerWidth);
                  segBegin = segBegin.add(0.0, 0.51F, 0.0);
                  segEnd = segEnd.add(0.0, 0.51F, 0.0);
                  Vec3 rightDir = moveDir.cross(new Vec3(0.0, 1.0, 0.0));
                  Vec3 rightOffset = rightDir.scale(playerWidth * 0.5F);
                  Vec3 leftSegBegin = segBegin.subtract(rightOffset);
                  Vec3 leftSegEnd = segEnd.subtract(rightOffset);
                  Vec3 rightSegBegin = segBegin.add(rightOffset);
                  Vec3 rightSegEnd = segEnd.add(rightOffset);
                  Iterable<VoxelShape> collisions = this.level().getCollisions(this, testBox);
                  Iterator<AABB> shape = StreamSupport.stream(collisions.spliterator(), false).flatMap(s -> s.toAabbs().stream()).iterator();
                  float obstacleHeight = Float.MIN_VALUE;

                  while (shape.hasNext()) {
                     AABB box = shape.next();
                     if (box.intersects(leftSegBegin, leftSegEnd) || box.intersects(rightSegBegin, rightSegEnd)) {
                        obstacleHeight = (float)box.maxY;
                        Vec3 obstacleShapeCenter = box.getCenter();
                        BlockPos obstacleBlockPos = BlockPos.containing(obstacleShapeCenter);

                        for (int steps = 1; steps < jumpHeight; steps++) {
                           BlockPos abovePos1 = obstacleBlockPos.above(steps);
                           BlockState aboveBlock = this.level().getBlockState(abovePos1);
                           VoxelShape blockShape;
                           if (!(blockShape = aboveBlock.getCollisionShape(this.level(), abovePos1, context)).isEmpty()) {
                              obstacleHeight = (float)blockShape.max(Direction.Axis.Y) + abovePos1.getY();
                              if (obstacleHeight - this.getY() > jumpHeight) {
                                 return;
                              }
                           }

                           if (steps > 1) {
                              ceilingPos = ceilingPos.above();
                              BlockState aboveBlock3 = this.level().getBlockState(ceilingPos);
                              if (!aboveBlock3.getCollisionShape(this.level(), ceilingPos, context).isEmpty()) {
                                 return;
                              }
                           }
                        }
                        break;
                     }
                  }

                  if (obstacleHeight != Float.MIN_VALUE) {
                     float ydelta = (float)(obstacleHeight - this.getY());
                     if (!(ydelta <= 0.5F) && !(ydelta > jumpHeight)) {
                        this.autoJumpTime = 1;
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected boolean isHorizontalCollisionMinor(final Vec3 movement) {
      float yRotInRadians = this.getYRot() * (float) (Math.PI / 180.0);
      double yRotSin = Mth.sin(yRotInRadians);
      double yRotCos = Mth.cos(yRotInRadians);
      double globalXA = this.xxa * yRotCos - this.zza * yRotSin;
      double globalZA = this.zza * yRotCos + this.xxa * yRotSin;
      double aLengthSquared = Mth.square(globalXA) + Mth.square(globalZA);
      double movementLengthSquared = Mth.square(movement.x) + Mth.square(movement.z);
      if (!(aLengthSquared < 1.0E-5F) && !(movementLengthSquared < 1.0E-5F)) {
         double dotProduct = globalXA * movement.x + globalZA * movement.z;
         double angleBetweenDesiredAndActualMovement = Math.acos(dotProduct / Math.sqrt(aLengthSquared * movementLengthSquared));
         return angleBetweenDesiredAndActualMovement < 0.13962634F;
      } else {
         return false;
      }
   }

   private boolean canAutoJump() {
      return this.isAutoJumpEnabled()
         && this.autoJumpTime <= 0
         && this.onGround()
         && !this.isStayingOnGroundSurface()
         && !this.isPassenger()
         && this.isMoving()
         && this.getBlockJumpFactor() >= 1.0;
   }

   private boolean isMoving() {
      return this.input.getMoveVector().lengthSquared() > 0.0F;
   }

   private boolean isSprintingPossible(final boolean allowedInShallowWater) {
      return !this.isMobilityRestricted()
         && (this.isPassenger() ? this.vehicleCanSprint(this.getVehicle()) : this.hasEnoughFoodToDoExhaustiveManoeuvres())
         && (allowedInShallowWater || !this.isInShallowWater());
   }

   private boolean canStartSprinting() {
      return !this.isSprinting()
         && this.input.hasForwardImpulse()
         && this.isSprintingPossible(this.getAbilities().flying)
         && !this.isSlowDueToUsingItem()
         && (!this.isFallFlying() || this.isUnderWater())
         && (!this.isMovingSlowly() || this.isUnderWater());
   }

   private boolean vehicleCanSprint(final Entity vehicle) {
      return vehicle.canSprint() && vehicle.isLocalInstanceAuthoritative();
   }

   public float getWaterVision() {
      if (!this.isEyeInFluid(FluidTags.WATER)) {
         return 0.0F;
      }

      float max = 600.0F;
      float mid = 100.0F;
      if (this.waterVisionTime >= 600.0F) {
         return 1.0F;
      }

      float a = Mth.clamp(this.waterVisionTime / 100.0F, 0.0F, 1.0F);
      float b = this.waterVisionTime < 100.0F ? 0.0F : Mth.clamp((this.waterVisionTime - 100.0F) / 500.0F, 0.0F, 1.0F);
      return a * 0.6F + b * 0.39999998F;
   }

   public void onGameModeChanged(final GameType gameType) {
      if (gameType == GameType.SPECTATOR) {
         this.setDeltaMovement(this.getDeltaMovement().with(Direction.Axis.Y, 0.0));
      }
   }

   @Override
   public boolean isUnderWater() {
      return this.wasUnderwater;
   }

   @Override
   protected boolean updateIsUnderwater() {
      boolean oldIsUnderwater = this.wasUnderwater;
      boolean newIsUnderwater = super.updateIsUnderwater();
      if (this.isSpectator()) {
         return this.wasUnderwater;
      }

      if (!oldIsUnderwater && newIsUnderwater) {
         this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         this.minecraft.getSoundManager().play(UnderLiquidAmbientSoundInstance.underwater(this));
      }

      if (oldIsUnderwater && !newIsUnderwater) {
         this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.AMBIENT, 1.0F, 1.0F, false);
      }

      return this.wasUnderwater;
   }

   @Override
   public Vec3 getRopeHoldPosition(final float partialTickTime) {
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         float yRot = Mth.lerp(partialTickTime * 0.5F, this.getYRot(), this.yRotO) * (float) (Math.PI / 180.0);
         float xRot = Mth.lerp(partialTickTime * 0.5F, this.getXRot(), this.xRotO) * (float) (Math.PI / 180.0);
         double handDir = this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0;
         Vec3 offset = new Vec3(0.39 * handDir, -0.6, 0.3);
         return offset.xRot(-xRot).yRot(-yRot).add(this.getEyePosition(partialTickTime));
      } else {
         return super.getRopeHoldPosition(partialTickTime);
      }
   }

   @Override
   public void updateTutorialInventoryAction(final ItemStack itemCarried, final ItemStack itemInSlot, final ClickAction clickAction) {
      this.minecraft.getTutorial().onInventoryAction(itemCarried, itemInSlot, clickAction);
   }

   @Override
   public float getVisualRotationYInDegrees() {
      return this.getYRot();
   }

   @Override
   public void handleCreativeModeItemDrop(final ItemStack stack) {
      this.minecraft.gameMode.handleCreativeModeItemDrop(stack);
   }

   @Override
   public boolean canDropItems() {
      return this.dropSpamThrottler.isUnderThreshold();
   }

   public TickThrottler getDropSpamThrottler() {
      return this.dropSpamThrottler;
   }

   public Input getLastSentInput() {
      return this.lastSentInput;
   }

   public HitResult raycastHitResult(final float a, final Entity cameraEntity) {
      ItemStack itemStack = this.getActiveItem();
      AttackRange itemAttackRange = itemStack.get(DataComponents.ATTACK_RANGE);
      double blockInteractionRange = this.blockInteractionRange();
      HitResult hitResult = null;
      if (itemAttackRange != null) {
         hitResult = itemAttackRange.getClosesetHit(cameraEntity, a, EntitySelector.CAN_BE_PICKED);
         if (hitResult instanceof BlockHitResult) {
            hitResult = filterHitResult(hitResult, cameraEntity.getEyePosition(a), blockInteractionRange);
         }
      }

      if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
         double entityInteractionRange = this.entityInteractionRange();
         hitResult = pick(cameraEntity, blockInteractionRange, entityInteractionRange, a);
      }

      return hitResult;
   }

   private static HitResult pick(final Entity cameraEntity, final double blockInteractionRange, final double entityInteractionRange, final float partialTicks) {
      double maxDistance = Math.max(blockInteractionRange, entityInteractionRange);
      double maxDistanceSq = Mth.square(maxDistance);
      Vec3 from = cameraEntity.getEyePosition(partialTicks);
      HitResult blockHitResult = cameraEntity.pick(maxDistance, partialTicks, false);
      double blockDistanceSq = blockHitResult.getLocation().distanceToSqr(from);
      if (blockHitResult.getType() != HitResult.Type.MISS) {
         maxDistanceSq = blockDistanceSq;
         maxDistance = Math.sqrt(maxDistanceSq);
      }

      Vec3 direction = cameraEntity.getViewVector(partialTicks);
      Vec3 to = from.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance);
      float overlap = 1.0F;
      AABB box = cameraEntity.getBoundingBox().expandTowards(direction.scale(maxDistance)).inflate(1.0, 1.0, 1.0);
      EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(cameraEntity, from, to, box, EntitySelector.CAN_BE_PICKED, maxDistanceSq);
      return entityHitResult != null && entityHitResult.getLocation().distanceToSqr(from) < blockDistanceSq
         ? filterHitResult(entityHitResult, from, entityInteractionRange)
         : filterHitResult(blockHitResult, from, blockInteractionRange);
   }

   private static HitResult filterHitResult(final HitResult hitResult, final Vec3 from, final double maxRange) {
      Vec3 hitLocation = hitResult.getLocation();
      if (!hitLocation.closerThan(from, maxRange)) {
         Vec3 location = hitResult.getLocation();
         Direction direction = Direction.getApproximateNearest(location.x - from.x, location.y - from.y, location.z - from.z);
         return BlockHitResult.miss(location, direction, BlockPos.containing(location));
      } else {
         return hitResult;
      }
   }

   public void setActivePostEffects(final List<Identifier> postEffects) {
      this.postEffects.clear();
      this.postEffects.addAll(postEffects);
   }

   public List<Identifier> getActivePostEffects() {
      return this.postEffects;
   }
}
