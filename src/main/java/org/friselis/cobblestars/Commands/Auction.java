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
      dispatcher.register((LiteralArgumentBuilder)CommandManager.literal("auction").executes((context) -> {
         ServerCommandSource source = (ServerCommandSource)context.getSource();
         Entity patt807$temp = source.getEntity();
         if (patt807$temp instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity)patt807$temp;

            try {
               SafariAuctionScreenHandler.open(player);
            } catch (Exception var4) {
               CobbleStars.LOGGER.error((String)"Error opening auction", (Throwable)var4);
            }
         } else {
            source.sendFeedback(() -> {
               return Text.literal("This command can only be run by a player.");
            }, false);
         }

         return 1;
      }));
   }
}
