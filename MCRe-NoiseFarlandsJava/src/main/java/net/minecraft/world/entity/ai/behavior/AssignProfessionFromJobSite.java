package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

public class AssignProfessionFromJobSite {
    public static BehaviorControl<Villager> create() {
        return BehaviorBuilder.create(
            i -> i.group(i.present(MemoryModuleType.POTENTIAL_JOB_SITE), i.registered(MemoryModuleType.JOB_SITE))
                .apply(
                    i,
                    (potentialJobSite, jobSite) -> (level, body, timestamp) -> {
                        GlobalPos pos = i.get(potentialJobSite);
                        if (!pos.pos().closerToCenterThan(body.position(), 2.0) && !body.assignProfessionWhenSpawned()) {
                            return false;
                        }

                        potentialJobSite.erase();
                        jobSite.set(pos);
                        level.broadcastEntityEvent(body, (byte)14);
                        if (!body.getVillagerData().profession().is(VillagerProfession.NONE)) {
                            return true;
                        }

                        MinecraftServer server = level.getServer();
                        Optional.ofNullable(server.getLevel(pos.dimension()))
                            .flatMap(l -> l.getPoiManager().getType(pos.pos()))
                            .flatMap(
                                poiType -> BuiltInRegistries.VILLAGER_PROFESSION
                                    .listElements()
                                    .filter(profession -> profession.value().heldJobSite().test((Holder<PoiType>)poiType))
                                    .findFirst()
                            )
                            .ifPresent(profession -> {
                                body.setVillagerData(body.getVillagerData().withProfession(profession));
                                body.refreshBrain(level);
                            });
                        return true;
                    }
                )
        );
    }
}