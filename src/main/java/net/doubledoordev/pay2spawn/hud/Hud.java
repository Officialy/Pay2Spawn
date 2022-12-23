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

package net.doubledoordev.pay2spawn.hud;

import com.google.common.base.Joiner;
import net.doubledoordev.pay2spawn.P2SConfig;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.oldforge.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Handler for the event, keeps track of all active IHudEntry s
 *
 * @author Dries007
 */
public class Hud
{
    public static final Hud                INSTANCE = new Hud();
    public final        HashSet<IHudEntry> set      = new HashSet<>();
    private final File folder;
    private String lineSeperator = " | ";
    private Joiner joiner;

    private Hud()
    {
        MinecraftForge.EVENT_BUS.register(this);
        folder = new File(Pay2Spawn.getFolder(), "textFiles");
        //noinspection ResultOfMethodCallIgnored
        folder.mkdir();
    }

    public void render(ArrayList<String> left, ArrayList<String> right, ArrayList<String> bottomLeft, ArrayList<String> bottomRight)
    {
        for (IHudEntry hudEntry : set)
        {
            switch (hudEntry.getPosition())
            {
                case 1:
                    hudEntry.addToList(left);
                    break;
                case 2:
                    hudEntry.addToList(right);
                    break;
                case 3:
                    hudEntry.addToList(bottomLeft);
                    break;
                case 4:
                    hudEntry.addToList(bottomRight);
                    break;
            }
        }
    }

    public void doConfig()
    {
        Configuration config = Pay2Spawn.getConfig().configuration;
        lineSeperator = config.get(P2SConfig.HUD, "lineSeperator", lineSeperator, "Separator in between 2 or more lines when writing to the text tile. Use \\n to indicate a new line.").getString();
        for (IHudEntry hudEntry : set) hudEntry.updateConfig();

        joiner = Joiner.on(lineSeperator);
    }

    @SubscribeEvent
    public void writefile(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        ArrayList<String> allLines = new ArrayList<>();
        for (IHudEntry hudEntry : set)
        {
            if (!hudEntry.writeToFile()) continue;

            ArrayList<String> lines = new ArrayList<>();
            hudEntry.addToList(lines);

            String text = joiner.join(lines);
            allLines.add(text);

            try
            {
                FileUtils.writeStringToFile(new File(folder, hudEntry.getFilename()), joiner.join(lines));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            FileUtils.writeStringToFile(new File(folder, "Combined.txt"), joiner.join(allLines));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
