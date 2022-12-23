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

package net.doubledoordev.pay2spawn.checkers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.doubledoordev.pay2spawn.hud.DonationsBasedHudEntry;
import net.doubledoordev.pay2spawn.hud.Hud;
import net.doubledoordev.pay2spawn.util.Donation;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.oldforge.Configuration;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;

import static net.doubledoordev.pay2spawn.util.Constants.BASECAT_TRACKERS;
import static net.doubledoordev.pay2spawn.util.Constants.JSON_PARSER;

/**
 * @author Dries007
 */
public class ImrasingChecker extends AbstractChecker implements Runnable
{
    public final static ImrasingChecker  INSTANCE          = new ImrasingChecker();
    public final static SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public final static String           NAME              = "imraising";
    public final static String           CAT               = BASECAT_TRACKERS + '.' + NAME;
    public final static String           URL               = "https://imraising.tv/api/v1/donations";
    public final static String           HEADER            = "APIKey apikey=\"%s\"";

    DonationsBasedHudEntry topDonationsBasedHudEntry, recentDonationsBasedHudEntry;
    String  APIKey   = "";
    boolean enabled  = false;
    int     interval = 20;

    private ImrasingChecker()
    {
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void init()
    {
        Hud.INSTANCE.set.add(topDonationsBasedHudEntry);
        Hud.INSTANCE.set.add(recentDonationsBasedHudEntry);

        new Thread(this, getName()).start();
    }

    @Override
    public boolean enabled()
    {
        return enabled && !APIKey.isEmpty();
    }

    @Override
    public void doConfig(Configuration configuration)
    {
        configuration.addCustomCategoryComment(CAT, "This is the checker for imraising.tv");

        enabled = configuration.get(CAT, "enabled", enabled).getBoolean(enabled);
        APIKey = configuration.get(CAT, "APIKey", APIKey).getString();
        interval = configuration.get(CAT, "interval", interval, "The time in between polls (in seconds).").getInt();
        min_donation = configuration.get(CAT, "min_donation", min_donation, "Donations below this amount will only be added to statistics and will not spawn rewards").getDouble();

        recentDonationsBasedHudEntry = new DonationsBasedHudEntry("recent" + NAME + ".txt", CAT + ".recentDonations", -1, 2, 5, "$name: $$amount", "-- Recent donations --", CheckerHandler.RECENT_DONATION_COMPARATOR);
        topDonationsBasedHudEntry = new DonationsBasedHudEntry("top" + NAME + ".txt", CAT + ".topDonations", -1, 1, 5, "$name: $$amount", "-- Top donations --", CheckerHandler.AMOUNT_DONATION_COMPARATOR);

        // Donation tracker doesn't allow a poll interval faster than 5 seconds
        // They will IP ban anyone using a time below 5 so force the value to be safe
        if (interval < 5)
        {
            interval = 5;
            // Now force the config setting to 5
            configuration.get(CAT, "interval", "The time in between polls minimum 5 (in seconds).").set(interval);
        }
    }

    @Override
    public DonationsBasedHudEntry[] getDonationsBasedHudEntries()
    {
        return new DonationsBasedHudEntry[]{topDonationsBasedHudEntry, recentDonationsBasedHudEntry};
    }

    @Override
    public void run()
    {
        // Process any current donations from the API
        processDonationAPI(true);

        // Start the processing loop
        while (true)
        {
            // Pause the configured wait period
            doWait(interval);

            // Check for any new donations
            processDonationAPI(false);
        }
    }

    /**
     * Connects to the API and attempt to process any donations
     *
     * @param firstRun <code>boolean</code> used to identify previous donations that should not be processed.
     */
    private void processDonationAPI(boolean firstRun)
    {
        try
        {
            JsonArray donations = JSON_PARSER.parse(Helper.readUrl(new URL(firstRun ? URL + "?sort=amount" : URL), new String[]{"authorization", String.format(HEADER, APIKey)})).getAsJsonArray();
            for (JsonElement jsonElement : donations)
            {
                Donation donation = getDonation(jsonElement.getAsJsonObject());

                // Make sure we have a donation to work with and see if this is a first run
                if (donation != null && firstRun)
                {
                    // This is a first run so add to current list/done ids
                    topDonationsBasedHudEntry.add(donation);
                    doneIDs.add(donation.id);
                }
                else if (donation != null)
                {
                    // We have a donation and this is a loop check so process the donation
                    process(donation, true, this);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private Donation getDonation(JsonObject jsonObject)
    {
        try
        {
            // Attempt to parse the data we need for the donation
            String username = jsonObject.get("nickname").getAsString();
            String note = jsonObject.get("message").getAsString();
            long timestamp = ISO8601DATEFORMAT.parse(jsonObject.get("time").getAsString()).getTime();
            double amount = jsonObject.getAsJsonObject("amount").getAsJsonObject("display").get("total").getAsFloat();
            String id = jsonObject.get("_id").getAsString();

            // We have all the data we need to return the Donation object
            return new Donation(id, amount, timestamp, username, note);
        }
        catch (Exception e)
        {
            // We threw an error so just log it and move on
            e.printStackTrace();
        }

        // Something is wrong in the data so return null
        return null;
    }
}
