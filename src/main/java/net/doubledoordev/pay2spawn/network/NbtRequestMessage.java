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

import io.netty.buffer.ByteBuf;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.types.StructureType;
import net.doubledoordev.pay2spawn.util.EventHandler;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.pay2spawn.util.IIHasCallback;
import net.doubledoordev.pay2spawn.util.JsonNBTHelper;
import net.minecraft.nbt.CompoundTag;

import static net.doubledoordev.pay2spawn.types.StructureType.BLOCKID_KEY;

/**
 * Request NBT from the server
 *
 * @author Dries007
 */
public class NbtRequestMessage implements IMessage
{
    public static  IIHasCallback callbackItemType;
    public static  IIHasCallback callbackCustomEntityType;
    public static  IIHasCallback callbackFireworksType;
    private static IIHasCallback callbackBlockType;
    private        Type          type;
    /**
     * true = request
     * false = response
     */
    private        boolean       request;
    private        int           entityIdOrSlot;
    /**
     * Only used when response = false
     */
    private        String        response;

    private int x, y, z, dim;

    public NbtRequestMessage()
    {

    }

    public NbtRequestMessage(Type type, int entityIdOrSlot)
    {
        this.type = type;
        this.request = true;
        this.entityIdOrSlot = entityIdOrSlot;
    }

    public NbtRequestMessage(Type type)
    {
        this.type = type;
        this.request = true;
    }

    public NbtRequestMessage(Type type, String response)
    {
        this.type = type;
        this.request = false;
        this.response = response;
    }

    public NbtRequestMessage(int x, int y, int z, int dim)
    {
        this.type = Type.BLOCK;
        this.request = true;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
    }

    public NbtRequestMessage(Type type, int entityIdOrSlot, String response)
    {
        this.type = type;
        this.request = false;
        this.entityIdOrSlot = entityIdOrSlot;
        this.response = response;
    }

    public static void requestEntity(IIHasCallback instance)
    {
        callbackCustomEntityType = instance;
        EventHandler.addEntityTracking();
    }

    public static void requestByEntityID(int entityId)
    {
        Pay2Spawn.getSnw().sendToServer(new NbtRequestMessage(Type.ENTITY, entityId));
    }

    public static void requestBlock(int x, int y, int z, int dim)
    {
        Pay2Spawn.getSnw().sendToServer(new NbtRequestMessage(x, y, z, dim));
    }

    public static void requestFirework(IIHasCallback instance)
    {
        callbackFireworksType = instance;
        Pay2Spawn.getSnw().sendToServer(new NbtRequestMessage(Type.FIREWORK));
    }

    public static void requestItem(IIHasCallback instance, int slot)
    {
        callbackItemType = instance;
        Pay2Spawn.getSnw().sendToServer(new NbtRequestMessage(Type.ITEM, slot));
    }

