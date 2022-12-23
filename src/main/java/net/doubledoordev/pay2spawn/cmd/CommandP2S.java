/*
 * Copyright (c) 2014, DoubleDoorDevelopment
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met
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

import com.google.common.base.Throwables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.checkers.CheckerHandler;
import net.doubledoordev.pay2spawn.checkers.TwitchChecker;
import net.doubledoordev.pay2spawn.configurator.ConfiguratorManager;
import net.doubledoordev.pay2spawn.configurator.HTMLGenerator;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.pay2spawn.util.Statistics;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Useful command when dealing with setting up the JSON file
 * Can get an entities/items JSONified NBT
 * Can reload the JSON file
 *
 * @author Dries007
 */
public class CommandP2S {
    static final String HELP = "Use command to control P2S Client side.";
    static Timer timer;

//       if (args.length == 0) {
//        Helper.msg(ChatFormatting.AQUA + HELP);
//        Helper.msg(ChatFormatting.AQUA + "Protip Use tab completion!");
//        return;
//    }

    public void processCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("pay2spawn");
        builder.then(Commands.literal("help")).executes(context -> {
            context.getSource().sendSuccess(new TranslatableComponent("p2s.command.p2s.help"), false);
            return 1;
        });

        //Reload Command
        builder.then(Commands.literal("reload").executes(context -> {
            if (Pay2Spawn.getRewardsDB().editable) {
                Pay2Spawn.reloadDB();
                Helper.msg(ChatFormatting.GREEN + "Reload done!");
            } else {
                Helper.msg(ChatFormatting.RED + "[P2S] If you are OP, use the server side command for this.");
            }
            return 1;
        }));

        //Configure Command
        builder.then(Commands.literal("configure").executes(context -> {

            if (Pay2Spawn.getRewardsDB().editable) ConfiguratorManager.openCfg();
            else Helper.msg(ChatFormatting.RED + "[P2S] You can't do that with a server side config.");
            return 1;
        }));

        //Get NBT Command
        builder.then(Commands.literal("getnbt").executes(context -> {
            ConfiguratorManager.openNbt();
            return 1;
        }));

        //MakeHTML Command
        builder.then(Commands.literal("makehtml").executes(context -> {
            try {
                HTMLGenerator.generate();
            } catch (IOException e) {
                Throwables.propagate(e);
            }
            return 1;
        }));

        //Off Command
        builder.then(Commands.literal("off").executes(context -> {
            if (Pay2Spawn.forceOn) Helper.msg(ChatFormatting.RED + "Forced on by server.");
            else {
                Pay2Spawn.enable = false;
                Helper.msg(ChatFormatting.GOLD + "[P2S] Disabled on the client.");
            }
            return 1;
        }));

        //On Command
        builder.then(Commands.literal("on").executes(context -> {
            if (Pay2Spawn.forceOn) Helper.msg(ChatFormatting.RED + "Forced on by server.");
            else {
                Pay2Spawn.enable = true;
                Helper.msg(ChatFormatting.GOLD + "[P2S] Enabled on the client.");
            }
            return 1;
        }));

        //Donate command
        builder.then(Commands.literal("donate").executes(context -> {
            Helper.msg(ChatFormatting.RED + "Use '/p2s donate <amount> [name]'.");
            return 1;
        }).then(Commands.argument("amount", DoubleArgumentType.doubleArg()).executes(context -> {
            String name = "Anonymous";
            return 1;
        }).then(Commands.argument("name", StringArgumentType.string()).executes(context -> {
            String name = context.getArgument("name", String.class);
            double amount = context.getArgument("amount", Double.class);
            CheckerHandler.fakeDonation(amount, name);
            return 1;
        }))));

        //Adjust Command
        builder.then(Commands.literal("adjusttotal").executes(context -> {
            Helper.msg(ChatFormatting.RED + "Use '/p2s adjusttotal <amount>'. You can use + and -");
            return 1;
        }).then(Commands.argument("amount", DoubleArgumentType.doubleArg()).executes(context -> {
            double amount = context.getArgument("amount", Double.class);
            Statistics.addToDonationAmount(amount);
            return 1;
        })));

        //Reset Subs Command
        builder.then(Commands.literal("resetsubs").executes(context -> {
            TwitchChecker.INSTANCE.reset();
            Helper.msg(ChatFormatting.GOLD + "[P2S] Subs have been resetted!");
            return 1;
        }));

        //Test Command
        builder.then(Commands.literal("test").executes(context -> {
            Helper.msg(ChatFormatting.RED + "Use '/p2s test <amount> <repeat delay in sec> [name]' use '/p2s test end' to stop the testing.");
            return 1;
        }).then(Commands.literal("end").executes(context -> {
            if (timer != null) {
                timer.cancel();
            }
            return 1;
        }).then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                .then(Commands.argument("delay", IntegerArgumentType.integer())
                        .then(Commands.argument("name", StringArgumentType.string())
                                .executes(context -> {
                                    if (timer != null) {
                                        timer.cancel();
                                    }
                                    double amount = context.getArgument("amount", Double.class);
                                    int delay = context.getArgument("delay", Integer.class);
                                    String name = context.getArgument("name", String.class);
                                    timer = new Timer();
                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            CheckerHandler.fakeDonation(amount, name);
                                        }
                                    }, 0, delay);
                                    return 1;
                                }))))));

    /*            else if (args.length > 2) {
            final String name;
            final Double amount = CommandBase.parseDouble(dispatcher, args[1]);
            final Integer delay = CommandBase.parseInt(dispatcher, args[2]) * 1000;
            if (args.length > 3) name = args[3];
            else name = "Anonymous";
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {

                public void run() {
                    CheckerHandler.fakeDonation(amount, name);
                }
            }, 0, delay);
        } else

            return 1;
    }));*/
//                Helper.msg(ChatFormatting.RED + "Unknown command. Protip Use tab completion!");

    }

 /*   public List addTabCompletionOptions(CommandSourceStack sender, String[] args) {
        if (args.length == 1)
            return getListOfStringsMatchingLastWord(args, "reload", "configure", "getnbt", "makehtml", "off", "on", "donate", "permissions", "adjusttotal", "test");
        return null;
    }*/
}
