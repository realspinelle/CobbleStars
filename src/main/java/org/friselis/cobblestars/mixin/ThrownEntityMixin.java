package org.friselis.cobblestars.mixin;

import net.minecraft.entity.projectile.thrown.ThrownEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ThrownEntity.class})
public abstract class ThrownEntityMixin extends ProjectileEntityMixin {
}
