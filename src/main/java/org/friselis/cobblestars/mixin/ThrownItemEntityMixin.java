package org.friselis.cobblestars.mixin;

import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({ThrownItemEntity.class})
public abstract class ThrownItemEntityMixin extends ThrownEntityMixin {
   @Shadow
   protected abstract ItemStack getItem();
}
