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


import net.doubledoordev.pay2spawn.types.guis.StructureTypeGui;
import net.doubledoordev.pay2spawn.util.shapes.PointI;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

import static net.doubledoordev.pay2spawn.types.StructureType.*;
import static net.doubledoordev.pay2spawn.util.Constants.COMPOUND;

/**
 * Reads all blockID, metadata and NBT from a list of points
 * <p/>
 * Uses NBT instead of a stringified JSON array because of network efficiency
 *
 * @author Dries007
 */
public class StructureImportMessage {
    CompoundTag root;

    public StructureImportMessage(CompoundTag root) {
        this.root = root;
    }

    public StructureImportMessage(FriendlyByteBuf buf) {
        root = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(root);
    }

    public static void handle(StructureImportMessage message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().enqueueWork(() -> {
                int offsetx = message.root.getInt("x"), offsety = message.root.getInt("y"), offsetz = message.root.getInt("z");
                CompoundTag newRoot = new CompoundTag();
                ListTag newList = new ListTag();

                ListTag list = message.root.getList("list", COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    PointI point = new PointI(list.getCompound(i));
                    Level world = ctx.get().getSender().level;
                    int x = point.getX(), y = point.getY(), z = point.getZ();

                    // Set up the correct block data
                    ListTag blockDataNbt = new ListTag();
                    {
                        CompoundTag compound = new CompoundTag();

                        // BlockID
                        compound.putString(BLOCKID_KEY, ForgeRegistries.BLOCKS.getKey(world.getBlockState(new BlockPos(x, y, z)).getBlock()).toString());

                        // metaData
//                        int meta = world.getBlockMetadata(x, y, z);
//                        if (meta != 0) compound.putInt(META_KEY, meta);

                        // TileEntity
                        BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
                        if (te != null) {
                            CompoundTag teNbt = new CompoundTag();
                            te.load(teNbt); //todo was save
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
//     todo       return new StructureImportMessage(newRoot);
            });
        } else if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER)
            StructureTypeGui.importCallback(message.root);
    }
}