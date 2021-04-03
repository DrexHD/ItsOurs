package me.drex.itsours.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.drex.itsours.ItsOursMod;
import me.drex.itsours.claim.permission.roles.Role;
import me.drex.itsours.claim.permission.util.Permission;
import me.drex.itsours.user.ClaimPlayer;
import me.drex.itsours.util.Color;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import java.util.Map;


public class RolesCommand extends Command {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> literal) {
        LiteralArgumentBuilder<ServerCommandSource> command = LiteralArgumentBuilder.literal("roles");
        command.executes(ctx -> listRoles(ctx.getSource()));
        {
            RequiredArgumentBuilder<ServerCommandSource, String> name = RequiredArgumentBuilder.argument("name", StringArgumentType.word());
            name.executes(ctx -> addRole(ctx.getSource(), StringArgumentType.getString(ctx, "name")));
            LiteralArgumentBuilder<ServerCommandSource> add = LiteralArgumentBuilder.literal("add");
            add.requires(src -> hasPermission(src, "itsours.roles.add"));
            add.then(name);
            command.then(add);
        }
        {
            RequiredArgumentBuilder<ServerCommandSource, String> name = roleArgument();
            name.executes(ctx -> removeRole(ctx.getSource(), StringArgumentType.getString(ctx, "name")));
            LiteralArgumentBuilder<ServerCommandSource> remove = LiteralArgumentBuilder.literal("remove");
            remove.requires(src -> hasPermission(src, "itsours.roles.remove"));
            remove.then(name);
            command.then(remove);
        }
        {
            RequiredArgumentBuilder<ServerCommandSource, String> value = permissionValueArgument();
            value.executes(ctx -> setPermission(ctx.getSource(), StringArgumentType.getString(ctx, "name"), getPermission(ctx), getPermissionValue(ctx)));
            RequiredArgumentBuilder<ServerCommandSource, String> perm = permissionArgument();
            RequiredArgumentBuilder<ServerCommandSource, String> name = roleArgument();
            name.executes(ctx -> listPermission(ctx.getSource(), StringArgumentType.getString(ctx, "name")));
            LiteralArgumentBuilder<ServerCommandSource> permission = LiteralArgumentBuilder.literal("permission");
            permission.requires(src -> hasPermission(src, "itsours.roles.permission"));
            perm.then(value);
            name.then(perm);
            permission.then(name);
            command.then(permission);
        }

        literal.then(command);
    }

    public static int addRole(ServerCommandSource source, String name) throws CommandSyntaxException {
        if (ItsOursMod.INSTANCE.getRoleManager().containsKey(name))
            throw new SimpleCommandExceptionType(new LiteralText("A role with that name already exists")).create();
        ItsOursMod.INSTANCE.getRoleManager().put(name, new Role(new NbtCompound()));
        ((ClaimPlayer) source.getPlayer()).sendMessage(Component.text("Role ").color(Color.YELLOW)
                .append(Component.text(name).color(Color.ORANGE).append(Component.text(" has been added").color(Color.YELLOW))));

        return 1;
    }

    public static int removeRole(ServerCommandSource source, String name) throws CommandSyntaxException {
        if (!ItsOursMod.INSTANCE.getRoleManager().containsKey(name))
            throw new SimpleCommandExceptionType(new LiteralText("There is no role with that name")).create();
        if (name.equals("default") || name.equals("trusted"))
            throw new SimpleCommandExceptionType(new LiteralText("You can't remove that role")).create();
        ItsOursMod.INSTANCE.getRoleManager().remove(name);
        ((ClaimPlayer) source.getPlayer()).sendMessage(Component.text("Role ").color(Color.YELLOW).append(Component.text(name).color(Color.ORANGE).append(Component.text(" has been removed").color(Color.YELLOW))));
        return 1;
    }

    public static int setPermission(ServerCommandSource source, String name, String permission, Permission.Value value) throws CommandSyntaxException {
        if (!ItsOursMod.INSTANCE.getRoleManager().containsKey(name))
            throw new SimpleCommandExceptionType(new LiteralText("There is no role with that name")).create();
        Role role = ItsOursMod.INSTANCE.getRoleManager().get(name);
        role.permissions().setPermission(permission, value);
        ((ClaimPlayer) source.getPlayer()).sendMessage(Component.text("Set ").color(Color.YELLOW)
                .append(Component.text(permission).color(Color.ORANGE)).append(Component.text(" for ").color(Color.YELLOW))
                .append(Component.text(name).color(Color.ORANGE)
                        .append(Component.text(" to ").color(Color.YELLOW)).append(value.format())));
        return 1;
    }

    public static int listPermission(ServerCommandSource source, String name) throws CommandSyntaxException {
        if (!ItsOursMod.INSTANCE.getRoleManager().containsKey(name))
            throw new SimpleCommandExceptionType(new LiteralText("There is no role with that name")).create();
        Role role = ItsOursMod.INSTANCE.getRoleManager().get(name);
        ((ClaimPlayer) source.getPlayer()).sendMessage(Component.text(name).color(Color.YELLOW).append(Component.text("\n")).append(role.permissions().toText()));
        return 1;
    }

    public static int listRoles(ServerCommandSource source) throws CommandSyntaxException {
        TextComponent.Builder builder = Component.text().content("Roles:\n").color(Color.ORANGE);
        for (Map.Entry<String, Role> entry : ItsOursMod.INSTANCE.getRoleManager().entrySet()) {
            String name = entry.getKey();
            Role role = entry.getValue();
            builder.append(Component.text(name).color(Color.YELLOW).style(style -> style.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(role.permissions().toText())))).append(Component.text(" "));
        }
        ((ClaimPlayer) source.getPlayer()).sendMessage(builder.build());
        return 1;
    }


}
