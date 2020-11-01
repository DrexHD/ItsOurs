package me.drex.itsours.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.itsours.user.ClaimPlayer;
import me.drex.itsours.util.Color;
import net.kyori.adventure.text.Component;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class DebugCommand extends Command {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> command) {
        LiteralArgumentBuilder<ServerCommandSource> debug = LiteralArgumentBuilder.literal("debug");
        debug.executes(ctx -> toggleDebug(ctx.getSource()));
        command.then(debug);
    }

    public static int toggleDebug(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayer();
        ClaimPlayer claimPlayer = (ClaimPlayer) player;
        boolean newVal = !(boolean)claimPlayer.getSetting("debug", false);
        claimPlayer.setSetting("debug", newVal);
        claimPlayer.sendMessage(Component.text("Claim debug " + (newVal ? "enabled" : "disabled")).color((newVal ? Color.LIGHT_GREEN : Color.RED)));
        return 1;
    }

}
