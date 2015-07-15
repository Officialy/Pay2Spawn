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

package net.doubledoordev.pay2spawn.types;

import com.google.gson.JsonObject;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.permissions.Node;
import net.doubledoordev.pay2spawn.types.guis.StructureTypeGui;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.pay2spawn.util.shapes.*;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraftforge.common.config.Configuration;

import java.util.*;

import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * @author Dries007
 */
public class StructureType extends TypeBase
{
    public static final String                  SHAPES_KEY       = "shapes";
    public static final String                  BLOCKDATA_KEY    = "blockData";
    public static final String                  TEDATA_KEY       = "tileEntityData";
    public static final String                  BLOCKID_KEY      = "blockID";
    public static final String                  META_KEY         = "meta";
    public static final String                  WEIGHT_KEY       = "weight";
    public static final String                  ROTATE_KEY       = "rotate";
    public static final String                  BASEROTATION_KEY = "baseRotation";
    public static final HashMap<String, String> typeMap          = new HashMap<>();

    static
    {
        typeMap.put(BLOCKID_KEY, NBTTypes[INT]);
        typeMap.put(META_KEY, NBTTypes[INT]);
        typeMap.put(WEIGHT_KEY, NBTTypes[INT]);
        typeMap.put(ROTATE_KEY, NBTTypes[BYTE]);
        typeMap.put(BASEROTATION_KEY, NBTTypes[BYTE]);
    }

    private static final String NAME = "structure";
    public static int[][] bannedBlocks;

    public static void applyShape(IShape shape, EntityPlayer player, ArrayList<NBTTagCompound> blockDataNbtList, byte baseRotation)
    {
        try
        {
            ArrayList<BlockData> blockDataList = new ArrayList<>();
            for (NBTTagCompound compound : blockDataNbtList)
            {
                BlockData blockData = new BlockData(compound);
                for (int i = 0; i <= blockData.weight; i++)
                    blockDataList.add(blockData);
            }

            int x = Helper.round(player.posX), y = Helper.round(player.posY + 1), z = Helper.round(player.posZ);
            Collection<PointI> points = shape.rotate(baseRotation).rotate(baseRotation == -1 ? -1 : Helper.getHeading(player)).move(x, y, z).getPoints();
            for (PointI p : points)
            {
                if (!shape.getReplaceableOnly() || player.worldObj.getBlock(p.getX(), p.getY(), p.getZ()).isReplaceable(player.worldObj, p.getX(), p.getY(), p.getZ()))
                {
                    BlockData block = blockDataList.size() == 1 ? blockDataList.get(0) : Helper.getRandomFromSet(blockDataList);
                    Block block1 = Block.getBlockById(block.id);
                    player.worldObj.setBlock(p.getX(), p.getY(), p.getZ(), block1, block.meta, 2);
                    if (block.te != null)
                    {
                        TileEntity tileEntity = TileEntity.createAndLoadEntity(block.te);
                        tileEntity.setWorldObj(player.worldObj);
                        tileEntity.xCoord = p.getX();
                        tileEntity.yCoord = p.getY();
                        tileEntity.zCoord = p.getZ();
                        player.worldObj.setTileEntity(p.getX(), p.getY(), p.getZ(), tileEntity);
                    }
                }
            }
        }
        catch (BlockData.BannedBlockException e)
        {
            ((EntityPlayerMP) player).playerNetServerHandler.kickPlayerFromServer(e.getMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Pay2Spawn.getLogger().warn("Error spawning in shape.");
            Pay2Spawn.getLogger().warn("Shape: " + shape.toString());
            Pay2Spawn.getLogger().warn("Player: " + player);
            Pay2Spawn.getLogger().warn("BlockData array: " + Arrays.deepToString(blockDataNbtList.toArray()));
        }
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public NBTTagCompound getExample()
    {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList shapesList = new NBTTagList();

        // Sphere
        {
            NBTTagCompound shapeNbt = Shapes.storeShape(new Sphere(10).setHollow(true).setReplaceableOnly(true));

            NBTTagList blockDataNbt = new NBTTagList();
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 35);
                compound.setInteger(WEIGHT_KEY, 5);
                blockDataNbt.appendTag(compound);
            }
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 35);
                compound.setInteger(META_KEY, 5);
                blockDataNbt.appendTag(compound);
            }

