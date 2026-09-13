package net.minecraft.world.item.enchantment.effects;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public record RunFunction(Identifier function) implements EnchantmentEntityEffect {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<RunFunction> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Identifier.CODEC.fieldOf("function").forGetter(RunFunction::function)).apply(i, RunFunction::new)
   );

   @Override
   public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
      MinecraftServer server = serverLevel.getServer();
      ServerFunctionManager functions = server.getFunctions();
      Optional<CommandFunction<CommandSourceStack>> function = functions.get(this.function);
      if (function.isPresent()) {
         CommandSourceStack source = server.createCommandSourceStack()
            .withPermission(LevelBasedPermissionSet.GAMEMASTER)
            .withSuppressedOutput()
            .withEntity(entity)
            .withLevel(serverLevel)
            .withPosition(position)
            .withRotation(entity.getRotationVector());
         functions.execute(function.get(), source);
      } else {
         LOGGER.error("Enchantment run_function effect failed for non-existent function {}", this.function);
      }
   }

   @Override
   public MapCodec<RunFunction> codec() {
      return CODEC;
   }
}
