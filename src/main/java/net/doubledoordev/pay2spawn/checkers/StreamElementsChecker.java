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
import com.google.gson.JsonParser;
import net.doubledoordev.oldforge.Configuration;
import net.doubledoordev.pay2spawn.hud.DonationsBasedHudEntry;
import net.doubledoordev.pay2spawn.util.Donation;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;

import static net.doubledoordev.pay2spawn.util.Constants.BASECAT_TRACKERS;

/**
 * For donation-tracker.com
 *
 * @author Dries007
 */
public class StreamElementsChecker extends AbstractChecker implements Runnable {

    public final static StreamElementsChecker INSTANCE = new StreamElementsChecker();
    public final static String NAME = "streamelements";
    public final static String CAT = BASECAT_TRACKERS + '.' + NAME;
    public String URL = "https://api.streamelements.com/kappa/v2/tips/";
    private static String JWT_TOKEN = "";
    private static String accountId = "";

    DonationsBasedHudEntry topDonationsBasedHudEntry, recentDonationsBasedHudEntry;

    boolean enabled = false;
    int interval = 20;

    private StreamElementsChecker() {
        super();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init() {
        new Thread(this, getName()).start();
    }

    @Override
    public boolean enabled() {
        return enabled && !JWT_TOKEN.isEmpty();
    }

    @Override
    public void doConfig(Configuration configuration) {
        configuration.addCustomCategoryComment(CAT, "This is the checker for streamelements.com");

        enabled = configuration.get(CAT, "enabled", enabled).getBoolean(enabled);

        JWT_TOKEN = configuration.get(CAT, "APIKey", JWT_TOKEN).getString();
        accountId = configuration.get(CAT, "accountId", accountId).getString();
        interval = configuration.get(CAT, "interval", interval, "The time in between polls minimum 20 (in seconds).").getInt();
        min_donation = configuration.get(CAT, "min_donation", min_donation, "Donations below this amount will only be added to statistics and will not spawn rewards").getDouble();
        URL = configuration.get(CAT, "url", URL, "Donation Tracker API end point string").getString();

        topDonationsBasedHudEntry = new DonationsBasedHudEntry("top" + NAME + ".txt", CAT + ".topDonations", -1, 1, 5, "$name: $$amount", "-- Top donations --", CheckerHandler.AMOUNT_DONATION_COMPARATOR);
        recentDonationsBasedHudEntry = new DonationsBasedHudEntry("recent" + NAME + ".txt", CAT + ".recentDonations", -1, 2, 5, "$name: $$amount", "-- Recent donations --", CheckerHandler.RECENT_DONATION_COMPARATOR);

        // Donation tracker doesn't allow a poll interval faster than 20 seconds
        // They will IP ban anyone using a time below 20 so force the value to be safe
        if (interval < 20) {
            interval = 20;
            // Now force the config setting to 20
            configuration.get(CAT, "interval", "The time in between polls minimum 5 (in seconds).").set(interval);
        }
    }

    @Override
    public DonationsBasedHudEntry[] getDonationsBasedHudEntries() {
        return new DonationsBasedHudEntry[]{topDonationsBasedHudEntry, recentDonationsBasedHudEntry};
    }

    @Override
    public void run() {
        // Process any current donations from the API
        processDonationAPI(true);

        // Start the processing loop
        while (true) {
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
    private void processDonationAPI(boolean firstRun) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL + accountId + "?limit=1"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + JWT_TOKEN)
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
//            Pay2Spawn.getLogger().info(root);

            JsonArray donations = root.getAsJsonArray("docs");
            for (JsonElement jsonElement : donations) {
                Donation donation = getDonation(jsonElement.getAsJsonObject());

                // Make sure we have a donation to work with and see if this is a first run
                if (donation != null && firstRun == true) {
                    // This is a first run so add to current list/done ids
                    topDonationsBasedHudEntry.add(donation);
                    recentDonationsBasedHudEntry.add(donation);
                    doneIDs.add(donation.id);
                } else if (donation != null) {
                    // We have a donation and this is a loop check so process the donation
                    process(donation, true, this);
                }
            }

        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Donation getDonation(JsonObject jsonObject) {
        try {
            // Attempt to parse the data we need for the donation
            String username = jsonObject.get("donation").getAsJsonObject().get("user").getAsJsonObject().get("username").getAsString();
            String note = jsonObject.get("donation").getAsJsonObject().has("message") ? jsonObject.get("donation").getAsJsonObject().get("message").getAsString() : "";

            SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            long timestamp = SIMPLE_DATE_FORMAT.parse(jsonObject.get("createdAt").getAsString()).getTime();
            double amount = jsonObject.get("donation").getAsJsonObject().get("amount").getAsDouble();
            String id = jsonObject.get("_id").getAsString();

            // We have all the data we need to return the Donation object
            return new Donation(id, amount, timestamp, username, note);
        } catch (Exception e) {
            // We threw an error so just log it and move on
            e.printStackTrace();
        }

        // Something is wrong in the data so return null
        return null;
    }
}