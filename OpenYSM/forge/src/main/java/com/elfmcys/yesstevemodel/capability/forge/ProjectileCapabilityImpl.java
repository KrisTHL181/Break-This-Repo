package com.elfmcys.yesstevemodel.capability.forge;

import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.elfmcys.yesstevemodel.forge.capability.ProjectileCapabilityProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Optional;

public final class ProjectileCapabilityImpl {

    private ProjectileCapabilityImpl() {
    }

    public static Optional<ProjectileCapability> get(Entity entity) {
        return entity.getCapability(ProjectileCapabilityProvider.PROJECTILE_CAP).resolve();
    }

    public static Optional<ProjectileCapability> get(Projectile projectile) {
        return projectile.getCapability(ProjectileCapabilityProvider.PROJECTILE_CAP).resolve();
    }
}
