///*
// * Copyright (c) 2014, DoubleDoorDevelopment
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *  Redistributions of source code must retain the above copyright notice, this
// *   list of conditions and the following disclaimer.
// *
// *  Redistributions in binary form must reproduce the above copyright notice,
// *   this list of conditions and the following disclaimer in the documentation
// *   and/or other materials provided with the distribution.
// *
// *  Neither the name of the project nor the names of its
// *   contributors may be used to endorse or promote products derived from
// *   this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package net.doubledoordev.pay2spawn.network;
//
//
//import io.netty.buffer.ByteBuf;
//import net.doubledoordev.pay2spawn.Pay2Spawn;
//import net.doubledoordev.pay2spawn.permissions.BanHelper;
//import net.doubledoordev.pay2spawn.permissions.Node;
//import net.doubledoordev.pay2spawn.permissions.PermissionsHandler;
//import net.doubledoordev.pay2spawn.types.TypeBase;
//import net.doubledoordev.pay2spawn.types.TypeRegistry;
//import net.doubledoordev.pay2spawn.util.Helper;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.util.ChatFormatting;
//
///**
// * Uses NBT instead of a stringified JSON array because of network efficiency
// *
// * @author Dries007
// */
//public class RewardMessage implements IMessage
//{
//    private CompoundTag reward, rewardData;
//
//    public RewardMessage()
//    {
//    }
//
//    public RewardMessage(CompoundTag reward, CompoundTag rewardData)
//    {
//        this.reward = reward;
//        this.rewardData = rewardData;
//    }
//
//    @Override
//    public void fromBytes(ByteBuf buf)
//    {
//        reward = ByteBufUtils.readTag(buf);
//        rewardData = ByteBufUtils.readTag(buf);
//    }
//
//    @Override
//    public void toBytes(ByteBuf buf)
//    {
//        ByteBufUtils.writeTag(buf, reward);
//        ByteBufUtils.writeTag(buf, rewardData);
//    }
//
//    public static class Handler implements IMessageHandler<RewardMessage, IMessage>
//    {
//        @Override
//        public IMessage onMessage(RewardMessage message, Supplier<NetworkEvent.Context> ctx)
//        {
//            try
//            {
//                TypeBase type = TypeRegistry.getByName(message.reward.getString("type").toLowerCase());
//                CompoundTag nbt = message.reward.getCompound("data");
//                Node node = type.getPermissionNode(ctx.get().getSender(), nbt);
//                if (BanHelper.isBanned(node))
//                {
//                    Helper.sendChatToPlayer(ctx.get().getSender(), "This node (" + node + ") is banned.", ChatFormatting.RED);
//                    Pay2Spawn.getLogger().warn(ctx.get().getSender().getCommandSenderName() + " tried using globally banned node " + node + ".");
//                    return null;
//                }
//                if (PermissionsHandler.needPermCheck(ctx.get().getSender()) && !PermissionsHandler.hasPermissionNode(ctx.get().getSender(), node))
//                {
//                    Pay2Spawn.getLogger().warn(ctx.get().getSender().getDisplayName() + " doesn't have perm node " + node.toString());
//                    return null;
//                }
//                type.spawnServerSide(ctx.get().getSender(), nbt, message.rewardData);
//            }
//            catch (Exception e)
//            {
//                Pay2Spawn.getLogger().warn("ERROR TYPE 3: Error spawning a reward on the server.");
//                Pay2Spawn.getLogger().warn("Type: " + message.reward.getString("type").toLowerCase());
//                Pay2Spawn.getLogger().warn("Data: " + message.reward.getCompound("data"));
//                e.printStackTrace();
//            }
//            return null;
//        }
//    }
//}
