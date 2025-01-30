package org.friselis.cobblestars.Commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.friselis.cobblestars.CobbleStars;

public class Glove {
   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("glove").requires((source) -> {
         return Permissions.check(source, "discord.bridge.command.end_now", 4);
      })).executes(Glove::execute));
   }

   public static int execute(CommandContext<ServerCommandSource> context) {
      CobbleStars.instance.config.gloveActive = !CobbleStars.instance.config.gloveActive;
      CobbleStars.instance.SaveConfig();
      return 1;
   }
}
