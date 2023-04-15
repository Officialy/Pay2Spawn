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

import com.google.gson.JsonObject;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.permissions.BanHelper;
import net.doubledoordev.pay2spawn.permissions.Node;
import net.doubledoordev.pay2spawn.permissions.PermissionsHandler;
import net.doubledoordev.pay2spawn.random.RndVariable;
import net.doubledoordev.pay2spawn.types.TypeBase;
import net.doubledoordev.pay2spawn.types.TypeRegistry;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.pay2spawn.util.JsonNBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Allows testing of rewards
 *
 * @author Dries007
 */
public class TestMessage {
    private String name;
    private CompoundTag data;

    public TestMessage() {

    }

    public TestMessage(String name, CompoundTag data) {
        this.name = name;
        this.data = data;
    }

    public static void sendToServer(String name, JsonObject jsondata) {
        if (Minecraft.getInstance().isPaused())
            Helper.msg(ChatFormatting.RED + "Some tests don't work while paused! Use your chat key to lose focus.");
        CompoundTag data = JsonNBTHelper.parseJSON(jsondata);
        if (Helper.checkTooBigForNetwork(data)) return;
        Pay2Spawn.getSnw().sendToServer(new TestMessage(name, data));
    }


    public TestMessage(FriendlyByteBuf buf) {
        name = buf.readUtf();
        data = buf.readNbt();
    }


    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeNbt(data);
    }

    public static void handle(TestMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            RndVariable.reset();

            CompoundTag rewardData = new CompoundTag();
            Helper.sendChatToPlayer(ctx.get().getSender(), "Testing reward " + message.name + ".");
            Pay2Spawn.getLogger().info("Test by " + ctx.get().getSender().getName().getContents() + " Type: " + message.name + " Data: " + message.data);
            TypeBase type = TypeRegistry.getByName(message.name);

            Node node = type.getPermissionNode(ctx.get().getSender(), message.data);
            if (BanHelper.isBanned(node)) {
                Helper.sendChatToPlayer(ctx.get().getSender(), "This node (" + node + ") is banned.", ChatFormatting.RED);
                Pay2Spawn.getLogger().warn(ctx.get().getSender().getName() + " tried using globally banned node " + node + ".");
                return;
            }
            if (PermissionsHandler.needPermCheck(ctx.get().getSender()) && !PermissionsHandler.hasPermissionNode(ctx.get().getSender(), node)) { //todo serverplayer
                Pay2Spawn.getLogger().warn(ctx.get().getSender().getName().getContents() + " doesn't have perm node " + node.toString());
                return;
            }
            type.spawnServerSide(ctx.get().getSender(), message.data, rewardData);
            ctx.get().setPacketHandled(true);
        });
    }

}
