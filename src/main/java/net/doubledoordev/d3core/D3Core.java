/*
 * Copyright (c) 2014,
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
 *  Neither the name of the {organization} nor the names of its
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
 *
 *
 */

package net.doubledoordev.d3core;

import net.doubledoordev.d3core.permissions.PermissionsDB;
import net.doubledoordev.d3core.util.*;
import net.doubledoordev.d3core.util.libs.org.mcstats.Metrics;
import net.doubledoordev.oldforge.Configuration;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static net.doubledoordev.d3core.util.CoreConstants.*;
import static net.doubledoordev.d3core.util.FMLEventHandler.FML_EVENT_HANDLER;
import static net.doubledoordev.d3core.util.ForgeEventHandler.FORGE_EVENT_HANDLER;
import static net.doubledoordev.d3core.util.VoidRefunds.VOID_REFUNDS;

/**
 * @author Dries007
 */
//todo seperate mods @Mod(MODID)
public class D3Core implements ID3Mod {
    public static D3Core instance;
    public static boolean aprilFools = true;
    private File folder;

    private static final Logger logger = LogManager.getLogger();
    private Configuration configuration;

    private boolean debug = false;
    private boolean sillyness = true;
    private boolean updateWarning = true;

    private List<ModContainer> d3Mods = new ArrayList<>();
    private List<CoreHelper.ModUpdateDate> updateDateList = new ArrayList<>();
    private boolean pastPost;

    public D3Core() {
        instance = this;

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(FML_EVENT_HANDLER);
        MinecraftForge.EVENT_BUS.register(VOID_REFUNDS);
        MinecraftForge.EVENT_BUS.register(FORGE_EVENT_HANDLER);
        MinecraftForge.EVENT_BUS.register(VOID_REFUNDS);

        folder = new File(FMLLoader.getGamePath() + "/" + "config" + "/", MODID);
        folder.mkdir();
        configuration = new Configuration(new File(folder, "config.cfg"));
//        syncConfig();

        PermissionsDB.load();
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        Materials.load();

        try {
            Metrics metrics = new Metrics(MODID, "1.18.2"); //todo mod version

            Metrics.Graph submods = metrics.createGraph("Submods");
            for (ModContainer modContainer : d3Mods) {
                submods.addPlotter(new Metrics.Plotter(modContainer.getModId()) {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            }

            for (ModContainer modContainer : d3Mods) {
                metrics.createGraph(modContainer.getModId()).addPlotter(new Metrics.Plotter("Version") { //todo idk what to put here lmao
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            }

            metrics.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        PermissionsDB.save();
    }

    
 /*   @Override
    public void syncConfig()
    {
        configuration.setCategoryLanguageKey(MODID, "d3.core.config.core").setCategoryComment(MODID, LanguageRegistry.instance().getStringLocalization("d3.core.config.core"));

        debug = configuration.getBoolean("debug", MODID, debug, "Enable debug mode", "d3.core.config.debug");
        sillyness = configuration.getBoolean("sillyness", MODID, sillyness, "Enable sillyness\nBut seriously, you can disable name changes, drops and block helmets with this setting.", "d3.core.config.sillyness");
        updateWarning = configuration.getBoolean("updateWarning", MODID, updateWarning, "Allow update warnings on login", "d3.core.config.updateWarning");
        FML_EVENT_HANDLER.norain = configuration.getBoolean("norain", MODID, FML_EVENT_HANDLER.norain, "No more rain if set to true.", "d3.core.config.norain");
        FML_EVENT_HANDLER.insomnia = configuration.getBoolean("insomnia", MODID, FML_EVENT_HANDLER.insomnia, "No more daytime when players sleep if set to true.", "d3.core.config.insomnia");
        FML_EVENT_HANDLER.lilypad = configuration.getBoolean("lilypad", MODID, FML_EVENT_HANDLER.lilypad, "Spawn the player on a lilypad when in or above water.", "d3.core.config.lilypad");
        FORGE_EVENT_HANDLER.nosleep = configuration.getBoolean("nosleep", MODID, FORGE_EVENT_HANDLER.nosleep, "No sleep at all", "d3.core.config.nosleep");
        FORGE_EVENT_HANDLER.printDeathCoords = configuration.getBoolean("printDeathCoords", MODID, FORGE_EVENT_HANDLER.printDeathCoords, "Print your death coordinates in chat (client side)", "d3.core.config.printDeathCoords");
        FORGE_EVENT_HANDLER.claysTortureMode = configuration.getBoolean("claysTortureMode", MODID, FORGE_EVENT_HANDLER.claysTortureMode, "Deletes all drops on death.", "d3.core.config.claystorturemode");
        aprilFools = configuration.getBoolean("aprilFools", MODID, aprilFools, "What would this do...");
        getDevPerks().update(sillyness);

        final String catTooltips = MODID + ".tooltips";
        configuration.setCategoryLanguageKey(catTooltips, "d3.core.config.tooltips").addCustomCategoryComment(catTooltips, LanguageRegistry.instance().getStringLocalization("d3.core.config.tooltips"));

        FORGE_EVENT_HANDLER.enableStringID = configuration.getBoolean("enableStringID", catTooltips, true, "Example: minecraft:gold_ore", "d3.core.config.tooltips.enableStringID");
        FORGE_EVENT_HANDLER.enableUnlocalizedName = configuration.getBoolean("enableUnlocalizedName", catTooltips, true, "Example: tile.oreGold", "d3.core.config.tooltips.enableUnlocalizedName");
        FORGE_EVENT_HANDLER.enableOreDictionary = configuration.getBoolean("enableOreDictionary", catTooltips, true, "Example: oreGold", "d3.core.config.tooltips.enableOreDictionary");
        FORGE_EVENT_HANDLER.enableBurnTime = configuration.getBoolean("enableBurnTime", catTooltips, true, "Example: 300 ticks", "d3.core.config.tooltips.enableBurnTime");

        {
            final String catEnderGriefing = MODID + ".EndermanGriefing";
            configuration.setCategoryLanguageKey(catEnderGriefing, "d3.core.config.EndermanGriefing");

            EndermanGriefing.undo();

            EndermanGriefing.disable = configuration.getBoolean("disable", catEnderGriefing, false, "Disable Enderman griefing completely.", "d3.core.config.EndermanGriefing.disable");
            EndermanGriefing.dropCarrying = configuration.getBoolean("dropCarrying", catEnderGriefing, false, "Made Enderman drop there carrying block on death.", "d3.core.config.EndermanGriefing.dropCarrying");

            Property property = configuration.get(catEnderGriefing, "blacklist", new String[0], "List of blocks (minecraft:stone) that will never be allowed to be picked up.");
            property.setLanguageKey("d3.core.config.EndermanGriefing.blacklist");
            EndermanGriefing.blacklist = property.getStringList();

            property = configuration.get(catEnderGriefing, "addlist", new String[0], "List of blocks (minecraft:stone) that will be added to the list of blocks Enderman pick up.");
            property.setLanguageKey("d3.core.config.EndermanGriefing.addlist");
            EndermanGriefing.addList = property.getStringList();

            if (pastPost) EndermanGriefing.init();
        }

        VOID_REFUNDS.config(configuration);

        if (configuration.hasChanged()) configuration.save();
    }*/


    public static Logger getLogger() {
        return instance.logger;
    }

    public static boolean debug() {
        return instance.debug;
    }

    public static Configuration getConfiguration() {
        return instance.configuration;
    }

    public static File getFolder() {
        return instance.folder;
    }
}
