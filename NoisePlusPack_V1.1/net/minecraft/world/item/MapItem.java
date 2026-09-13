package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

public class MapItem extends Item {
   public static final int IMAGE_WIDTH = 128;
   public static final int IMAGE_HEIGHT = 128;

   public MapItem(Item.Properties p_42847_) {
      super(p_42847_);
   }

   public static ItemStack create(ServerLevel p_398211_, int p_42888_, int p_42889_, byte p_42890_, boolean p_42891_, boolean p_42892_) {
      ItemStack itemstack = new ItemStack(Items.FILLED_MAP);
      MapId mapid = createNewSavedData(p_398211_, p_42888_, p_42889_, p_42890_, p_42891_, p_42892_, p_398211_.dimension());
      itemstack.set(DataComponents.MAP_ID, mapid);
      return itemstack;
   }

   public static @Nullable MapItemSavedData getSavedData(@Nullable MapId p_332257_, Level p_151130_) {
      return p_332257_ == null ? null : p_151130_.getMapData(p_332257_);
   }

   public static @Nullable MapItemSavedData getSavedData(ItemStack p_42854_, Level p_42855_) {
      MapId mapid = p_42854_.get(DataComponents.MAP_ID);
      return getSavedData(mapid, p_42855_);
   }

   private static MapId createNewSavedData(
      ServerLevel p_398225_, int p_151122_, int p_151123_, int p_151124_, boolean p_151125_, boolean p_151126_, ResourceKey<Level> p_151127_
   ) {
      MapItemSavedData mapitemsaveddata = MapItemSavedData.createFresh(p_151122_, p_151123_, (byte)p_151124_, p_151125_, p_151126_, p_151127_);
      MapId mapid = p_398225_.getFreeMapId();
      p_398225_.setMapData(mapid, mapitemsaveddata);
      return mapid;
   }

