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
import net.doubledoordev.pay2spawn.types.guis.StructureTypeGui;
import net.doubledoordev.pay2spawn.util.shapes.PointI;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import static net.doubledoordev.pay2spawn.types.StructureType.*;
import static net.doubledoordev.pay2spawn.util.Constants.COMPOUND;

/**
 * Reads all blockID, metadata and NBT from a list of points
 * <p/>
 * Uses NBT instead of a stringified JSON array because of network efficiency
 *
 * @author Dries007
 */
public class StructureImportMessage implements IMessage
{
    CompoundTag root;

    public StructureImportMessage()
    {
    }

    public StructureImportMessage(CompoundTag root)
    {
        this.root = root;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        root = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeTag(buf, root);
    }

    public static class Handler implements IMessageHandler<StructureImportMessage, IMessage>
    {
        @Override
        public IMessage onMessage(StructureImportMessage message, MessageContext ctx)
        {
            if (ctx.side.isServer())
            {
                int offsetx = message.root.getInt("x"), offsety = message.root.getInt("y"), offsetz = message.root.getInt("z");
                CompoundTag newRoot = new CompoundTag();
                ListTag newList = new ListTag();

                ListTag list = message.root.getTagList("list", COMPOUND);
                for (int i = 0; i < list.size(); i++)
                {
                    PointI point = new PointI(list.getCompoundTagAt(i));
                    Level world = ctx.getServerHandler().playerEntity.level;
                    int x = point.getX(), y = point.getY(), z = point.getZ();

                    // Set up the correct block data
                    ListTag blockDataNbt = new ListTag();
                    {
                        CompoundTag compound = new CompoundTag();

                        // BlockID
                        compound.putInt(BLOCKID_KEY, Block.getIdFromBlock(world.getBlock(x, y, z)));

                        // metaData
                        int meta = world.getBlockMetadata(x, y, z);
                        if (meta != 0) compound.putInt(META_KEY, meta);

                        // TileEntity
                        BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
                        if (te != null)
                        {
                            CompoundTag teNbt = new CompoundTag();
                            te.writeToNBT(teNbt);
                            teNbt.remove("x");
                            teNbt.remove("y");
                            teNbt.remove("z");
                            compound.put(TEDATA_KEY, teNbt);
                        }

                        blockDataNbt.add(compound);
                    }
                    CompoundTag shapeNbt = point.move(offsetx, offsety, offsetz).toNBT();
                    shapeNbt.put(BLOCKDATA_KEY, blockDataNbt);
                    newList.add(shapeNbt);
                }

                newRoot.put("list", newList);
                return new StructureImportMessage(newRoot);
            }
            else
            {
                StructureTypeGui.importCallback(message.root);
                return null;
            }
        }
    }
}