            shapeNbt.setTag(BLOCKDATA_KEY, blockDataNbt);

            shapesList.appendTag(shapeNbt);
        }

        // Box
        {
            NBTTagCompound shapeNbt = Shapes.storeShape(new Box(new PointI(-2, -3, 5), 5, 2, 3).setReplaceableOnly(true));

            NBTTagList blockDataNbt = new NBTTagList();
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 98);
                blockDataNbt.appendTag(compound);
            }
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 98);
                compound.setInteger(META_KEY, 1);
                blockDataNbt.appendTag(compound);
            }
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 98);
                compound.setInteger(META_KEY, 2);
                blockDataNbt.appendTag(compound);
            }
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 98);
                compound.setInteger(META_KEY, 3);
                blockDataNbt.appendTag(compound);
            }
            shapeNbt.setTag(BLOCKDATA_KEY, blockDataNbt);

            shapesList.appendTag(shapeNbt);
        }

        // Cylinder
        {
            NBTTagCompound shapeNbt = Shapes.storeShape(new Cylinder(new PointI(0, 3, 0), 12));

            NBTTagList blockDataNbt = new NBTTagList();
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 99);
                compound.setInteger(META_KEY, 14);
                blockDataNbt.appendTag(compound);
            }
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 100);
                compound.setInteger(META_KEY, 14);
                blockDataNbt.appendTag(compound);
            }
            shapeNbt.setTag(BLOCKDATA_KEY, blockDataNbt);

            shapesList.appendTag(shapeNbt);
        }

        // Pillar
        {
            NBTTagCompound shapeNbt = Shapes.storeShape(new Pillar(new PointI(-2, 0, -6), 15));

            NBTTagList blockDataNbt = new NBTTagList();
            for (int meta = 0; meta < 16; meta++)
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 159);
                compound.setInteger(META_KEY, meta);
                blockDataNbt.appendTag(compound);
            }
            shapeNbt.setTag(BLOCKDATA_KEY, blockDataNbt);

            shapesList.appendTag(shapeNbt);
        }

        // Point
        {
            NBTTagCompound shapeNbt = Shapes.storeShape(new PointI(0, 0, 0));

            NBTTagList blockDataNbt = new NBTTagList();
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 54);

                TileEntityChest chest = new TileEntityChest();
                chest.setInventorySlotContents(13, new ItemStack(Items.golden_apple));
                NBTTagCompound chestNbt = new NBTTagCompound();
                chest.writeToNBT(chestNbt);
                compound.setTag(TEDATA_KEY, chestNbt);

                blockDataNbt.appendTag(compound);
            }
            shapeNbt.setTag(BLOCKDATA_KEY, blockDataNbt);

            shapesList.appendTag(shapeNbt);
        }

        // Point
        {
            NBTTagCompound shapeNbt = Shapes.storeShape(new PointI(-1, -2, -1));

            NBTTagList blockDataNbt = new NBTTagList();
            for (Object mob : EntityList.entityEggs.keySet())
            {
                NBTTagCompound compound = new NBTTagCompound();
                compound.setInteger(BLOCKID_KEY, 52);

                TileEntityMobSpawner mobSpawner = new TileEntityMobSpawner();
                mobSpawner.func_145881_a().setEntityName(EntityList.getStringFromID((Integer) mob));
                NBTTagCompound spawnerNbt = new NBTTagCompound();
                mobSpawner.writeToNBT(spawnerNbt);

                // Removes some clutter, not really necessary though
                spawnerNbt.removeTag("x");
                spawnerNbt.removeTag("y");
                spawnerNbt.removeTag("z");

                compound.setTag(TEDATA_KEY, spawnerNbt);

                blockDataNbt.appendTag(compound);
            }
            shapeNbt.setTag(BLOCKDATA_KEY, blockDataNbt);

            shapesList.appendTag(shapeNbt);
        }

        root.setTag(SHAPES_KEY, shapesList);
        return root;
    }

    @Override
    public void spawnServerSide(EntityPlayerMP player, NBTTagCompound dataFromClient, NBTTagCompound rewardData)
    {
        byte baseRotation = dataFromClient.getBoolean(ROTATE_KEY) ? dataFromClient.getByte(BASEROTATION_KEY) : -1;
        NBTTagList list = dataFromClient.getTagList(SHAPES_KEY, COMPOUND);
        for (int i = 0; i < list.tagCount(); i++)
        {
            NBTTagCompound shapeNbt = list.getCompoundTagAt(i);

            ArrayList<NBTTagCompound> blockDataList = new ArrayList<>();
            NBTTagList blockDataNbt = shapeNbt.getTagList(BLOCKDATA_KEY, COMPOUND);
            for (int j = 0; j < blockDataNbt.tagCount(); j++)
                blockDataList.add(blockDataNbt.getCompoundTagAt(j));

            applyShape(Shapes.loadShape(shapeNbt), player, blockDataList, baseRotation);
        }
    }

    @Override
    public void doConfig(Configuration configuration)
    {
        configuration.addCustomCategoryComment(TYPES_CAT, "Reward config options");
        configuration.addCustomCategoryComment(TYPES_CAT + '.' + NAME, "Used when spawning structures");
        String[] bannedBlocksStrings = configuration.get(TYPES_CAT + '.' + NAME, "bannedBlocks", new String[0], "Banned blocks, format like this:\nid:metaData => Ban only that meta\nid => Ban all meta of that block").getStringList();
        bannedBlocks = new int[bannedBlocksStrings.length][];
        for (int i = 0; i < bannedBlocksStrings.length; i++)
        {
            String[] split = bannedBlocksStrings[i].split(":");
            if (split.length == 1) bannedBlocks[i] = new int[]{Integer.parseInt(split[0])};
            else bannedBlocks[i] = new int[]{Integer.parseInt(split[0]), Integer.parseInt(split[1])};
        }
    }

    @Override
    public void openNewGui(int rewardID, JsonObject data)
    {
        new StructureTypeGui(rewardID, NAME, data, typeMap);
    }

    @Override
    public Collection<Node> getPermissionNodes()
    {
        HashSet<Node> nodes = new HashSet<>();

        nodes.add(new Node(NAME));

        return nodes;
    }

    @Override
    public Node getPermissionNode(EntityPlayer player, NBTTagCompound dataFromClient)
    {
        return new Node(NAME);
    }

    @Override
    public String replaceInTemplate(String id, JsonObject jsonObject)
    {
        return id;
    }

    public static class BlockData
    {
        final int id, meta, weight;
        final NBTTagCompound te;

        private BlockData(NBTTagCompound compound) throws BannedBlockException
        {
            id = compound.getInteger(BLOCKID_KEY);
            meta = compound.getInteger(META_KEY);
            weight = compound.hasKey(WEIGHT_KEY) ? compound.getInteger(WEIGHT_KEY) : 1;

            te = compound.hasKey(TEDATA_KEY) ? compound.getCompoundTag(TEDATA_KEY) : null;

            for (int[] ban : bannedBlocks)
            {
                if (ban.length == 1 && id == ban[0]) throw new BannedBlockException("You are trying to use a globally banned block!\nBlockid: " + ban[0]);
                else if (ban.length == 2 && id == ban[0] && meta == ban[1]) throw new BannedBlockException("You are trying to use a globally banned block!\nBlockid:" + ban[0] + ":" + ban[1]);
            }
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof BlockData)) return false;

            BlockData data = (BlockData) o;

            return id == data.id && meta == data.meta && !(te != null ? !te.equals(data.te) : data.te != null);
        }

        @Override
        public String toString()
        {
            return "BlockData{" +
                    "id=" + id +
                    ", meta=" + meta +
                    ", weight=" + weight +
                    ", te=" + te +
                    '}';
        }

        public class BannedBlockException extends Exception
        {

            public BannedBlockException(String s)
            {
                super(s);
            }
        }
    }
}
