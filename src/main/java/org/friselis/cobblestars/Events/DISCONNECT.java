package org.friselis.cobblestars.Events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.friselis.cobblestars.WebSocket;

public class DISCONNECT {
   public static void RUN(ServerPlayNetworkHandler packet, MinecraftServer server) {
      WebSocket.sendLeavingMessage(packet.player.getDisplayName().getString());
   }
}
