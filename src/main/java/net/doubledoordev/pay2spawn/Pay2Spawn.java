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

package net.doubledoordev.pay2spawn;

import com.mojang.brigadier.CommandDispatcher;
import net.doubledoordev.d3core.D3Core;
import net.doubledoordev.d3core.util.ID3Mod;
import net.doubledoordev.pay2spawn.checkers.CheckerHandler;
import net.doubledoordev.pay2spawn.client.Rendering;
import net.doubledoordev.pay2spawn.cmd.CommandP2S;
import net.doubledoordev.pay2spawn.cmd.CommandP2SPermissions;
import net.doubledoordev.pay2spawn.cmd.CommandP2SServer;
import net.doubledoordev.pay2spawn.configurator.ConfiguratorManager;
import net.doubledoordev.pay2spawn.configurator.HTMLGenerator;
import net.doubledoordev.pay2spawn.network.*;
import net.doubledoordev.pay2spawn.permissions.PermissionsHandler;
import net.doubledoordev.pay2spawn.types.TypeBase;
import net.doubledoordev.pay2spawn.types.TypeRegistry;
import net.doubledoordev.pay2spawn.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * The main mod class
 *
 * @author Dries007
 */
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Mod(MODID)
public class Pay2Spawn implements ID3Mod {
    public static final HashSet<String> playersWithValidConfig = new HashSet<>();

    public static Pay2Spawn instance;
    public static boolean enable = true;
    public static boolean forceOn = false;
    private static boolean serverHasMod = false;

    private RewardsDB rewardsDB;
    private P2SConfig config;
    private File configFolder;
    private static final Logger logger = LogManager.getLogger();
    private SimpleChannel snw;
    private boolean newConfig;
    private static final String PROTOCOL_VERSION = "1.0";

    public static RewardsDB getRewardsDB() {
        return instance.rewardsDB;
    }

    public static Logger getLogger() {
        return instance.logger;
    }

    public static P2SConfig getConfig() {
        return instance.config;
    }

    public static File getFolder() {
        return instance.configFolder;
    }

    public static SimpleChannel getSnw() {
        return instance.snw;
    }

    public static File getRewardDBFile() {
        return new File(instance.configFolder, NAME + ".json");
    }

