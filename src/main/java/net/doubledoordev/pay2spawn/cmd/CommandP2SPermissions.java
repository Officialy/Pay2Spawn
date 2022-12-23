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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.doubledoordev.pay2spawn.permissions.Group;
import net.doubledoordev.pay2spawn.permissions.Node;
import net.doubledoordev.pay2spawn.permissions.PermissionsHandler;
import net.doubledoordev.pay2spawn.permissions.Player;
import net.doubledoordev.pay2spawn.util.Helper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/**
 * The main permission system command
 *
 * @author Dries007
 */
public class CommandP2SPermissions {

    public static void processCommand(CommandDispatcher<CommandSourceStack> sender) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("p2sperm").executes(context -> {
            Helper.sendChatToPlayer(context.getSource(), "Use '/p2sperm group|groups|player' for more info.", ChatFormatting.RED);
            return 0;
        });

        builder.then(Commands.literal("groups").executes(context -> {
                    Helper.sendChatToPlayer(context.getSource(), "Use 'p2sperm groups add|remove <name> [parent group]' to add or remove a group.", ChatFormatting.RED);
                    return 1;
                }).then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("parent", StringArgumentType.string())
                                        .executes(context -> {
                                            String name = context.getArgument("name", String.class);
                                            String parent = context.getArgument("parent", String.class);
                                            PermissionsHandler.getDB().newGroup(name, parent);
                                            Helper.sendChatToPlayer(context.getSource(), "Added new group named '" + name + (parent != null ? "' with parent group '" + parent : "") + "'.", ChatFormatting.GOLD);
                                            PermissionsHandler.getDB().save();
                                            return 1;
                                        }))))
                .then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> {
                            String name = context.getArgument("name", String.class);
                            PermissionsHandler.getDB().remove(name);
                            Helper.sendChatToPlayer(context.getSource(), "Removed group named '" + name + "'", ChatFormatting.GOLD);
                            PermissionsHandler.getDB().save();
                            return 1;
                        }))));

        builder.then(Commands.literal("group").executes(context -> {
            Helper.sendChatToPlayer(context.getSource(), "Use 'p2sperm group <name> add|remove <node>' OR '<name> parent set|clear [name]'", ChatFormatting.RED);
            return 1;
        }).then(Commands.argument("name", StringArgumentType.string())
                .then(Commands.literal("add")
                        .then(Commands.argument("node", StringArgumentType.string())
                                .executes(context -> {
                                    String name = context.getArgument("name", String.class);
                                    String node = context.getArgument("node", String.class);
                                    Group group = PermissionsHandler.getDB().getGroup(name);
                                    if (group == null) {
                                        Helper.sendChatToPlayer(context.getSource(), "Group '" + name + "' does not exist.", ChatFormatting.RED);
                                        return 1;
                                    }
                                    group.addNode(node);
                                    Helper.sendChatToPlayer(context.getSource(), "Added node '" + node + "' to group '" + name + "'.", ChatFormatting.GOLD);
                                    PermissionsHandler.getDB().save();
                                    return 1;
                                }))) //end of add
                .then(Commands.literal("remove").then(Commands.argument("node", StringArgumentType.string())
                        .executes(context -> {
                            String name = context.getArgument("name", String.class);
                            String node = context.getArgument("node", String.class);
                            Group group = PermissionsHandler.getDB().getGroup(name);
                            if (group == null) {
                                Helper.sendChatToPlayer(context.getSource(), "Group '" + name + "' does not exist.", ChatFormatting.RED);
                                return 1;
                            }
                            group.removeNode(node);
                            Helper.sendChatToPlayer(context.getSource(), "Removed node '" + node + "' from group '" + name + "'.", ChatFormatting.GOLD);
                            PermissionsHandler.getDB().save();
                            return 1;
                        }))) //end of remove
                .then(Commands.literal("parent")
                        .then(Commands.literal("set").executes(context -> {
                            String name = context.getArgument("name", String.class);
                            Group group = PermissionsHandler.getDB().getGroup(name);
                            group.setParent(name);
                            Helper.sendChatToPlayer(context.getSource(), "Set parent to: " + name, ChatFormatting.GOLD);
                            PermissionsHandler.getDB().save();
                            return 1;
                        }))).then(Commands.literal("clear").executes(context -> {
                    String name = context.getArgument("name", String.class);
                    Group group = PermissionsHandler.getDB().getGroup(name);
                    group.setParent(null);
                    Helper.sendChatToPlayer(context.getSource(), "Cleared parent group.", ChatFormatting.GOLD);
                    PermissionsHandler.getDB().save();
                    return 1;
                })).then(Commands.literal("player").executes(context -> {
                    Helper.sendChatToPlayer(context.getSource(), "Use 'p2sperm player <name> group add|remove <group>' OR '<name> perm add|remove <node>'", ChatFormatting.RED);
                    return 1;
                })).then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.literal("group")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("group", StringArgumentType.string())
                                                .executes(context -> {
                                                    String name = context.getArgument("name", String.class);
                                                    String group = context.getArgument("group", String.class);
                                                    Player player = PermissionsHandler.getDB().getPlayer(name);
                                                    if (player == null) {
                                                        Helper.sendChatToPlayer(context.getSource(), "Player '" + name + "' does not exist.", ChatFormatting.RED);
                                                        return 1;
                                                    }
                                                    player.addGroup(group);
                                                    Helper.sendChatToPlayer(context.getSource(), "Added group '" + group + "' to player '" + name + "'.", ChatFormatting.GOLD);
                                                    PermissionsHandler.getDB().save();
                                                    return 1;
                                                }))) //end of add
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("group", StringArgumentType.string())
                                                .executes(context -> {
                                                    String name = context.getArgument("name", String.class);
                                                    String group = context.getArgument("group", String.class);
                                                    Player player = PermissionsHandler.getDB().getPlayer(name);
                                                    if (player == null) {
                                                        Helper.sendChatToPlayer(context.getSource(), "Player '" + name + "' does not exist.", ChatFormatting.RED);
                                                        return 1;
                                                    }
                                                    player.removeGroup(group);
                                                    Helper.sendChatToPlayer(context.getSource(), "Removed group '" + group + "' from player '" + name + "'.", ChatFormatting.GOLD);
                                                    PermissionsHandler.getDB().save();
                                                    return 1;
                                                }))) //end of remove
                                .then(Commands.literal("perm")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("node", StringArgumentType.string())
                                                        .executes(context -> {
                                                            String name = context.getArgument("name", String.class);
                                                            String node = context.getArgument("node", String.class);
                                                            Player player = PermissionsHandler.getDB().getPlayer(name);
                                                            if (player == null) {
                                                                Helper.sendChatToPlayer(context.getSource(), "Player '" + name + "' does not exist.", ChatFormatting.RED);
                                                                return 1;
                                                            }
                                                            player.addNode(new Node(node));
                                                            Helper.sendChatToPlayer(context.getSource(), "Added node '" + node + "' to player '" + name + "'.", ChatFormatting.GOLD);
                                                            PermissionsHandler.getDB().save();
                                                            return 1;
                                                        }))) //end of add
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("node", StringArgumentType.string())
                                                        .executes(context -> {
                                                            String name = context.getArgument("name", String.class);
                                                            String node = context.getArgument("node", String.class);
                                                            Player player = PermissionsHandler.getDB().getPlayer(name);
                                                            if (player == null) {
                                                                Helper.sendChatToPlayer(context.getSource(), "Player '" + name + "' does not exist.", ChatFormatting.RED);
                                                                return 1;
                                                            }
                                                            player.removeNode(new Node(node));
                                                            Helper.sendChatToPlayer(context.getSource(), "Removed node '" + node + "' from player '" + name + "'.", ChatFormatting.GOLD);
                                                            PermissionsHandler.getDB().save();
                                                            return 1;
                                                        })))))))); //end of remove
    }
        /*case "player" -> {
            if (args.length < 5) {
            } else {
                Player playero = PermissionsHandler.getDB().getPlayer(args[1]);
                if (playero == null) {
                    Helper.sendChatToPlayer(context.getSource(), "That player doesn't exist.", ChatFormatting.RED);
                    break;
                }
                switch (args[2]) {
                    case "group" -> {
                        switch (args[3]) {
                            case "add" -> {
                                playero.addGroup(args[4]);
                                Helper.sendChatToPlayer(context.getSource(), "Added " + args[1] + " to " + args[4], ChatFormatting.GOLD);
                            }
                            case "remove" -> {
                                if (playero.removeGroup(args[4]))
                                    Helper.sendChatToPlayer(context.getSource(), "Removed group: " + args[4], ChatFormatting.GOLD);
                                else
                                    Helper.sendChatToPlayer(context.getSource(), "Group not removed, it wasn't there in the first place...", ChatFormatting.RED);
                            }
                        }
                    }
                    case "perm" -> {
                        switch (args[3]) {
                            case "add" -> playero.addNode(new Node(args[4]));
                            case "remove" -> {
                                if (playero.removeNode(new Node(args[4])))
                                    Helper.sendChatToPlayer(context.getSource(), "Added per node: " + args[4], ChatFormatting.GOLD);
                                else
                                    Helper.sendChatToPlayer(context.getSource(), "Perm node not removed, it wasn't there in the first place...", ChatFormatting.RED);
                            }
                        }
                    }
                }
            }
        }*/

