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

package net.doubledoordev.pay2spawn.util;

import com.google.common.base.Strings;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.hud.Hud;
import net.doubledoordev.pay2spawn.hud.StatisticsHudEntry;
import net.doubledoordev.pay2spawn.hud.StatusHudEntry;
import net.doubledoordev.pay2spawn.hud.TotalDonationHudEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * ALL OF THE STATISTICS
 *
 * @author Dries007
 */
public class Statistics
{
    private static File statisticsFile;
    private static CompoundTag root = new CompoundTag();

    private static HashMap<String, Integer> killsMap       = new HashMap<>();
    private static TreeMap<String, Integer> sortedKillsMap = new TreeMap<>(new ValueComparator(killsMap));

    private static HashMap<String, Integer> spawnsMap       = new HashMap<>();
    private static TreeMap<String, Integer> sortedSpawnsMap = new TreeMap<>(new ValueComparator(spawnsMap));

    private static StatisticsHudEntry    spawnsStatisticsHudEntry;
    private static TotalDonationHudEntry totalDonationHudEntry;
    private static StatusHudEntry        statusHudEntry;

    private Statistics()
    {
    }

    public static void addToDonationAmount(double amount)
    {
        MetricsHelper.totalMoney += amount;
        totalDonationHudEntry.addToDonationamount(amount);
    }

    public static void handleKill(CompoundTag data)
    {
        Pay2Spawn.getLogger().warn("Debug kill data:" + JsonNBTHelper.parseNBT(data).toString());
        String name = data.getString("Reward");

        sortedKillsMap.clear();

        Integer i = killsMap.get(name);
        if (i == null) i = 0;
        killsMap.put(name, i + 1);

        sortedKillsMap.putAll(killsMap);

        save();
    }

    public static void handleSpawn(String name)
    {
        sortedSpawnsMap.clear();
        Integer i = spawnsMap.get(name);
        if (i == null) i = 0;
        spawnsMap.put(name, i + 1);

        sortedSpawnsMap.putAll(spawnsMap);

        update(sortedSpawnsMap, spawnsStatisticsHudEntry);

        save();
    }

    public static void preInit() throws IOException
    {
        statisticsFile = new File(Pay2Spawn.getFolder(), "Statistics.dat");
        if (statisticsFile.exists())
        {
            root = NbtIo.read(statisticsFile);
            if (root.contains("kills"))
            {
                for (Object tagName : root.getCompound("kills").getAllKeys())
                {
                    Tag tag = root.getCompound("kills").get(tagName.toString());
                    if (tag instanceof IntTag)
                    {
                        killsMap.put(tagName.toString(), ((IntTag) tag).getAsInt());
                    }
                }
                sortedKillsMap.putAll(killsMap);
            }

            if (root.contains("spawns"))
            {
                for (Object tagName : root.getCompound("spawns").getAllKeys())
                {
                    Tag tag = root.getCompound("spawns").get(tagName.toString());
                    if (tag instanceof IntTag)
                    {
                        spawnsMap.put(tagName.toString(), ((IntTag) tag).getAsInt());
                    }
                }
                sortedSpawnsMap.putAll(spawnsMap);
            }
        }

        spawnsStatisticsHudEntry = new StatisticsHudEntry("topSpawned", -1, 2, 5, "$amount x $name", "-- Top spawned rewards: --");
        Hud.INSTANCE.set.add(spawnsStatisticsHudEntry);
        update(sortedSpawnsMap, spawnsStatisticsHudEntry);

        totalDonationHudEntry = new TotalDonationHudEntry("totalDonation", 1, "Total amount donated: $$amount", root.contains("donated") ? root.getDouble("donated") : 0);
        Hud.INSTANCE.set.add(totalDonationHudEntry);

        statusHudEntry = new StatusHudEntry("status", 2);
        Hud.INSTANCE.set.add(statusHudEntry);
    }

    private static void update(TreeMap<String, Integer> map, StatisticsHudEntry hudEntry)
    {
        if (map == null || hudEntry == null) return;
        int i = 0;
        hudEntry.strings.clear();
        if (!Strings.isNullOrEmpty(hudEntry.getHeader())) Helper.addWithEmptyLines(hudEntry.strings, hudEntry.getHeader());
        for (Map.Entry<String, Integer> entry : map.entrySet())
        {
            if (i > hudEntry.getAmount()) break;
            i++;

            String key = entry.getKey();
            Integer value = entry.getValue();

            hudEntry.strings.add(hudEntry.getFormat().replace("$name", key).replace("$amount", value.toString()));
        }
    }

    public static void save()
    {
        CompoundTag kills = new CompoundTag();
        for (String name : killsMap.keySet())
        {
            kills.putInt(name, killsMap.get(name));
        }
        root.put("kills", kills);

        CompoundTag spawns = new CompoundTag();
        for (String name : spawnsMap.keySet())
        {
            spawns.putInt(name, spawnsMap.get(name));
        }
        root.put("spawns", spawns);
        root.putDouble("donated", totalDonationHudEntry.getDonated());

        try
        {
            NbtIo.write(root, statisticsFile);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    static class ValueComparator implements Comparator<String>
    {
        Map<String, Integer> base;

        public ValueComparator(Map<String, Integer> base)
        {
            this.base = base;
        }

        // Note: this comparator imposes orderings that are inconsistent with equals.
        public int compare(String a, String b)
        {
            if (base.get(a) >= base.get(b))
            {
                return -1;
            }
            else
            {
                return 1;
            } // returning 0 would merge keys
        }
    }
}
