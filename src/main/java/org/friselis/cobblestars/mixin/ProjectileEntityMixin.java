package org.friselis.cobblestars.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({ProjectileEntity.class})
public abstract class ProjectileEntityMixin {
   @Shadow
   @Nullable
   public abstract Entity getOwner();
}