//        
    

/*
    public List addTabCompletionOptions(CommandSourceStack sender, String[] args) {
        switch (args.length) {
            case 1:
                return getListOfStringsMatchingLastWord(args, "groups", "group", "player");
            case 2:
                switch (args[0]) {
                    case "groups" -> {
                        return getListOfStringsMatchingLastWord(args, "add", "remove");
                    }
                    case "group" -> {
                        return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getDB().getGroups());
                    }
                    case "player" -> {
                        return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getDB().getPlayers());
                    }
                }
                break;
            case 3:
                switch (args[0]) {
                    case "groups" -> {
                        if (args[1].equals("remove"))
                            return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getDB().getGroups());
                    }
                    case "group" -> {
                        return getListOfStringsMatchingLastWord(args, "parent", "add", "remove");
                    }
                    case "player" -> {
                        return getListOfStringsMatchingLastWord(args, "group", "perm");
                    }
                }
                break;
            case 4:
                switch (args[0]) {
                    case "groups":
                        if (args[1].equals("add"))
                            return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getDB().getGroups());
                    case "group":
                        switch (args[2]) {
                            case "parent":
                                return getListOfStringsMatchingLastWord(args, "set", "clear");
                            case "add":
                                return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getAllPermNodes());
                            case "remove":
                                return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getDB().getGroup(args[2]).getNodes());
                        }
                        break;
                    case "player":
                        switch (args[2]) {
                            case "group" -> {
                                return getListOfStringsMatchingLastWord(args, "add", "remove");
                            }
                            case "perm" -> {
                                return getListOfStringsMatchingLastWord(args, "add", "remove");
                            }
                        }
                        break;
                }
                break;
            case 5:
                switch (args[0]) {
                    case "group" -> {
                        if (args[2].equals("parent") && args[3].equals("set"))
                            return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getDB().getGroups());
                    }
                    case "player" -> {
                        switch (args[2]) {
                            case "group" -> {
                                switch (args[3]) {
                                    case "add" -> {
                                        return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getDB().getGroups());
                                    }
                                    case "remove" -> {
                                        return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getDB().getPlayer(args[1]).getGroups());
                                    }
                                }
                            }
                            case "perm" -> {
                                switch (args[3]) {
                                    case "add" -> {
                                        return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getAllPermNodes());
                                    }
                                    case "remove" -> {
                                        return getListOfStringsFromIterableMatchingLastWord(args, PermissionsHandler.getDB().getPlayer(args[1]).getNodes());
                                    }
                                }
                            }
                        }
                    }
                }
                break;
        }
        return null;
    }
*/
}