    public static void requestBlock(IIHasCallback instance)
    {
        callbackBlockType = instance;
        EventHandler.addBlockTracker();
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        type = Type.values()[buf.readInt()];
        request = buf.readBoolean();
        if (request)
        {
            if (type == Type.ENTITY || type == Type.ITEM) entityIdOrSlot = buf.readInt();
            else if (type == Type.BLOCK)
            {
                x = buf.readInt();
                y = buf.readInt();
                z = buf.readInt();
                dim = buf.readInt();
            }
        }
        else
        {
            response = Helper.readLongStringToByteBuf(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(type.ordinal());
        buf.writeBoolean(request);
        if (request)
        {
            if (type == Type.ENTITY || type == Type.ITEM) buf.writeInt(entityIdOrSlot);
            if (type == Type.BLOCK)
            {
                buf.writeInt(x);
                buf.writeInt(y);
                buf.writeInt(z);
                buf.writeInt(dim);
            }
        }
        else
        {
            Helper.writeLongStringToByteBuf(buf, response);
        }
    }

    public static enum Type
    {
        ITEM,
        BLOCK,
        ENTITY,
        FIREWORK
    }

    public static class Handler implements IMessageHandler<NbtRequestMessage, IMessage>
    {
        @Override
        public IMessage onMessage(NbtRequestMessage message, MessageContext ctx)
        {
            if (ctx.side.isClient())
            {
                switch (message.type)
                {
                    case ENTITY:
                        callbackCustomEntityType.callback(message.response);
                        break;
                    case FIREWORK:
                        callbackFireworksType.callback(message.response);
                        break;
                    case ITEM:
                        callbackItemType.callback(message.response);
                        break;
                    case BLOCK:
                        callbackBlockType.callback(message.response);
                        break;
                }
            }
            else
            {
                switch (message.type)
                {
                    case ENTITY:
                        CompoundTag nbt = new CompoundTag();
                        Entity entity = ctx.getServerHandler().playerEntity.level.getEntityByID(message.entityIdOrSlot);
                        entity.writeToNBT(nbt);
                        entity.writeToNBTOptional(nbt);
                        nbt.putString("id", EntityList.getEntityString(entity));
                        return new NbtRequestMessage(message.type, JsonNBTHelper.parseNBT(nbt).toString());
                    case FIREWORK:
                        ItemStack itemStack = ctx.getServerHandler().playerEntity.getHeldItem();
                        if (itemStack != null && itemStack.getItem() instanceof ItemFirework)
                        {
                            return new NbtRequestMessage(message.type, JsonNBTHelper.parseNBT(ctx.getServerHandler().playerEntity.getHeldItem().writeToNBT(new CompoundTag())).toString());
                        }
                        else
                        {
                            Helper.sendChatToPlayer(ctx.getServerHandler().playerEntity, "You are not holding an ItemFirework...", ChatFormatting.RED);
                        }
                        break;
                    case ITEM:
                        if (message.entityIdOrSlot == -1)
                        {
                            if (ctx.getServerHandler().playerEntity.getHeldItem() != null)
                            {
                                return new NbtRequestMessage(message.type, message.entityIdOrSlot, JsonNBTHelper.parseNBT(ctx.getServerHandler().playerEntity.getHeldItem().writeToNBT(new CompoundTag())).toString());
                            }
                            else
                            {
                                Helper.sendChatToPlayer(ctx.getServerHandler().playerEntity, "You are not holding an item...", ChatFormatting.RED);
                            }
                        }
                        else
                        {
                            ItemStack stack = ctx.getServerHandler().playerEntity.inventory.getStackInSlot(message.entityIdOrSlot);
                            if (stack != null) return new NbtRequestMessage(message.type, message.entityIdOrSlot, JsonNBTHelper.parseNBT(stack.writeToNBT(new CompoundTag())).toString());
                            else return new NbtRequestMessage(message.type, message.entityIdOrSlot, "{}");
                        }
                        break;
                    case BLOCK:
                        CompoundTag compound = new CompoundTag();
                        World world = DimensionManager.getWorld(message.dim);
                        compound.putInt(BLOCKID_KEY, Block.getIdFromBlock(world.getBlock(message.x, message.y, message.z)));
                        //compound.putString(StructureType.BLOCKID_KEY, GameData.getBlockRegistry().getNameForObject(world.getBlock(message.x, message.y, message.z)));
                        compound.putInt(StructureType.META_KEY, world.getBlockMetadata(message.x, message.y, message.z));
                        TileEntity tileEntity = world.getTileEntity(message.x, message.y, message.z);
                        if (tileEntity != null)
                        {
                            CompoundTag te = new CompoundTag();
                            tileEntity.writeToNBT(te);
                            compound.put(StructureType.TEDATA_KEY, te);
                        }
                        return new NbtRequestMessage(message.type, JsonNBTHelper.parseNBT(compound).toString());
                }
            }
            return null;
        }
    }
}
