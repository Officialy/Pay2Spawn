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

import net.doubledoordev.pay2spawn.P2SConfig;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.oldforge.Configuration;

import java.util.ArrayList;

/**
 * Base class for any on screen thing to display statistics
 *
 * @author Dries007
 */
public class StatisticsHudEntry implements IHudEntry
{
    public final ArrayList<String> strings = new ArrayList<>();
    int position, amount, maxAmount, defaultPosition, defaultAmount;
    String header = "", format = "", configCat = "", defaultFormat = "", defaultHeader = "";
    private boolean writeToFile = true;

    public StatisticsHudEntry(String configCat, int maxAmount, int defaultPosition, int defaultAmount, String defaultFormat, String defaultHeader)
    {
        this.configCat = configCat;
        this.maxAmount = maxAmount;
        this.defaultPosition = defaultPosition;
        this.defaultAmount = defaultAmount;
        this.defaultFormat = defaultFormat;
        this.defaultHeader = defaultHeader;

        updateConfig();
    }

    @Override
    public int getPosition()
    {
        return position;
    }

    @Override
    public int getAmount()
    {
        return amount;
    }

    @Override
    public String getHeader()
    {
        return "";
    }

    @Override
    public String getFormat()
    {
        return format;
    }

    @Override
    public void addToList(ArrayList<String> list)
    {
        if (position != 0)
        {
            list.addAll(strings);
        }
    }

    @Override
    public void updateConfig()
    {
        Configuration config = Pay2Spawn.getConfig().configuration;

        position = config.get(P2SConfig.HUD + "." + configCat, "position", defaultPosition, "0 = off, 1 = left top, 2 = right top, 3 = left bottom, 4 = right bottom.").getInt(defaultPosition);
        amount = config.get(P2SConfig.HUD + "." + configCat, "amount", amount).getInt(amount);
        if (maxAmount != -1 && amount > maxAmount) amount = maxAmount;

        format = Helper.formatColors(config.get(P2SConfig.HUD + "." + configCat, "format", defaultFormat).getString());
        header = Helper.formatColors(config.get(P2SConfig.HUD + "." + configCat, "header", defaultHeader, "Empty for no header. Use \\n for a blank line.").getString()).trim();

        writeToFile = config.getBoolean("writeToFile", configCat, writeToFile, "Write to a file for external use.");
    }

    @Override
    public String getFilename()
    {
        return "stats.txt";
    }

    @Override
    public boolean writeToFile()
    {
        return writeToFile;
    }
}
