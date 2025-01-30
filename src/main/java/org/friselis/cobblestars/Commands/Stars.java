package org.friselis.cobblestars.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.friselis.cobblestars.StarInventory;

public class Stars {
   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)CommandManager.literal("stars").then(CommandManager.argument("player", EntityArgumentType.player()).executes(Stars::execute)));
   }

   public static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
      ServerCommandSource source = (ServerCommandSource)context.getSource();
      StarInventory stars = StarInventory.get(EntityArgumentType.getPlayer(context, "player"));
      source.sendFeedback(() -> {
         return Text.literal(String.valueOf(stars.getCount()));
      }, false);
      return 1;
   }
}
