package org.friselis.cobblestars.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.friselis.cobblestars.CobbleStars;
import org.friselis.cobblestars.SafariAuction.SafariAuctionScreenHandler;

public class Auction {
   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register(CommandManager.literal("auction").executes((context) -> {
         ServerCommandSource source = context.getSource();
         Entity entity = source.getEntity();
         if (entity instanceof ServerPlayerEntity player) {
             try {
               SafariAuctionScreenHandler.open(player);
            } catch (Exception var4) {
               CobbleStars.LOGGER.error("Error opening auction", var4);
            }
         } else {
            source.sendFeedback(() -> Text.literal("This command can only be run by a player."), false);
         }

         return 1;
      }));
   }
}