    public static void reloadDB() {
        instance.rewardsDB = new RewardsDB(getRewardDBFile());
        ConfiguratorManager.reload();
        try {
            PermissionsHandler.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reloadDB_Server() throws Exception {
        StatusMessage.serverConfig = GSON_NOPP.toJson(JSON_PARSER.parse(new FileReader(getRewardDBFile())));
        StatusMessage.sendConfigToAllPlayers();
    }

    public static void reloadDBFromServer(String input) {
        instance.rewardsDB = new RewardsDB(input);
        ConfiguratorManager.reload();
    }

    public static boolean doesServerHaveMod() {
        return serverHasMod;
    }

    public static boolean doesPlayerHaveValidConfig(String username) {
        return playersWithValidConfig.contains(username);
    }

    public static void resetServerStatus() {
        enable = true;
        forceOn = false;
    }

/*    @NetworkCheckHandler
    public boolean networkCheckHandler(Map<String, String> data, Dist side) {
        if (side.isClient()) serverHasMod = data.containsKey(MODID);
        return !data.containsKey(MODID) || data.get(MODID).equals(metadata.version);
    }*/

    public Pay2Spawn() throws IOException {
        System.setProperty("java.awt.headless", "false");
        logger.info("Is Headless: " + GraphicsEnvironment.isHeadless());
        instance = this;
        D3Core d3Core = new D3Core();
        configFolder = new File(FMLLoader.getGamePath() + "/" + "config" + "/", NAME);
//        noinspection ResultOfMethodCallIgnored
        configFolder.mkdirs();

        File configFile = new File(configFolder, NAME + ".cfg");
        newConfig = !configFile.exists();
        config = new P2SConfig(configFile);
        MetricsHelper.init();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(d3Core::commonSetup);
        modEventBus.addListener(this::clientSetup);

        int id = 0;
        snw = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

        snw.messageBuilder(TestMessage.class, id++).encoder(TestMessage::toBytes).decoder(TestMessage::new).consumer(TestMessage::handle).add();
        snw.messageBuilder(MessageMessage.class, id++).encoder(MessageMessage::toBytes).decoder(MessageMessage::new).consumer(MessageMessage::handle).add();
//        snw.messageBuilder(NbtRequestMessage.class, id++).encoder(NbtRequestMessage::toBytes).decoder(NbtRequestMessage::new).consumer(NbtRequestMessage::handle).add();
//        snw.messageBuilder(NbtRequestMessage.class, id++).encoder(NbtRequestMessage::toBytes).decoder(NbtRequestMessage::new).consumer(NbtRequestMessage::handle).add(); //todo server
        snw.messageBuilder(RewardMessage.class, id++).encoder(RewardMessage::toBytes).decoder(RewardMessage::new).consumer(RewardMessage::handle).add();
        snw.messageBuilder(StatusMessage.class, id++).encoder(StatusMessage::toBytes).decoder(StatusMessage::new).consumer(StatusMessage::handle).add();
//        snw.messageBuilder(StatusMessage.class, id++).encoder(StatusMessage::toBytes).decoder(StatusMessage::new).consumer(StatusMessage::handle).add(); //todo server
        snw.messageBuilder(StructureImportMessage.class, id++).encoder(StructureImportMessage::toBytes).decoder(StructureImportMessage::new).consumer(StructureImportMessage::handle).add();
//        snw.messageBuilder(StructureImportMessage.class, id++).encoder(StructureImportMessage::toBytes).decoder(StructureImportMessage::new).consumer(StructureImportMessage::handle).add(); //todo server
        snw.messageBuilder(HTMLuploadMessage.class, id++).encoder(HTMLuploadMessage::toBytes).decoder(HTMLuploadMessage::new).consumer(HTMLuploadMessage::handle).add();
        snw.messageBuilder(CrashMessage.class, id++).encoder(CrashMessage::toBytes).decoder(CrashMessage::new).consumer(CrashMessage::handle).add();


        TypeRegistry.preInit();
        Statistics.preInit();

        config.syncConfig();
    }

    public void commonSetup(FMLCommonSetupEvent event) {
//        ServerTickHandler.INSTANCE.init();

        rewardsDB = new RewardsDB(getRewardDBFile());

//        CustomAI.INSTANCE.init();

        ClientTickHandler.INSTANCE.init();
//        ConnectionHandler.INSTANCE.init();

        config.syncConfig();

        for (TypeBase base : TypeRegistry.getAllTypes()) base.printHelpList(configFolder);

        TypeRegistry.registerPermissions();
        try {
            HTMLGenerator.init();
        } catch (IOException e) {
            logger.warn("Error initializing the HTMLGenerator.");
            e.printStackTrace();
        }

        if (newConfig && FMLLoader.getDist().isClient()) {
            JOptionPane pane = new JOptionPane();
            pane.setMessageType(JOptionPane.WARNING_MESSAGE);
            pane.setMessage("""
                    Please configure Pay2Spawn properly BEFORE you try launching this instance again.
                    You should provide AT LEAST your channel in the config.

                    If you need help with the configuring of your rewards, contact us!""");
            JDialog dialog = pane.createDialog("Please configure Pay2Spawn!");
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
            ServerLifecycleHooks.handleExit(1);
        }

        if (Pay2Spawn.getConfig().majorConfigVersionChange) {
            try {
                MetricsHelper.metrics.enable();
            } catch (IOException ignored) {

            }

            if (FMLLoader.getDist().isClient()) {
                JOptionPane pane = new JOptionPane();
                pane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
                pane.setMessage("""
                        Pay2Spawn has been updated to a new major version.
                        There is a new item spawning type, called 'Items' instead of 'Item'.
                        It supports multiple items at once, or picking one (weighted) random item.

                        You can also now set a name and/or lore tag in your config file, and it will be applied to all items spawned.

                        Also, the metrics has been re-enabled as it does not crash the game anymore.
                        Leave it on if you want us to continue p2s development.""");
                JDialog dialog = pane.createDialog("Some major Pay2Spawn changes");
                dialog.setAlwaysOnTop(true);
                dialog.setVisible(true);
            }
        }
        config.syncConfig();
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event) throws IOException {
        PermissionsHandler.init();
        try {
            StatusMessage.serverConfig = GSON_NOPP.toJson(JSON_PARSER.parse(new FileReader(new File(instance.configFolder, NAME + ".json"))));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onRegisterCommandEvent(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        CommandP2SPermissions.register(commandDispatcher);
        CommandP2SServer.register(commandDispatcher);
        CommandP2S.register(commandDispatcher);
    }

    public void clientSetup(FMLClientSetupEvent event) {
        CheckerHandler.init();
        new EventHandler();

        Rendering.init();
    }

//    @Override
//    public void syncConfig() {
//        config.syncConfig();
//    }

//    @Override
//    public void addConfigElements(List<IConfigElement> configElements)
//    {
//        List<IConfigElement> listsList = new ArrayList<IConfigElement>();
//
//        listsList.add(new ConfigElement(config.configuration.getCategory(Constants.MODID.toLowerCase())));
//        listsList.add(new ConfigElement(config.configuration.getCategory(Constants.FILTER_CAT.toLowerCase())));
//        listsList.add(new ConfigElement(config.configuration.getCategory(Constants.SERVER_CAT.toLowerCase())));
//        listsList.add(new ConfigElement(config.configuration.getCategory(Constants.BASECAT_TRACKERS.toLowerCase())));
//
//        configElements.add(new DummyConfigElement.DummyCategoryElement(MODID, "d3.pay2spawn.config.pay2spawn", listsList));
//    }
}
