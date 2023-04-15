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

package net.doubledoordev.pay2spawn.network;


import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.configurator.ConfiguratorManager;
import net.doubledoordev.pay2spawn.permissions.PermissionsHandler;
import net.doubledoordev.pay2spawn.util.Helper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Used for status things
 * - Handshake
 * - Forced config
 * - Enable or disable
 * - Status check
 *
 * @author Dries007
 */
public class StatusMessage {
    public static String serverConfig;
    private final Type type;
    private final String[] extraData;

    public StatusMessage(Type type, String... extraData) {
        this.type = type;
        this.extraData = extraData;
    }

    public static void sendHandshakeToPlayer(ServerPlayer player) {
        Pay2Spawn.getSnw().sendTo(new StatusMessage(Type.HANDSHAKE), player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
    }

    private static void sendForceToPlayer(ServerPlayer player) {
        Pay2Spawn.getSnw().sendTo(new StatusMessage(Type.FORCE), player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
    }

    private static void sendConfigToPlayer(ServerPlayer player) {
        Pay2Spawn.getSnw().sendTo(new StatusMessage(Type.CONFIGSYNC, serverConfig), player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendConfigToAllPlayers() {
        Pay2Spawn.getSnw().sendToServer(new StatusMessage(Type.CONFIGSYNC, serverConfig));
    }

    public StatusMessage(FriendlyByteBuf buf) {
        type = Type.values()[buf.readInt()];
        extraData = new String[buf.readInt()];
        for (int i = 0; i < extraData.length; i++) extraData[i] = Helper.readLongStringToByteBuf(buf);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(type.ordinal());
        buf.writeInt(extraData.length);
        for (String s : extraData) Helper.writeLongStringToByteBuf(buf, s);
    }

    public static enum Type {
        HANDSHAKE,
        CONFIGSYNC,
        FORCE,
        STATUS,
        SALE
    }

    public static void handle(StatusMessage message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            switch (message.type) {
                case HANDSHAKE:
                    new StatusMessage(Type.HANDSHAKE);
                    ctx.get().setPacketHandled(true);
                case CONFIGSYNC:
                    Pay2Spawn.reloadDBFromServer(message.extraData[0]);
                    ConfiguratorManager.exit();
                    Helper.msg(ChatFormatting.GOLD + "[P2S] Using config specified by the server.");
                    ctx.get().setPacketHandled(true);
                    break;
                case FORCE:
                    Pay2Spawn.forceOn = true;
                    ctx.get().setPacketHandled(true);
                    break;
                case STATUS:
                    new StatusMessage(Type.STATUS, message.extraData[0], Boolean.toString(Pay2Spawn.enable));
                    ctx.get().setPacketHandled(true);
                case SALE:
                    Pay2Spawn.getRewardsDB().addSale(Integer.parseInt(message.extraData[0]), Integer.parseInt(message.extraData[1]));
                    ctx.get().setPacketHandled(true);
                    break;
            }
        } else {
            switch (message.type) {
                case HANDSHAKE -> {
                    PermissionsHandler.getDB().newPlayer(ctx.get().getSender().getName().getString());
                    Pay2Spawn.playersWithValidConfig.add(ctx.get().getSender().getName().getString());
                    // Can't use return statement here cause you can't return multiple packets
                    if (ctx.get().getSender().getServer().isDedicatedServer() && Pay2Spawn.getConfig().forceServerconfig)
                        sendConfigToPlayer(ctx.get().getSender());
                    if (ctx.get().getSender().getServer().isDedicatedServer() && Pay2Spawn.getConfig().forceP2S)
                        sendForceToPlayer(ctx.get().getSender());

                    ctx.get().setPacketHandled(true);
                }
                case STATUS -> {
                    Player sender = ctx.get().getSender().getServer().getPlayerList().getPlayerByName(message.extraData[0]);
                    Helper.sendChatToPlayer((ServerPlayer) sender /*todo casting here check it*/, ctx.get().getSender().getName().getString() + " has Pay2Spawn " + (Boolean.parseBoolean(message.extraData[1]) ? "enabled." : "disabled."), ChatFormatting.AQUA);
                    ctx.get().setPacketHandled(true);
                }
            }
        }
    }
}

