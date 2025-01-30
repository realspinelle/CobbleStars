package org.friselis.cobblestars.mixin;

import abeshutt.staracademy.entity.DuelingGloveEntity;
import abeshutt.staracademy.init.ModConfigs;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.Iterator;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.friselis.cobblestars.CobbleStars;
import org.friselis.cobblestars.PlayerData;
import org.friselis.cobblestars.StarInventory;
import org.friselis.cobblestars.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({DuelingGloveEntity.class})
public abstract class DuelingGloveMixin extends ThrownItemEntityMixin {
   @Inject(
      at = {@At("HEAD")},
      cancellable = true,
      method = {"onEntityHit"}
   )
   protected void onEntityHitInjected(EntityHitResult result, CallbackInfo ci) {
      Entity opponent = result.getEntity();
      Entity author = this.getOwner();
      if (author == null) {
         ci.cancel();
      } else if (opponent == null) {
         ci.cancel();
      } else if (!(opponent instanceof ServerPlayerEntity)) {
         ci.cancel();
      } else {
         ServerPlayerEntity battlePartner = (ServerPlayerEntity)opponent;
         if (author instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity)author;
            if (!CobbleStars.instance.config.gloveActive) {
               author.sendMessage(Text.literal("Glove isnt active currently").formatted(Formatting.RED));
               ci.cancel();
            } else if (Utils.isPlayerBetween(player.getPos(), new Vec3d(-38.0D, 0.0D, 266.0D), new Vec3d(-296.0D, 0.0D, 80.0D))) {
               author.sendMessage(Text.literal("You cant duel in spawn").formatted(Formatting.RED));
               ci.cancel();
            } else {
               PlayerData playerData = (PlayerData)CobbleStars.instance.playersData.get(player.getUuid());
               PlayerData battlePartenerData = (PlayerData)CobbleStars.instance.playersData.get(battlePartner.getUuid());
               if (playerData.onCooldown()) {
                  author.sendMessage(Text.literal("You are on duel cooldown " + playerData.cooldownRemaining() + " Left").formatted(Formatting.RED));
                  ci.cancel();
               } else if (battlePartenerData.onCooldown()) {
                  author.sendMessage(Text.literal("Your opponent is on duel cooldown " + battlePartenerData.cooldownRemaining() + " Left").formatted(Formatting.RED));
                  ci.cancel();
               } else if (player.getItemCooldownManager().getCooldownProgress((Item)Registries.ITEM.get(new Identifier("academy:dueling_glove")), (float)ModConfigs.DUELING.getCooldownTicks()) != 0.0F) {
                  author.sendMessage(Text.literal("Glitch").formatted(Formatting.RED));
                  ci.cancel();
               } else if (opponent.getUuid().equals(author.getUuid())) {
                  author.sendMessage(Text.literal("You cant battle urself").formatted(Formatting.RED));
                  ci.cancel();
               } else {
                  StarInventory battlePartenerStars = StarInventory.get(battlePartner);
                  if (battlePartenerStars.getStarSlot() == -1) {
                     player.sendMessage(Text.literal("Your opponent has no stars.").formatted(Formatting.RED));
                     ci.cancel();
                  } else {
                     StarInventory playerStars = StarInventory.get(player);
                     if (playerStars.getAvailableSlot() == -1) {
                        player.sendMessage(Text.literal("you cannot battle when having 10 stars.").formatted(Formatting.RED));
                        ci.cancel();
                     } else if (BattleRegistry.INSTANCE.getBattle(player.getUuid()) != null) {
                        player.sendMessage(Text.literal("You can't start a new battle, while in a battle.").formatted(Formatting.RED));
                        ci.cancel();
                     } else if (BattleRegistry.INSTANCE.getBattle(battlePartner.getUuid()) != null) {
                        player.sendMessage(Text.literal("Opponent is currently in a battle.").formatted(Formatting.RED));
                        ci.cancel();
                     } else {
                        Pokemon playerLeadingPokemon = null;

                        try {
                           Iterator var12 = Cobblemon.INSTANCE.getStorage().getParty(player).iterator();

                           Pokemon pokemon;
                           while(var12.hasNext()) {
                              pokemon = (Pokemon)var12.next();
                              pokemon.heal();
                           }

                           var12 = Cobblemon.INSTANCE.getStorage().getParty(player).iterator();

                           while(var12.hasNext()) {
                              pokemon = (Pokemon)var12.next();
                              if (!pokemon.isFainted()) {
                                 playerLeadingPokemon = pokemon;
                                 break;
                              }
                           }
                        } catch (Exception var16) {
                        }

                        if (playerLeadingPokemon == null) {
                           player.sendMessage(Text.literal("No available Pokemon to battle.").formatted(Formatting.RED));
                           BattleRegistry.INSTANCE.removeChallenge(player.getUuid(), battlePartner.getUuid());
                           ci.cancel();
                        } else {
                           Pokemon partnerLeadingPokemon = null;

                           try {
                              Iterator var18 = Cobblemon.INSTANCE.getStorage().getParty(battlePartner).iterator();

                              Pokemon pokemon;
                              while(var18.hasNext()) {
                                 pokemon = (Pokemon)var18.next();
                                 pokemon.heal();
                              }

                              var18 = Cobblemon.INSTANCE.getStorage().getParty(battlePartner).iterator();

                              while(var18.hasNext()) {
                                 pokemon = (Pokemon)var18.next();
                                 if (!pokemon.isFainted()) {
                                    partnerLeadingPokemon = pokemon;
                                    break;
                                 }
                              }
                           } catch (Exception var15) {
                           }

                           if (partnerLeadingPokemon == null) {
                              battlePartner.sendMessage(Text.literal("No available Pokemon to battle.").formatted(Formatting.RED));
                              BattleRegistry.INSTANCE.removeChallenge(player.getUuid(), battlePartner.getUuid());
                              ci.cancel();
                           } else {
                              BattleBuilder.INSTANCE.pvp1v1(player, battlePartner, playerLeadingPokemon.getUuid(), partnerLeadingPokemon.getUuid());
                              CobbleStars.instance.duelLists.add(player.getUuid());
                              CobbleStars.instance.duelLists.add(battlePartner.getUuid());
                              player.getItemCooldownManager().set((Item)Registries.ITEM.get(new Identifier("academy:dueling_glove")), ModConfigs.DUELING.getCooldownTicks());
                              ci.cancel();
                           }
                        }
                     }
                  }
               }
            }
         } else {
            ci.cancel();
         }
      }
   }
}
