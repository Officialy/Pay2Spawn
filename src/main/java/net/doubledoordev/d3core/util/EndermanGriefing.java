package net.doubledoordev.d3core.util;

import net.doubledoordev.d3core.D3Core;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.GameData;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Dries007
 */
public class EndermanGriefing
{
    public static boolean disable;
    public static boolean dropCarrying;
    public static String[] blacklist;
    public static String[] addList;

    private static HashMap<String, Boolean> reverseMap = new HashMap<>();

    public static void init()
    {
        if (disable)
        {
            FMLControlledNamespacedRegistry<Block> blockData = GameData.getBlockRegistry();
            for (Object key : blockData.getKeys())
            {
                Block block = (Block) blockData.getObject(key);
                reverseMap.put(blockData.getNameForObject(block), EnderMan.getCarriable(block));
                EnderMan.setCarriable(block, false);
            }
        }
        else
        {
            int added = 0, removed = 0;
            for (String item : addList)
            {
                List<Block> blocks = matchBlock(item);
                if (blocks.isEmpty())  D3Core.getLogger().warn("[EndermanGriefing] '{}' does not match any block...", item);
                else
                {
                    for (Block block : blocks)
                    {
                        reverseMap.put(item, EnderMan.getCarriable(block));
                        EnderMan.setCarriable(block, true);
                        added ++;
                    }
                }
            }
            for (String item : blacklist)
            {
                List<Block> blocks = matchBlock(item);
                if (blocks.isEmpty()) D3Core.getLogger().warn("[EndermanGriefing] '{}' does not match any block...", item);
                else
                {
                    for (Block block : blocks)
                    {
                        reverseMap.put(item, EnderMan.getCarriable(block));
                        EnderMan.setCarriable(block, false);
                        removed ++;
                    }
                }
            }
            D3Core.getLogger().info("[EndermanGriefing] Added {} and removed {} blocks to the Ederman grab list...", added, removed);
        }
    }

    private static List<Block> matchBlock(String item)
    {
        boolean ignored = false;
        ArrayList<Block> blocks = new ArrayList<>();
        Pattern pattern = Pattern.compile(item.replace("*", ".*?"));
        FMLControlledNamespacedRegistry<Block> blockData = GameData.getBlockRegistry();
        for (Block block : blockData.typeSafeIterable())
        {
            if (pattern.matcher(blockData.getNameForObject(block)).matches())
            {
                if (blockData.getId(block) > 255) ignored = true;
                else blocks.add(block);
            }
        }
        if (ignored) D3Core.getLogger().warn("[EndermanGriefing] Blocks with ID > 255 won't work! Some blocks matching {} have been ignored.", item);
        return blocks;
    }

    public static void undo()
    {
        for (String entry : reverseMap.keySet())
        {
            EnderMan.setCarriable(GameData.getBlockRegistry().getObject(entry), reverseMap.get(entry));
        }
    }
}
