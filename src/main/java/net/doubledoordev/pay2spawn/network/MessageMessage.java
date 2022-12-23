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

import com.google.common.base.Strings;

import com.google.common.base.Supplier;
import io.netty.buffer.ByteBuf;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.util.Donation;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.pay2spawn.util.Reward;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkEvent;

import static net.doubledoordev.pay2spawn.util.Constants.GSON;
import static net.doubledoordev.pay2spawn.util.Constants.GSON_NOPP;

/**
 * A message for messages?
 *
 * @author Dries007
 */
public class MessageMessage {
    private Reward reward;
    private Donation donation;
    private String message, name;
    private double amount;
    private int countdown;

    public MessageMessage(Reward reward, Donation donation) {
        this.reward = reward;
        this.donation = donation;
    }

    public MessageMessage() {

    }

    public static MessageMessage fromBytes(FriendlyByteBuf buf) {
        message = ByteBufUtils.readUTF8String(buf);
        name = ByteBufUtils.readUTF8String(buf);
        amount = buf.readDouble();
        countdown = buf.readInt();

        donation = GSON.fromJson(Helper.readLongStringToByteBuf(buf), Donation.class);

    }

    public void toBytes(FriendlyByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, reward.getMessage());
        ByteBufUtils.writeUTF8String(buf, reward.getName());
        buf.writeDouble(reward.getAmount());
        buf.writeInt(reward.getCountdown());

        Helper.writeLongStringToByteBuf(buf, GSON_NOPP.toJson(donation));
    }

    public static void handle(MessageMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            String format = Helper.formatColors(Pay2Spawn.getConfig().serverMessage);
            if (Strings.isNullOrEmpty(format)) {
                return;
            }

            format = format.replace("$name", message.donation.username);
            format = format.replace("$amount", message.donation.amount + "");
            format = format.replace("$note", message.donation.note);
            format = format.replace("$streamer", ctx.get().getSender().getDisplayName().toString());
            format = format.replace("$reward_message", message.message);
            format = format.replace("$reward_name", message.name);
            format = format.replace("$reward_amount", message.amount + "");
            format = format.replace("$reward_countdown", message.countdown + "");

            ctx.get().getSender().displayClientMessage(new TextComponent(format), false);
            ctx.get().setPacketHandled(true);
        });
        ctx.get().setPacketHandled(true);
    }

}