   public void update(Level p_42894_, Entity p_42895_, MapItemSavedData p_42896_) {
      if (p_42894_.dimension() == p_42896_.dimension && p_42895_ instanceof Player) {
         int i = 1 << p_42896_.scale;
         int j = p_42896_.centerX;
         int k = p_42896_.centerZ;
         int l = Mth.floor(p_42895_.getX() - j) / i + 64;
         int i1 = Mth.floor(p_42895_.getZ() - k) / i + 64;
         int j1 = 128 / i;
         if (p_42894_.dimensionType().hasCeiling()) {
            j1 /= 2;
         }

         MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = p_42896_.getHoldingPlayer((Player)p_42895_);
         mapitemsaveddata$holdingplayer.step++;
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();
         boolean flag = false;

         for (int k1 = l - j1 + 1; k1 < l + j1; k1++) {
            if ((k1 & 15) == (mapitemsaveddata$holdingplayer.step & 15) || flag) {
               flag = false;
               double d0 = 0.0;

               for (int l1 = i1 - j1 - 1; l1 < i1 + j1; l1++) {
                  if (k1 >= 0 && l1 >= -1 && k1 < 128 && l1 < 128) {
                     int i2 = Mth.square(k1 - l) + Mth.square(l1 - i1);
                     boolean flag1 = i2 > (j1 - 2) * (j1 - 2);
                     int j2 = (j / i + k1 - 64) * i;
                     int k2 = (k / i + l1 - 64) * i;
                     Multiset<MapColor> multiset = LinkedHashMultiset.create();
                     LevelChunk levelchunk = p_42894_.getChunk(SectionPos.blockToSectionCoord(j2), SectionPos.blockToSectionCoord(k2));
                     if (!levelchunk.isEmpty()) {
                        int l2 = 0;
                        double d1 = 0.0;
                        if (p_42894_.dimensionType().hasCeiling()) {
                           int i3 = j2 + k2 * 231871;
                           i3 = i3 * i3 * 31287121 + i3 * 11;
                           if ((i3 >> 20 & 1) == 0) {
                              multiset.add(Blocks.DIRT.defaultBlockState().getMapColor(p_42894_, BlockPos.ZERO), 10);
                           } else {
                              multiset.add(Blocks.STONE.defaultBlockState().getMapColor(p_42894_, BlockPos.ZERO), 100);
                           }

                           d1 = 100.0;
                        } else {
                           for (int i4 = 0; i4 < i; i4++) {
                              for (int j3 = 0; j3 < i; j3++) {
                                 blockpos$mutableblockpos.set(j2 + i4, 0, k2 + j3);
                                 int k3 = levelchunk.getHeight(Heightmap.Types.WORLD_SURFACE, blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ())
                                    + 1;
                                 BlockState blockstate;
                                 if (k3 <= p_42894_.getMinY()) {
                                    blockstate = Blocks.BEDROCK.defaultBlockState();
                                 } else {
                                    do {
                                       blockpos$mutableblockpos.setY(--k3);
                                       blockstate = levelchunk.getBlockState(blockpos$mutableblockpos);
                                    } while (blockstate.getMapColor(p_42894_, blockpos$mutableblockpos) == MapColor.NONE && k3 > p_42894_.getMinY());

                                    if (k3 > p_42894_.getMinY() && !blockstate.getFluidState().isEmpty()) {
                                       int l3 = k3 - 1;
                                       blockpos$mutableblockpos1.set(blockpos$mutableblockpos);

                                       BlockState blockstate1;
                                       do {
                                          blockpos$mutableblockpos1.setY(l3--);
                                          blockstate1 = levelchunk.getBlockState(blockpos$mutableblockpos1);
                                          l2++;
                                       } while (l3 > p_42894_.getMinY() && !blockstate1.getFluidState().isEmpty());

                                       blockstate = this.getCorrectStateForFluidBlock(p_42894_, blockstate, blockpos$mutableblockpos);
                                    }
                                 }

                                 p_42896_.checkBanners(p_42894_, blockpos$mutableblockpos.getX(), blockpos$mutableblockpos.getZ());
                                 d1 += (double)k3 / (i * i);
                                 multiset.add(blockstate.getMapColor(p_42894_, blockpos$mutableblockpos));
                              }
                           }
                        }

                        l2 /= i * i;
                        MapColor mapcolor = (MapColor)Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.NONE);
                        MapColor.Brightness mapcolor$brightness;
                        if (mapcolor == MapColor.WATER) {
                           double d2 = l2 * 0.1 + (k1 + l1 & 1) * 0.2;
                           if (d2 < 0.5) {
                              mapcolor$brightness = MapColor.Brightness.HIGH;
                           } else if (d2 > 0.9) {
                              mapcolor$brightness = MapColor.Brightness.LOW;
                           } else {
                              mapcolor$brightness = MapColor.Brightness.NORMAL;
                           }
                        } else {
                           double d3 = (d1 - d0) * 4.0 / (i + 4) + ((k1 + l1 & 1) - 0.5) * 0.4;
                           if (d3 > 0.6) {
                              mapcolor$brightness = MapColor.Brightness.HIGH;
                           } else if (d3 < -0.6) {
                              mapcolor$brightness = MapColor.Brightness.LOW;
                           } else {
                              mapcolor$brightness = MapColor.Brightness.NORMAL;
                           }
                        }

                        d0 = d1;
                        if (l1 >= 0 && i2 < j1 * j1 && (!flag1 || (k1 + l1 & 1) != 0)) {
                           flag |= p_42896_.updateColor(k1, l1, mapcolor.getPackedId(mapcolor$brightness));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private BlockState getCorrectStateForFluidBlock(Level p_42901_, BlockState p_42902_, BlockPos p_42903_) {
      FluidState fluidstate = p_42902_.getFluidState();
      return !fluidstate.isEmpty() && !p_42902_.isFaceSturdy(p_42901_, p_42903_, Direction.UP) ? fluidstate.createLegacyBlock() : p_42902_;
   }

   private static boolean isBiomeWatery(boolean[] p_212252_, int p_212253_, int p_212254_) {
      return p_212252_[p_212254_ * 128 + p_212253_];
   }

   public static void renderBiomePreviewMap(ServerLevel p_42851_, ItemStack p_42852_) {
      MapItemSavedData mapitemsaveddata = getSavedData(p_42852_, p_42851_);
      if (mapitemsaveddata != null && p_42851_.dimension() == mapitemsaveddata.dimension) {
         int i = 1 << mapitemsaveddata.scale;
         int j = mapitemsaveddata.centerX;
         int k = mapitemsaveddata.centerZ;
         boolean[] aboolean = new boolean[16384];
         int l = j / i - 64;
         int i1 = k / i - 64;
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for (int j1 = 0; j1 < 128; j1++) {
            for (int k1 = 0; k1 < 128; k1++) {
               Holder<Biome> holder = p_42851_.getBiome(blockpos$mutableblockpos.set((l + k1) * i, 0, (i1 + j1) * i));
               aboolean[j1 * 128 + k1] = holder.is(BiomeTags.WATER_ON_MAP_OUTLINES);
            }
         }

         for (int j2 = 1; j2 < 127; j2++) {
            for (int k2 = 1; k2 < 127; k2++) {
               int l2 = 0;

               for (int l1 = -1; l1 < 2; l1++) {
                  for (int i2 = -1; i2 < 2; i2++) {
                     if ((l1 != 0 || i2 != 0) && isBiomeWatery(aboolean, j2 + l1, k2 + i2)) {
                        l2++;
                     }
                  }
               }

               MapColor.Brightness mapcolor$brightness = MapColor.Brightness.LOWEST;
               MapColor mapcolor = MapColor.NONE;
               if (isBiomeWatery(aboolean, j2, k2)) {
                  mapcolor = MapColor.COLOR_ORANGE;
                  if (l2 > 7 && k2 % 2 == 0) {
                     switch ((j2 + (int)(Mth.sin(k2 + 0.0F) * 7.0F)) / 8 % 5) {
                        case 0:
                        case 4:
                           mapcolor$brightness = MapColor.Brightness.LOW;
                           break;
                        case 1:
                        case 3:
                           mapcolor$brightness = MapColor.Brightness.NORMAL;
                           break;
                        case 2:
                           mapcolor$brightness = MapColor.Brightness.HIGH;
                     }
                  } else if (l2 > 7) {
                     mapcolor = MapColor.NONE;
                  } else if (l2 > 5) {
                     mapcolor$brightness = MapColor.Brightness.NORMAL;
                  } else if (l2 > 3) {
                     mapcolor$brightness = MapColor.Brightness.LOW;
                  } else if (l2 > 1) {
                     mapcolor$brightness = MapColor.Brightness.LOW;
                  }
               } else if (l2 > 0) {
                  mapcolor = MapColor.COLOR_BROWN;
                  if (l2 > 3) {
                     mapcolor$brightness = MapColor.Brightness.NORMAL;
                  } else {
                     mapcolor$brightness = MapColor.Brightness.LOWEST;
                  }
               }

               if (mapcolor != MapColor.NONE) {
                  mapitemsaveddata.setColor(j2, k2, mapcolor.getPackedId(mapcolor$brightness));
               }
            }
         }
      }
   }

   @Override
   public void inventoryTick(ItemStack p_42870_, ServerLevel p_395390_, Entity p_42872_, @Nullable EquipmentSlot p_393433_) {
      MapItemSavedData mapitemsaveddata = getSavedData(p_42870_, p_395390_);
      if (mapitemsaveddata != null) {
         if (p_42872_ instanceof Player player) {
            mapitemsaveddata.tickCarriedBy(player, p_42870_);
         }

         if (!mapitemsaveddata.locked && p_393433_ != null && p_393433_.getType() == EquipmentSlot.Type.HAND) {
            this.update(p_395390_, p_42872_, mapitemsaveddata);
         }
      }
   }

   @Override
   public void onCraftedPostProcess(ItemStack p_42913_, Level p_42914_) {
      MapPostProcessing mappostprocessing = p_42913_.remove(DataComponents.MAP_POST_PROCESSING);
      if (mappostprocessing != null && p_42914_ instanceof ServerLevel serverlevel) {
         switch (mappostprocessing) {
            case LOCK:
               lockMap(p_42913_, serverlevel);
               break;
            case SCALE:
               scaleMap(p_42913_, serverlevel);
         }
      }
   }

   private static void scaleMap(ItemStack p_42857_, ServerLevel p_398209_) {
      MapItemSavedData mapitemsaveddata = getSavedData(p_42857_, p_398209_);
      if (mapitemsaveddata != null) {
         MapId mapid = p_398209_.getFreeMapId();
         p_398209_.setMapData(mapid, mapitemsaveddata.scaled());
         p_42857_.set(DataComponents.MAP_ID, mapid);
      }
   }

   private static void lockMap(ItemStack p_42899_, ServerLevel p_398218_) {
      MapItemSavedData mapitemsaveddata = getSavedData(p_42899_, p_398218_);
      if (mapitemsaveddata != null) {
         MapId mapid = p_398218_.getFreeMapId();
         MapItemSavedData mapitemsaveddata1 = mapitemsaveddata.locked();
         p_398218_.setMapData(mapid, mapitemsaveddata1);
         p_42899_.set(DataComponents.MAP_ID, mapid);
      }
   }

   @Override
   public InteractionResult useOn(UseOnContext p_42885_) {
      BlockState blockstate = p_42885_.getLevel().getBlockState(p_42885_.getClickedPos());
      if (blockstate.is(BlockTags.BANNERS)) {
         if (!p_42885_.getLevel().isClientSide()) {
            MapItemSavedData mapitemsaveddata = getSavedData(p_42885_.getItemInHand(), p_42885_.getLevel());
            if (mapitemsaveddata != null && !mapitemsaveddata.toggleBanner(p_42885_.getLevel(), p_42885_.getClickedPos())) {
               return InteractionResult.FAIL;
            }
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.useOn(p_42885_);
      }
   }
}
