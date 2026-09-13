package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GameTestHelper {
   private final GameTestInfo testInfo;
   private boolean finalCheckAdded;

   public GameTestHelper(GameTestInfo p_127597_) {
      this.testInfo = p_127597_;
   }

   public GameTestAssertException assertionException(Component p_396338_) {
      return new GameTestAssertException(p_396338_, this.testInfo.getTick());
   }

   public GameTestAssertException assertionException(String p_391256_, Object... p_396991_) {
      return this.assertionException(Component.translatableEscape(p_391256_, p_396991_));
   }

   public GameTestAssertPosException assertionException(BlockPos p_397296_, Component p_395659_) {
      return new GameTestAssertPosException(p_395659_, this.absolutePos(p_397296_), p_397296_, this.testInfo.getTick());
   }

   public GameTestAssertPosException assertionException(BlockPos p_395825_, String p_391999_, Object... p_394029_) {
      return this.assertionException(p_395825_, Component.translatableEscape(p_391999_, p_394029_));
   }

   public ServerLevel getLevel() {
      return this.testInfo.getLevel();
   }

   public BlockState getBlockState(BlockPos p_177233_) {
      return this.getLevel().getBlockState(this.absolutePos(p_177233_));
   }

   public <T extends BlockEntity> T getBlockEntity(BlockPos p_177348_, Class<T> p_395786_) {
      BlockEntity blockentity = this.getLevel().getBlockEntity(this.absolutePos(p_177348_));
      if (blockentity == null) {
         throw this.assertionException(p_177348_, "test.error.missing_block_entity");
      } else if (p_395786_.isInstance(blockentity)) {
         return p_395786_.cast(blockentity);
      } else {
         throw this.assertionException(p_177348_, "test.error.wrong_block_entity", blockentity.getType().builtInRegistryHolder().getRegisteredName());
      }
   }

   public void killAllEntities() {
      this.killAllEntitiesOfClass(Entity.class);
   }

   public void killAllEntitiesOfClass(Class<? extends Entity> p_289538_) {
      AABB aabb = this.getBounds();
      List<? extends Entity> list = this.getLevel().getEntitiesOfClass(p_289538_, aabb.inflate(1.0), p_177131_ -> !(p_177131_ instanceof Player));
      list.forEach(p_358470_ -> p_358470_.kill(this.getLevel()));
   }

   public ItemEntity spawnItem(Item p_329778_, Vec3 p_334689_) {
      ServerLevel serverlevel = this.getLevel();
      Vec3 vec3 = this.absoluteVec(p_334689_);
      ItemEntity itementity = new ItemEntity(serverlevel, vec3.x, vec3.y, vec3.z, new ItemStack(p_329778_, 1));
      itementity.setDeltaMovement(0.0, 0.0, 0.0);
      serverlevel.addFreshEntity(itementity);
      return itementity;
   }

   public ItemEntity spawnItem(Item p_177190_, float p_177191_, float p_177192_, float p_177193_) {
      return this.spawnItem(p_177190_, new Vec3(p_177191_, p_177192_, p_177193_));
   }

   public ItemEntity spawnItem(Item p_251435_, BlockPos p_250287_) {
      return this.spawnItem(p_251435_, p_250287_.getX(), p_250287_.getY(), p_250287_.getZ());
   }

   public <E extends Entity> E spawn(EntityType<E> p_177177_, BlockPos p_177178_) {
      return this.spawn(p_177177_, Vec3.atBottomCenterOf(p_177178_));
   }

   public <E extends Entity> List<E> spawn(EntityType<E> p_429726_, BlockPos p_426571_, int p_423051_) {
      return this.spawn(p_429726_, Vec3.atBottomCenterOf(p_426571_), p_423051_);
   }

   public <E extends Entity> List<E> spawn(EntityType<E> p_425897_, Vec3 p_423085_, int p_427563_) {
      List<E> list = new ArrayList<>();

      for (int i = 0; i < p_427563_; i++) {
         list.add(this.spawn(p_425897_, p_423085_));
      }

      return list;
   }

   public <E extends Entity> E spawn(EntityType<E> p_177174_, Vec3 p_177175_) {
      return this.spawn(p_177174_, p_177175_, null);
   }

   public <E extends Entity> E spawn(EntityType<E> p_454466_, Vec3 p_450489_, @Nullable EntitySpawnReason p_450793_) {
      ServerLevel serverlevel = this.getLevel();
      E e = p_454466_.create(serverlevel, EntitySpawnReason.STRUCTURE);
      if (e == null) {
         throw this.assertionException(BlockPos.containing(p_450489_), "test.error.spawn_failure", p_454466_.builtInRegistryHolder().getRegisteredName());
      }

      if (e instanceof Mob mob) {
         mob.setPersistenceRequired();
      }

      Vec3 vec3 = this.absoluteVec(p_450489_);
      float f = e.rotate(this.getTestRotation());
      e.snapTo(vec3.x, vec3.y, vec3.z, f, e.getXRot());
      e.setYBodyRot(f);
      e.setYHeadRot(f);
      if (p_450793_ != null && e instanceof Mob mob1) {
         mob1.finalizeSpawn(this.getLevel(), this.getLevel().getCurrentDifficultyAt(mob1.blockPosition()), p_450793_, null);
      }

      serverlevel.addFreshEntityWithPassengers(e);
      return e;
   }

   public <E extends Mob> E spawn(EntityType<E> p_454689_, int p_460056_, int p_454763_, int p_454773_, EntitySpawnReason p_457711_) {
      return this.spawn(p_454689_, new Vec3(p_460056_, p_454763_, p_454773_), p_457711_);
   }

   public void hurt(Entity p_369429_, DamageSource p_367525_, float p_361355_) {
      p_369429_.hurtServer(this.getLevel(), p_367525_, p_361355_);
   }

   public void kill(Entity p_364190_) {
      p_364190_.kill(this.getLevel());
   }

   public <E extends Entity> E findOneEntity(EntityType<E> p_333077_) {
      return this.findClosestEntity(p_333077_, 0, 0, 0, 2.147483647E9);
   }

   public <E extends Entity> E findClosestEntity(EntityType<E> p_335109_, int p_329434_, int p_334603_, int p_333149_, double p_331586_) {
      List<E> list = this.findEntities(p_335109_, p_329434_, p_334603_, p_333149_, p_331586_);
      if (list.isEmpty()) {
         throw this.assertionException("test.error.expected_entity_around", p_335109_.getDescription(), p_329434_, p_334603_, p_333149_);
      }

      if (list.size() > 1) {
         throw this.assertionException("test.error.too_many_entities", p_335109_.toShortString(), p_329434_, p_334603_, p_333149_, list.size());
      }

      Vec3 vec3 = this.absoluteVec(new Vec3(p_329434_, p_334603_, p_333149_));
      list.sort((p_325933_, p_325934_) -> {
         double d0 = p_325933_.position().distanceTo(vec3);
         double d1 = p_325934_.position().distanceTo(vec3);
         return Double.compare(d0, d1);
      });
      return list.get(0);
   }

   public <E extends Entity> List<E> findEntities(EntityType<E> p_327745_, int p_330471_, int p_329385_, int p_328777_, double p_336258_) {
      return this.findEntities(p_327745_, Vec3.atBottomCenterOf(new BlockPos(p_330471_, p_329385_, p_328777_)), p_336258_);
   }

   public <E extends Entity> List<E> findEntities(EntityType<E> p_327849_, Vec3 p_331515_, double p_330795_) {
      ServerLevel serverlevel = this.getLevel();
      Vec3 vec3 = this.absoluteVec(p_331515_);
      AABB aabb = this.testInfo.getStructureBounds();
      AABB aabb1 = new AABB(vec3.add(-p_330795_, -p_330795_, -p_330795_), vec3.add(p_330795_, p_330795_, p_330795_));
      return serverlevel.getEntities(p_327849_, aabb, p_325936_ -> p_325936_.getBoundingBox().intersects(aabb1) && p_325936_.isAlive());
   }

   public <E extends Entity> E spawn(EntityType<E> p_177169_, int p_177170_, int p_177171_, int p_177172_) {
      return this.spawn(p_177169_, new BlockPos(p_177170_, p_177171_, p_177172_));
   }

   public <E extends Entity> E spawn(EntityType<E> p_177164_, float p_177165_, float p_177166_, float p_177167_) {
      return this.spawn(p_177164_, new Vec3(p_177165_, p_177166_, p_177167_));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177330_, BlockPos p_177331_) {
      E e = (E)this.spawn(p_177330_, p_177331_);
      e.removeFreeWill();
      return e;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177322_, int p_177323_, int p_177324_, int p_177325_) {
      return this.spawnWithNoFreeWill(p_177322_, new BlockPos(p_177323_, p_177324_, p_177325_));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177327_, Vec3 p_177328_) {
      E e = (E)this.spawn(p_177327_, p_177328_);
      e.removeFreeWill();
      return e;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177317_, float p_177318_, float p_177319_, float p_177320_) {
      return this.spawnWithNoFreeWill(p_177317_, new Vec3(p_177318_, p_177319_, p_177320_));
   }

   public void moveTo(Mob p_335410_, float p_330841_, float p_334132_, float p_332530_) {
      Vec3 vec3 = this.absoluteVec(new Vec3(p_330841_, p_334132_, p_332530_));
      p_335410_.snapTo(vec3.x, vec3.y, vec3.z, p_335410_.getYRot(), p_335410_.getXRot());
   }

   public GameTestSequence walkTo(Mob p_177186_, BlockPos p_177187_, float p_177188_) {
      return this.startSequence().thenExecuteAfter(2, () -> {
         Path path = p_177186_.getNavigation().createPath(this.absolutePos(p_177187_), 0);
         p_177186_.getNavigation().moveTo(path, p_177188_);
      });
   }

   public void pressButton(int p_177104_, int p_177105_, int p_177106_) {
      this.pressButton(new BlockPos(p_177104_, p_177105_, p_177106_));
   }

   public void pressButton(BlockPos p_177386_) {
      this.assertBlockTag(BlockTags.BUTTONS, p_177386_);
      BlockPos blockpos = this.absolutePos(p_177386_);
      BlockState blockstate = this.getLevel().getBlockState(blockpos);
      ButtonBlock buttonblock = (ButtonBlock)blockstate.getBlock();
      buttonblock.press(blockstate, this.getLevel(), blockpos, null);
   }

   public void useBlock(BlockPos p_177409_) {
      this.useBlock(p_177409_, this.makeMockPlayer(GameType.CREATIVE));
   }

   public void useBlock(BlockPos p_250131_, Player p_251507_) {
      BlockPos blockpos = this.absolutePos(p_250131_);
      this.useBlock(p_250131_, p_251507_, new BlockHitResult(Vec3.atCenterOf(blockpos), Direction.NORTH, blockpos, true));
   }

   public void useBlock(BlockPos p_262023_, Player p_261901_, BlockHitResult p_262040_) {
      BlockPos blockpos = this.absolutePos(p_262023_);
      BlockState blockstate = this.getLevel().getBlockState(blockpos);
      InteractionHand interactionhand = InteractionHand.MAIN_HAND;
      InteractionResult interactionresult = blockstate.useItemOn(
         p_261901_.getItemInHand(interactionhand), this.getLevel(), p_261901_, interactionhand, p_262040_
      );
      if (!interactionresult.consumesAction()
         && (
            !(interactionresult instanceof InteractionResult.TryEmptyHandInteraction)
               || !blockstate.useWithoutItem(this.getLevel(), p_261901_, p_262040_).consumesAction()
         )) {
         UseOnContext useoncontext = new UseOnContext(p_261901_, interactionhand, p_262040_);
         p_261901_.getItemInHand(interactionhand).useOn(useoncontext);
      }
   }

   public LivingEntity makeAboutToDrown(LivingEntity p_177184_) {
      p_177184_.setAirSupply(0);
      p_177184_.setHealth(0.25F);
      return p_177184_;
   }

   public LivingEntity withLowHealth(LivingEntity p_286794_) {
      p_286794_.setHealth(0.25F);
      return p_286794_;
   }

   public Player makeMockPlayer(final GameType p_333981_) {
      return new Player(this.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player")) {
         @Override
         public GameType gameMode() {
            return p_333981_;
         }

         @Override
         public boolean isClientAuthoritative() {
            return false;
         }
      };
   }

   public ServerPlayer makeMockServerPlayerInLevel() {
      CommonListenerCookie commonlistenercookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
      ServerPlayer serverplayer = new ServerPlayer(
         this.getLevel().getServer(), this.getLevel(), commonlistenercookie.gameProfile(), commonlistenercookie.clientInformation()
      ) {
         @Override
         public GameType gameMode() {
            return GameType.CREATIVE;
         }
      };
      Connection connection = new Connection(PacketFlow.SERVERBOUND);
      new EmbeddedChannel(new ChannelHandler[]{connection});
      this.getLevel().getServer().getPlayerList().placeNewPlayer(connection, serverplayer, commonlistenercookie);
      return serverplayer;
   }

   public void pullLever(int p_177303_, int p_177304_, int p_177305_) {
      this.pullLever(new BlockPos(p_177303_, p_177304_, p_177305_));
   }

   public void pullLever(BlockPos p_177422_) {
      this.assertBlockPresent(Blocks.LEVER, p_177422_);
      BlockPos blockpos = this.absolutePos(p_177422_);
      BlockState blockstate = this.getLevel().getBlockState(blockpos);
      LeverBlock leverblock = (LeverBlock)blockstate.getBlock();
      leverblock.pull(blockstate, this.getLevel(), blockpos, null);
   }

   public void pulseRedstone(BlockPos p_177235_, long p_177236_) {
      this.setBlock(p_177235_, Blocks.REDSTONE_BLOCK);
      this.runAfterDelay(p_177236_, () -> this.setBlock(p_177235_, Blocks.AIR));
   }

   public void destroyBlock(BlockPos p_177435_) {
      this.getLevel().destroyBlock(this.absolutePos(p_177435_), false, null);
   }

   public void setBlock(int p_177108_, int p_177109_, int p_177110_, Block p_177111_) {
      this.setBlock(new BlockPos(p_177108_, p_177109_, p_177110_), p_177111_);
   }

   public void setBlock(int p_177113_, int p_177114_, int p_177115_, BlockState p_177116_) {
      this.setBlock(new BlockPos(p_177113_, p_177114_, p_177115_), p_177116_);
   }

   public void setBlock(BlockPos p_177246_, Block p_177247_) {
      this.setBlock(p_177246_, p_177247_.defaultBlockState());
   }

   public void setBlock(BlockPos p_177253_, BlockState p_177254_) {
      this.getLevel().setBlock(this.absolutePos(p_177253_), p_177254_, 3);
   }

   public void setBlock(BlockPos p_427939_, Block p_424672_, Direction p_423634_) {
      this.setBlock(p_427939_, p_424672_.defaultBlockState(), p_423634_);
   }

   public void setBlock(BlockPos p_422292_, BlockState p_425624_, Direction p_429796_) {
      BlockState blockstate = p_425624_;
      if (p_425624_.hasProperty(HorizontalDirectionalBlock.FACING)) {
         blockstate = p_425624_.setValue(HorizontalDirectionalBlock.FACING, p_429796_);
      }

      if (p_425624_.hasProperty(BlockStateProperties.FACING)) {
         blockstate = p_425624_.setValue(BlockStateProperties.FACING, p_429796_);
      }

      this.getLevel().setBlock(this.absolutePos(p_422292_), blockstate, 3);
   }

   public void assertBlockPresent(Block p_177204_, int p_177205_, int p_177206_, int p_177207_) {
      this.assertBlockPresent(p_177204_, new BlockPos(p_177205_, p_177206_, p_177207_));
   }

   public void assertBlockPresent(Block p_177209_, BlockPos p_177210_) {
      BlockState blockstate = this.getBlockState(p_177210_);
      this.assertBlock(
         p_177210_,
         p_177216_ -> blockstate.is(p_177209_),
         p_389750_ -> Component.translatable("test.error.expected_block", p_177209_.getName(), p_389750_.getName())
      );
   }

   public void assertBlockNotPresent(Block p_177337_, int p_177338_, int p_177339_, int p_177340_) {
      this.assertBlockNotPresent(p_177337_, new BlockPos(p_177338_, p_177339_, p_177340_));
   }

   public void assertBlockNotPresent(Block p_177342_, BlockPos p_177343_) {
      this.assertBlock(
         p_177343_,
         p_177251_ -> !this.getBlockState(p_177343_).is(p_177342_),
         p_389752_ -> Component.translatable("test.error.unexpected_block", p_177342_.getName())
      );
   }

   public void assertBlockTag(TagKey<Block> p_395167_, BlockPos p_394420_) {
      this.assertBlockState(
         p_394420_,
         p_389762_ -> p_389762_.is(p_395167_),
         p_448752_ -> Component.translatable("test.error.expected_block_tag", Component.translationArg(p_395167_.location()), p_448752_.getBlock().getName())
      );
   }

   public void succeedWhenBlockPresent(Block p_177378_, int p_177379_, int p_177380_, int p_177381_) {
      this.succeedWhenBlockPresent(p_177378_, new BlockPos(p_177379_, p_177380_, p_177381_));
   }

   public void succeedWhenBlockPresent(Block p_177383_, BlockPos p_177384_) {
      this.succeedWhen(() -> this.assertBlockPresent(p_177383_, p_177384_));
   }

   public void assertBlock(BlockPos p_177272_, Predicate<Block> p_177273_, Function<Block, Component> p_395351_) {
      this.assertBlockState(p_177272_, p_177296_ -> p_177273_.test(p_177296_.getBlock()), p_389756_ -> p_395351_.apply(p_389756_.getBlock()));
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos p_177256_, Property<T> p_177257_, T p_177258_) {
      BlockState blockstate = this.getBlockState(p_177256_);
      boolean flag = blockstate.hasProperty(p_177257_);
      if (!flag) {
         throw this.assertionException(p_177256_, "test.error.block_property_missing", p_177257_.getName(), p_177258_);
      }

      if (!blockstate.<T>getValue(p_177257_).equals(p_177258_)) {
         throw this.assertionException(p_177256_, "test.error.block_property_mismatch", p_177257_.getName(), p_177258_, blockstate.getValue(p_177257_));
      }
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos p_177260_, Property<T> p_177261_, Predicate<T> p_177262_, Component p_393167_) {
      this.assertBlockState(p_177260_, p_277264_ -> {
         if (!p_277264_.hasProperty(p_177261_)) {
            return false;
         }

         T t = p_277264_.getValue(p_177261_);
         return p_177262_.test(t);
      }, p_397227_ -> p_393167_);
   }

   public void assertBlockState(BlockPos p_396349_, BlockState p_394832_) {
      BlockState blockstate = this.getBlockState(p_396349_);
      if (!blockstate.equals(p_394832_)) {
         throw this.assertionException(p_396349_, "test.error.state_not_equal", p_394832_, blockstate);
      }
   }

   public void assertBlockState(BlockPos p_177358_, Predicate<BlockState> p_177359_, Function<BlockState, Component> p_392520_) {
      BlockState blockstate = this.getBlockState(p_177358_);
      if (!p_177359_.test(blockstate)) {
         throw this.assertionException(p_177358_, p_392520_.apply(blockstate));
      }
   }

   public <T extends BlockEntity> void assertBlockEntityData(BlockPos p_345406_, Class<T> p_391986_, Predicate<T> p_342583_, Supplier<Component> p_343096_) {
      T t = this.getBlockEntity(p_345406_, p_391986_);
      if (!p_342583_.test(t)) {
         throw this.assertionException(p_345406_, p_343096_.get());
      }
   }

   public void assertRedstoneSignal(BlockPos p_289644_, Direction p_289642_, IntPredicate p_289645_, Supplier<Component> p_289684_) {
      BlockPos blockpos = this.absolutePos(p_289644_);
      ServerLevel serverlevel = this.getLevel();
      BlockState blockstate = serverlevel.getBlockState(blockpos);
      int i = blockstate.getSignal(serverlevel, blockpos, p_289642_);
      if (!p_289645_.test(i)) {
         throw this.assertionException(p_289644_, p_289684_.get());
      }
   }

   public void assertEntityPresent(EntityType<?> p_177157_) {
      if (!this.getLevel().hasEntities(p_177157_, this.getBounds(), Entity::isAlive)) {
         throw this.assertionException("test.error.expected_entity_in_test", p_177157_.getDescription());
      }
   }

   public void assertEntityPresent(EntityType<?> p_177370_, int p_177371_, int p_177372_, int p_177373_) {
      this.assertEntityPresent(p_177370_, new BlockPos(p_177371_, p_177372_, p_177373_));
   }

   public void assertEntityPresent(EntityType<?> p_177375_, BlockPos p_177376_) {
      BlockPos blockpos = this.absolutePos(p_177376_);
      if (!this.getLevel().hasEntities(p_177375_, new AABB(blockpos), Entity::isAlive)) {
         throw this.assertionException(p_177376_, "test.error.expected_entity", p_177375_.getDescription());
      }
   }

   public void assertEntityPresent(EntityType<?> p_252010_, AABB p_367168_) {
      AABB aabb = this.absoluteAABB(p_367168_);
      if (!this.getLevel().hasEntities(p_252010_, aabb, Entity::isAlive)) {
         throw this.assertionException(BlockPos.containing(p_367168_.getCenter()), "test.error.expected_entity", p_252010_.getDescription());
      }
   }

   public void assertEntityPresent(EntityType<?> p_455939_, AABB p_456245_, Component p_453058_) {
      AABB aabb = this.absoluteAABB(p_456245_);
      if (!this.getLevel().hasEntities(p_455939_, aabb, Entity::isAlive)) {
         throw this.assertionException(BlockPos.containing(p_456245_.getCenter()), p_453058_);
      }
   }

   public void assertEntitiesPresent(EntityType<?> p_313026_, int p_310037_) {
      List<? extends Entity> list = this.getLevel().getEntities(p_313026_, this.getBounds(), Entity::isAlive);
      if (list.size() != p_310037_) {
         throw this.assertionException("test.error.expected_entity_count", p_310037_, p_313026_.getDescription(), list.size());
      }
   }

   public void assertEntitiesPresent(EntityType<?> p_239372_, BlockPos p_239373_, int p_239374_, double p_239375_) {
      BlockPos blockpos = this.absolutePos(p_239373_);
      List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)p_239372_, p_239373_, p_239375_);
      if (list.size() != p_239374_) {
         throw this.assertionException(p_239373_, "test.error.expected_entity_count", p_239374_, p_239372_.getDescription(), list.size());
      }
   }

   public void assertEntityPresent(EntityType<?> p_177180_, BlockPos p_177181_, double p_177182_) {
      List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)p_177180_, p_177181_, p_177182_);
      if (list.isEmpty()) {
         BlockPos blockpos = this.absolutePos(p_177181_);
         throw this.assertionException(p_177181_, "test.error.expected_entity", p_177180_.getDescription());
      }
   }

   public <T extends Entity> List<T> getEntities(EntityType<T> p_238400_, BlockPos p_238401_, double p_238402_) {
      BlockPos blockpos = this.absolutePos(p_238401_);
      return this.getLevel().getEntities(p_238400_, new AABB(blockpos).inflate(p_238402_), Entity::isAlive);
   }

   public <T extends Entity> List<T> getEntities(EntityType<T> p_330219_) {
      return this.getLevel().getEntities(p_330219_, this.getBounds(), Entity::isAlive);
   }

   public void assertEntityInstancePresent(Entity p_177133_, int p_177134_, int p_177135_, int p_177136_) {
      this.assertEntityInstancePresent(p_177133_, new BlockPos(p_177134_, p_177135_, p_177136_));
   }

   public void assertEntityInstancePresent(Entity p_177141_, BlockPos p_177142_) {
      BlockPos blockpos = this.absolutePos(p_177142_);
      List<? extends Entity> list = this.getLevel().getEntities(p_177141_.getType(), new AABB(blockpos), Entity::isAlive);
      list.stream()
         .filter(p_177139_ -> p_177139_ == p_177141_)
         .findFirst()
         .orElseThrow(() -> this.assertionException(p_177142_, "test.error.expected_entity", p_177141_.getType().getDescription()));
   }

   public void assertItemEntityCountIs(Item p_177199_, BlockPos p_177200_, double p_177201_, int p_177202_) {
      BlockPos blockpos = this.absolutePos(p_177200_);
      List<ItemEntity> list = this.getLevel().getEntities(EntityType.ITEM, new AABB(blockpos).inflate(p_177201_), Entity::isAlive);
      int i = 0;

      for (ItemEntity itementity : list) {
         ItemStack itemstack = itementity.getItem();
         if (itemstack.is(p_177199_)) {
            i += itemstack.getCount();
         }
      }

      if (i != p_177202_) {
         throw this.assertionException(p_177200_, "test.error.expected_items_count", p_177202_, p_177199_.getName(), i);
      }
   }

   public void assertItemEntityPresent(Item p_177195_, BlockPos p_177196_, double p_177197_) {
      BlockPos blockpos = this.absolutePos(p_177196_);
      Predicate<ItemEntity> predicate = p_448748_ -> p_448748_.isAlive() && p_448748_.getItem().is(p_177195_);
      if (!this.getLevel().hasEntities(EntityType.ITEM, new AABB(blockpos).inflate(p_177197_), predicate)) {
         throw this.assertionException(p_177196_, "test.error.expected_item", p_177195_.getName());
      }
   }

   public void assertItemEntityNotPresent(Item p_236779_, BlockPos p_236780_, double p_236781_) {
      BlockPos blockpos = this.absolutePos(p_236780_);
      Predicate<ItemEntity> predicate = p_448746_ -> p_448746_.isAlive() && p_448746_.getItem().is(p_236779_);
      if (this.getLevel().hasEntities(EntityType.ITEM, new AABB(blockpos).inflate(p_236781_), predicate)) {
         throw this.assertionException(p_236780_, "test.error.unexpected_item", p_236779_.getName());
      }
   }

   public void assertItemEntityPresent(Item p_310630_) {
      Predicate<ItemEntity> predicate = p_448744_ -> p_448744_.isAlive() && p_448744_.getItem().is(p_310630_);
      if (!this.getLevel().hasEntities(EntityType.ITEM, this.getBounds(), predicate)) {
         throw this.assertionException("test.error.expected_item", p_310630_.getName());
      }
   }

   public void assertItemEntityNotPresent(Item p_312600_) {
      Predicate<ItemEntity> predicate = p_448750_ -> p_448750_.isAlive() && p_448750_.getItem().is(p_312600_);
      if (this.getLevel().hasEntities(EntityType.ITEM, this.getBounds(), predicate)) {
         throw this.assertionException("test.error.unexpected_item", p_312600_.getName());
      }
   }

   public void assertEntityNotPresent(EntityType<?> p_177310_) {
      List<? extends Entity> list = this.getLevel().getEntities(p_177310_, this.getBounds(), Entity::isAlive);
      if (!list.isEmpty()) {
         throw this.assertionException(list.getFirst().blockPosition(), "test.error.unexpected_entity", p_177310_.getDescription());
      }
   }

   public void assertEntityNotPresent(EntityType<?> p_177398_, int p_177399_, int p_177400_, int p_177401_) {
      this.assertEntityNotPresent(p_177398_, new BlockPos(p_177399_, p_177400_, p_177401_));
   }

   public void assertEntityNotPresent(EntityType<?> p_177403_, BlockPos p_177404_) {
      BlockPos blockpos = this.absolutePos(p_177404_);
      if (this.getLevel().hasEntities(p_177403_, new AABB(blockpos), Entity::isAlive)) {
         throw this.assertionException(p_177404_, "test.error.unexpected_entity", p_177403_.getDescription());
      }
   }

   public void assertEntityNotPresent(EntityType<?> p_328558_, AABB p_361757_) {
      AABB aabb = this.absoluteAABB(p_361757_);
      List<? extends Entity> list = this.getLevel().getEntities(p_328558_, aabb, Entity::isAlive);
      if (!list.isEmpty()) {
         throw this.assertionException(list.getFirst().blockPosition(), "test.error.unexpected_entity", p_328558_.getDescription());
      }
   }

   public void assertEntityTouching(EntityType<?> p_177159_, double p_177160_, double p_177161_, double p_177162_) {
      Vec3 vec3 = new Vec3(p_177160_, p_177161_, p_177162_);
      Vec3 vec31 = this.absoluteVec(vec3);
      Predicate<? super Entity> predicate = p_177346_ -> p_177346_.getBoundingBox().intersects(vec31, vec31);
      if (!this.getLevel().hasEntities(p_177159_, this.getBounds(), predicate)) {
         throw this.assertionException(
            "test.error.expected_entity_touching", p_177159_.getDescription(), vec31.x(), vec31.y(), vec31.z(), p_177160_, p_177161_, p_177162_
         );
      }
   }

   public void assertEntityNotTouching(EntityType<?> p_177312_, double p_177313_, double p_177314_, double p_177315_) {
      Vec3 vec3 = new Vec3(p_177313_, p_177314_, p_177315_);
      Vec3 vec31 = this.absoluteVec(vec3);
      Predicate<? super Entity> predicate = p_177231_ -> !p_177231_.getBoundingBox().intersects(vec31, vec31);
      if (!this.getLevel().hasEntities(p_177312_, this.getBounds(), predicate)) {
         throw this.assertionException(
            "test.error.expected_entity_not_touching", p_177312_.getDescription(), vec31.x(), vec31.y(), vec31.z(), p_177313_, p_177314_, p_177315_
         );
      }
   }

   public <E extends Entity, T> void assertEntityData(BlockPos p_362770_, EntityType<E> p_363965_, Predicate<E> p_367551_) {
      BlockPos blockpos = this.absolutePos(p_362770_);
      List<E> list = this.getLevel().getEntities(p_363965_, new AABB(blockpos), Entity::isAlive);
      if (list.isEmpty()) {
         throw this.assertionException(p_362770_, "test.error.expected_entity", p_363965_.getDescription());
      }

      for (E e : list) {
         if (!p_367551_.test(e)) {
            throw this.assertionException(e.blockPosition(), "test.error.expected_entity_data_predicate", e.getName());
         }
      }
   }

   public <E extends Entity, T> void assertEntityData(BlockPos p_177238_, EntityType<E> p_177239_, Function<? super E, T> p_177240_, @Nullable T p_177241_) {
      this.assertEntityData(new AABB(p_177238_), p_177239_, p_177240_, p_177241_);
   }

   public <E extends Entity, T> void assertEntityData(AABB p_460111_, EntityType<E> p_456561_, Function<? super E, T> p_458228_, @Nullable T p_451568_) {
      List<E> list = this.getLevel().getEntities(p_456561_, this.absoluteAABB(p_460111_), Entity::isAlive);
      if (list.isEmpty()) {
         throw this.assertionException(BlockPos.containing(p_460111_.getBottomCenter()), "test.error.expected_entity", p_456561_.getDescription());
      }

      for (E e : list) {
         T t = p_458228_.apply(e);
         if (!Objects.equals(t, p_451568_)) {
            throw this.assertionException(BlockPos.containing(p_460111_.getBottomCenter()), "test.error.expected_entity_data", p_451568_, t);
         }
      }
   }

   public <E extends LivingEntity> void assertEntityIsHolding(BlockPos p_263501_, EntityType<E> p_263510_, Item p_263517_) {
      BlockPos blockpos = this.absolutePos(p_263501_);
      List<E> list = this.getLevel().getEntities(p_263510_, new AABB(blockpos), Entity::isAlive);
      if (list.isEmpty()) {
         throw this.assertionException(p_263501_, "test.error.expected_entity", p_263510_.getDescription());
      }

      for (E e : list) {
         if (e.isHolding(p_263517_)) {
            return;
         }
      }

      throw this.assertionException(p_263501_, "test.error.expected_entity_holding", p_263517_.getName());
   }

   public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(BlockPos p_263495_, EntityType<E> p_263521_, Item p_263502_) {
      BlockPos blockpos = this.absolutePos(p_263495_);
      List<E> list = this.getLevel().getEntities(p_263521_, new AABB(blockpos), p_263479_ -> p_263479_.isAlive());
      if (list.isEmpty()) {
         throw this.assertionException(p_263495_, "test.error.expected_entity", p_263521_.getDescription());
      }

      for (E e : list) {
         if (e.getInventory().hasAnyMatching(p_263481_ -> p_263481_.is(p_263502_))) {
            return;
         }
      }

      throw this.assertionException(p_263495_, "test.error.expected_entity_having", p_263502_.getName());
   }

   public void assertContainerEmpty(BlockPos p_177441_) {
      BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(p_177441_, BaseContainerBlockEntity.class);
      if (!basecontainerblockentity.isEmpty()) {
         throw this.assertionException(p_177441_, "test.error.expected_empty_container");
      }
   }

   public void assertContainerContainsSingle(BlockPos p_393278_, Item p_397233_) {
      BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(p_393278_, BaseContainerBlockEntity.class);
      if (basecontainerblockentity.countItem(p_397233_) != 1) {
         throw this.assertionException(p_393278_, "test.error.expected_container_contents_single", p_397233_.getName());
      }
   }

   public void assertContainerContains(BlockPos p_177243_, Item p_177244_) {
      BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(p_177243_, BaseContainerBlockEntity.class);
      if (basecontainerblockentity.countItem(p_177244_) == 0) {
         throw this.assertionException(p_177243_, "test.error.expected_container_contents", p_177244_.getName());
      }
   }

   public void assertSameBlockStates(BoundingBox p_177225_, BlockPos p_177226_) {
      BlockPos.betweenClosedStream(p_177225_).forEach(p_177267_ -> {
         BlockPos blockpos = p_177226_.offset(p_177267_.getX() - p_177225_.minX(), p_177267_.getY() - p_177225_.minY(), p_177267_.getZ() - p_177225_.minZ());
         this.assertSameBlockState(p_177267_, blockpos);
      });
   }

   public void assertSameBlockState(BlockPos p_177269_, BlockPos p_177270_) {
      BlockState blockstate = this.getBlockState(p_177269_);
      BlockState blockstate1 = this.getBlockState(p_177270_);
      if (blockstate != blockstate1) {
         throw this.assertionException(p_177269_, "test.error.state_not_equal", blockstate1, blockstate);
      }
   }

   public void assertAtTickTimeContainerContains(long p_177124_, BlockPos p_177125_, Item p_177126_) {
      this.runAtTickTime(p_177124_, () -> this.assertContainerContainsSingle(p_177125_, p_177126_));
   }

   public void assertAtTickTimeContainerEmpty(long p_177121_, BlockPos p_177122_) {
      this.runAtTickTime(p_177121_, () -> this.assertContainerEmpty(p_177122_));
   }

   public <E extends Entity, T> void succeedWhenEntityData(BlockPos p_177350_, EntityType<E> p_177351_, Function<E, T> p_177352_, T p_177353_) {
      this.succeedWhen(() -> this.assertEntityData(p_177350_, p_177351_, p_177352_, p_177353_));
   }

   public <E extends Entity> void assertEntityProperty(E p_177148_, Predicate<E> p_393110_, Component p_397208_) {
      if (!p_393110_.test(p_177148_)) {
         throw this.assertionException(p_177148_.blockPosition(), "test.error.entity_property", p_177148_.getName(), p_397208_);
      }
   }

   public <E extends Entity, T> void assertEntityProperty(E p_177153_, Function<E, T> p_396986_, T p_393513_, Component p_392760_) {
      T t = p_396986_.apply(p_177153_);
      if (!t.equals(p_393513_)) {
         throw this.assertionException(p_177153_.blockPosition(), "test.error.entity_property_details", p_177153_.getName(), p_392760_, t, p_393513_);
      }
   }

   public void assertLivingEntityHasMobEffect(LivingEntity p_300128_, Holder<MobEffect> p_331754_, int p_298143_) {
      MobEffectInstance mobeffectinstance = p_300128_.getEffect(p_331754_);
      if (mobeffectinstance == null || mobeffectinstance.getAmplifier() != p_298143_) {
         throw this.assertionException("test.error.expected_entity_effect", p_300128_.getName(), PotionContents.getPotionDescription(p_331754_, p_298143_));
      }
   }

   public void succeedWhenEntityPresent(EntityType<?> p_177414_, int p_177415_, int p_177416_, int p_177417_) {
      this.succeedWhenEntityPresent(p_177414_, new BlockPos(p_177415_, p_177416_, p_177417_));
   }

   public void succeedWhenEntityPresent(EntityType<?> p_177419_, BlockPos p_177420_) {
      this.succeedWhen(() -> this.assertEntityPresent(p_177419_, p_177420_));
   }

   public void succeedWhenEntityNotPresent(EntityType<?> p_177427_, int p_177428_, int p_177429_, int p_177430_) {
      this.succeedWhenEntityNotPresent(p_177427_, new BlockPos(p_177428_, p_177429_, p_177430_));
   }

   public void succeedWhenEntityNotPresent(EntityType<?> p_177432_, BlockPos p_177433_) {
      this.succeedWhen(() -> this.assertEntityNotPresent(p_177432_, p_177433_));
   }

   public void succeed() {
      this.testInfo.succeed();
   }

   private void ensureSingleFinalCheck() {
      if (this.finalCheckAdded) {
         throw new IllegalStateException("This test already has final clause");
      }

      this.finalCheckAdded = true;
   }

   public void succeedIf(Runnable p_177280_) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(0L, p_177280_).thenSucceed();
   }

   public void succeedWhen(Runnable p_177362_) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(p_177362_).thenSucceed();
   }

   public void succeedOnTickWhen(int p_177118_, Runnable p_177119_) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(p_177118_, p_177119_).thenSucceed();
   }

   public void runAtTickTime(long p_177128_, Runnable p_177129_) {
      this.testInfo.setRunAtTickTime(p_177128_, p_177129_);
   }

   public void runAfterDelay(long p_177307_, Runnable p_177308_) {
      this.runAtTickTime(this.testInfo.getTick() + p_177307_, p_177308_);
   }

   public void randomTick(BlockPos p_177447_) {
      BlockPos blockpos = this.absolutePos(p_177447_);
      ServerLevel serverlevel = this.getLevel();
      serverlevel.getBlockState(blockpos).randomTick(serverlevel, blockpos, serverlevel.random);
   }

   public void tickBlock(BlockPos p_396550_) {
      BlockPos blockpos = this.absolutePos(p_396550_);
      ServerLevel serverlevel = this.getLevel();
      serverlevel.getBlockState(blockpos).tick(serverlevel, blockpos, serverlevel.random);
   }

   public void tickPrecipitation(BlockPos p_311105_) {
      BlockPos blockpos = this.absolutePos(p_311105_);
      ServerLevel serverlevel = this.getLevel();
      serverlevel.tickPrecipitation(blockpos);
   }

   public void tickPrecipitation() {
      AABB aabb = this.getRelativeBounds();
      int i = (int)Math.floor(aabb.maxX);
      int j = (int)Math.floor(aabb.maxZ);
      int k = (int)Math.floor(aabb.maxY);

      for (int l = (int)Math.floor(aabb.minX); l < i; l++) {
         for (int i1 = (int)Math.floor(aabb.minZ); i1 < j; i1++) {
            this.tickPrecipitation(new BlockPos(l, k, i1));
         }
      }
   }

   public int getHeight(Heightmap.Types p_236775_, int p_236776_, int p_236777_) {
      BlockPos blockpos = this.absolutePos(new BlockPos(p_236776_, 0, p_236777_));
      return this.relativePos(this.getLevel().getHeightmapPos(p_236775_, blockpos)).getY();
   }

   public void fail(Component p_397357_, BlockPos p_177291_) {
      throw this.assertionException(p_177291_, p_397357_);
   }

   public void fail(Component p_393216_, Entity p_177288_) {
      throw this.assertionException(p_177288_.blockPosition(), p_393216_);
   }

   public void fail(Component p_391587_) {
      throw this.assertionException(p_391587_);
   }

   public void fail(String p_428908_) {
      throw this.assertionException(Component.literal(p_428908_));
   }

   public void failIf(Runnable p_177393_) {
      this.testInfo.createSequence().thenWaitUntil(p_177393_).thenFail(() -> this.assertionException("test.error.fail"));
   }

   public void failIfEver(Runnable p_177411_) {
      LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks())
         .forEach(p_177365_ -> this.testInfo.setRunAtTickTime(p_177365_, p_177411_::run));
   }

   public GameTestSequence startSequence() {
      return this.testInfo.createSequence();
   }

   public BlockPos absolutePos(BlockPos p_177450_) {
      BlockPos blockpos = this.testInfo.getTestOrigin();
      BlockPos blockpos1 = blockpos.offset(p_177450_);
      return StructureTemplate.transform(blockpos1, Mirror.NONE, this.testInfo.getRotation(), blockpos);
   }

   public BlockPos relativePos(BlockPos p_177453_) {
      BlockPos blockpos = this.testInfo.getTestOrigin();
      Rotation rotation = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
      BlockPos blockpos1 = StructureTemplate.transform(p_177453_, Mirror.NONE, rotation, blockpos);
      return blockpos1.subtract(blockpos);
   }

   public AABB absoluteAABB(AABB p_369302_) {
      Vec3 vec3 = this.absoluteVec(p_369302_.getMinPosition());
      Vec3 vec31 = this.absoluteVec(p_369302_.getMaxPosition());
      return new AABB(vec3, vec31);
   }

   public AABB relativeAABB(AABB p_369720_) {
      Vec3 vec3 = this.relativeVec(p_369720_.getMinPosition());
      Vec3 vec31 = this.relativeVec(p_369720_.getMaxPosition());
      return new AABB(vec3, vec31);
   }

   public Vec3 absoluteVec(Vec3 p_177228_) {
      Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
      return StructureTemplate.transform(vec3.add(p_177228_), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
   }

   public Vec3 relativeVec(Vec3 p_251543_) {
      Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
      return StructureTemplate.transform(p_251543_.subtract(vec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
   }

   public Rotation getTestRotation() {
      return this.testInfo.getRotation();
   }

   public Direction getTestDirection() {
      return this.testInfo.getRotation().rotate(Direction.SOUTH);
   }

   public Direction getAbsoluteDirection(Direction p_457858_) {
      return this.getTestRotation().rotate(p_457858_);
   }

   public void assertTrue(boolean p_249380_, Component p_393681_) {
      if (!p_249380_) {
         throw this.assertionException(p_393681_);
      }
   }

   public void assertTrue(boolean p_456543_, String p_454418_) {
      this.assertTrue(p_456543_, Component.literal(p_454418_));
   }

   public <N> void assertValueEqual(N p_460719_, N p_452996_, String p_456600_) {
      this.assertValueEqual(p_460719_, p_452996_, Component.literal(p_456600_));
   }

   public <N> void assertValueEqual(N p_328559_, N p_332683_, Component p_397042_) {
      if (!p_328559_.equals(p_332683_)) {
         throw this.assertionException("test.error.value_not_equal", p_397042_, p_328559_, p_332683_);
      }
   }

   public void assertFalse(boolean p_277974_, Component p_392338_) {
      this.assertTrue(!p_277974_, p_392338_);
   }

   public void assertFalse(boolean p_451832_, String p_457647_) {
      this.assertFalse(p_451832_, Component.literal(p_457647_));
   }

   public long getTick() {
      return this.testInfo.getTick();
   }

   public AABB getBounds() {
      return this.testInfo.getStructureBounds();
   }

   public AABB getRelativeBounds() {
      AABB aabb = this.testInfo.getStructureBounds();
      Rotation rotation = this.testInfo.getRotation();
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            return new AABB(0.0, 0.0, 0.0, aabb.getZsize(), aabb.getYsize(), aabb.getXsize());
         default:
            return new AABB(0.0, 0.0, 0.0, aabb.getXsize(), aabb.getYsize(), aabb.getZsize());
      }
   }

   public void forEveryBlockInStructure(Consumer<BlockPos> p_177293_) {
      AABB aabb = this.getRelativeBounds().contract(1.0, 1.0, 1.0);
      BlockPos.MutableBlockPos.betweenClosedStream(aabb).forEach(p_177293_);
   }

   public void onEachTick(Runnable p_177424_) {
      LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks())
         .forEach(p_177283_ -> this.testInfo.setRunAtTickTime(p_177283_, p_177424_::run));
   }

   public void placeAt(Player p_261595_, ItemStack p_262007_, BlockPos p_261973_, Direction p_262008_) {
      BlockPos blockpos = this.absolutePos(p_261973_.relative(p_262008_));
      BlockHitResult blockhitresult = new BlockHitResult(Vec3.atCenterOf(blockpos), p_262008_, blockpos, false);
      UseOnContext useoncontext = new UseOnContext(p_261595_, InteractionHand.MAIN_HAND, blockhitresult);
      p_262007_.useOn(useoncontext);
   }

   public void setBiome(ResourceKey<Biome> p_312755_) {
      AABB aabb = this.getBounds();
      BlockPos blockpos = BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ);
      BlockPos blockpos1 = BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ);
      Either<Integer, CommandSyntaxException> either = FillBiomeCommand.fill(
         this.getLevel(), blockpos, blockpos1, this.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(p_312755_)
      );
      if (either.right().isPresent()) {
         throw this.assertionException("test.error.set_biome");
      }
   }
}
