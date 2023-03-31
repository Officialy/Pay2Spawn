/*
 * Copyright (c) 2014, DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.doubledoordev.pay2spawn.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.util.Constants;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * The server side only command
 *
 * @author Dries007
 */
public class CommandP2SServer {
    static final String HELP = "OP only command, Server side.";

    public String getCommandName() {
        return "pay2spawnserver";
    }

    public String getCommandUsage(CommandSourceStack icommandsender) {
        return HELP;
    }

    public static void register(CommandDispatcher<CommandSourceStack> sender) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("pay2spawn").requires(cs -> cs.hasPermission(2)).executes(
                context -> {
                    sendChatToPlayer(context.getSource(), HELP, ChatFormatting.AQUA);
                    sendChatToPlayer(context.getSource(), "Protip: Use tab completion!", ChatFormatting.AQUA);
                    return 1;
                }
        );

        builder.then(Commands.literal("reload").executes(context -> {
            if (context.getSource().getServer().isDedicatedServer()) {
                try {
                    Pay2Spawn.reloadDB_Server();
                } catch (Exception e) {
                    sendChatToPlayer(context.getSource(), "RELOAD FAILED.", ChatFormatting.RED);
                    e.printStackTrace();
                }
            }
            return 0;
        }));

        builder.then(Commands.literal("butcher").executes(context -> {
            sendChatToPlayer(context.getSource(), "Removing all spawned entities...", ChatFormatting.YELLOW);
            int count = 0;
            for (ServerLevel world : context.getSource().getServer().getAllLevels()) {
                for (Entity entity : world.getAllEntities()) {
                  if (entity.getEntityData().getAll().contains(Constants.NAME)) { //todo check if this works
                        count++;
                        entity.kill();
                    }
                }
            }
            sendChatToPlayer(context.getSource(), "Removed " + count + " entities.", ChatFormatting.GREEN);
            return 1;
        }));

        builder.then(Commands.literal("hasmod").executes(context -> {
            sendChatToPlayer(context.getSource(), "Use '/p2sserver hasmod <player>'.", ChatFormatting.RED);
            return 1;
        })).then(Commands.argument("player", EntityArgument.player()).executes(context -> {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            sendChatToPlayer(context.getSource(), player.getName().getString() + (Pay2Spawn.doesPlayerHaveValidConfig(player.getName().getString()) ? " does " : " doesn't"), ChatFormatting.AQUA);
            return 1;
        }));

//        sendChatToPlayer(context.getSource(), "Unknown command. Protip: Use tab completion!", ChatFormatting.RED);
        sender.register(builder);
    }
    
/*    public List addTabCompletionOptions(CommandSourceStack sender, String[] args) {
        switch (args.length) {
            case 1:
                return getListOfStringsMatchingLastWord(args, "reload", "hasmod", "butcher");
            case 2:
                switch (args[1]) {
                    case "hasmod":
                        return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
                }
        }
        return null;
    }*/

    public static void sendChatToPlayer(CommandSourceStack sender, String message, ChatFormatting chatFormatting) throws CommandSyntaxException {
        sender.getPlayerOrException().displayClientMessage(Component.literal(message).withStyle(chatFormatting), false);
    }
}
