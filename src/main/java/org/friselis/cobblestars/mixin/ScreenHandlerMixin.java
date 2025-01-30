package org.friselis.cobblestars.mixin;

import java.util.Objects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ScreenHandler.class})
public abstract class ScreenHandlerMixin {
   @Shadow
   public abstract Slot getSlot(int var1);

   @Inject(
      at = {@At("HEAD")},
      cancellable = true,
      method = {"onSlotClick"}
   )
   protected void InjectedOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
      if (slotIndex != -999) {
         if (slotIndex != -1) {
            String slotItemID = Registries.ITEM.getId(this.getSlot(slotIndex).getStack().getItem()).toString();
            if (Objects.equals(slotItemID, "academy:star_badge")) {
               ci.cancel();
            }

         }
      }
   }
}
