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

package net.doubledoordev.d3core.util;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * @author Dries007
 */
public class ForgeEventHandler {
    public static final ForgeEventHandler FORGE_EVENT_HANDLER = new ForgeEventHandler();
    public boolean enableStringID;
    public boolean enableUnlocalizedName;
    public boolean enableOreDictionary;
    public boolean enableBurnTime;
    public boolean nosleep;
    public boolean printDeathCoords = true;
    public boolean claysTortureMode;

    private ForgeEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void itemTooltipEventHandler(ItemTooltipEvent event) {
//     todo   if (event.showAdvancedItemTooltips)
//        {
//            if (enableStringID) event.getToolTip().add(ChatFormatting.DARK_AQUA + GameData.getItemRegistry().getNameForObject(event.getItemStack().getItem()));
        if (enableUnlocalizedName)
            event.getToolTip().add(new TextComponent(ChatFormatting.DARK_GREEN + event.getItemStack().getDescriptionId()));
        if (enableBurnTime && FurnaceBlockEntity.isFuel(event.getItemStack()))
            event.getToolTip().add(Component.nullToEmpty(ChatFormatting.GOLD + "Burns for " + FurnaceBlockEntity.isFuel(event.getItemStack()) + " ticks"));
//        }
    }

    @SubscribeEvent()
    public void entityDeathEvent(LivingDropsEvent event) {
        if (event.getEntityLiving() instanceof Player && claysTortureMode) {
            event.setCanceled(true);
        } else if (event.getEntityLiving() instanceof EnderMan && EndermanGriefing.dropCarrying) {
            EnderMan entityEnderman = ((EnderMan) event.getEntityLiving());
//            todo if (entityEnderman.getCarriedBlock().getBlock() != Blocks.AIR) {
//                ItemStack stack = new ItemStack(entityEnderman.getCarriedBlock().getBlock(), 1, entityEnderman.getCarryingData());
//                event.getDrops().add(new ItemEntity(entityEnderman.level, entityEnderman.getX(), entityEnderman.getY(), entityEnderman.getZ(), stack));
//            }
        }
    }

    @SubscribeEvent()
    public void playerDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof Player && printDeathCoords) {
            TextComponent posText = new TextComponent("X: " + Mth.floor(event.getEntityLiving().getX()) + " Y: " + Mth.floor(event.getEntityLiving().getY() + 0.5d) + " Z: " + Mth.floor(event.getEntityLiving().getZ()));
            /*todo try {
                if (!event.getEntityLiving().createCommandSourceStack().hasPermission(2))//todo check .getCommandManager().getPossibleCommands((CommandSourceStack) event.getEntityLiving(), "tp").isEmpty())
                {
                    posText.withStyle(new Style().setItalic(true)
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to teleport!")))
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + event.getEntityLiving().getX() + " " + (event.getEntityLiving().getY() + 0.5d) + " " + event.getEntityLiving().getZ())));
                }
            } catch (Exception ignored) {

            }*/

            ((Player) event.getEntityLiving()).displayClientMessage(new TextComponent("You died at ").setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)).append(posText), false);
        }
    }

    @SubscribeEvent()
    public void sleepEvent(PlayerSleepInBedEvent event) {
        if (nosleep || CoreConstants.isAprilFools()) {
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @SubscribeEvent
    public void aprilFools(ServerChatEvent event) {
        if (CoreConstants.isAprilFools()) {
            Style style = event.getComponent().getStyle();
            float chance = 0.25f;
            if (CoreConstants.RANDOM.nextFloat() < chance) {
                style.withBold(true);
                chance *= chance;
            }
            if (CoreConstants.RANDOM.nextFloat() < chance) {
                style.withItalic(true);
                chance *= chance;
            }
            if (CoreConstants.RANDOM.nextFloat() < chance) {
                style.withUnderlined(true);
                chance *= chance;
            }
            if (CoreConstants.RANDOM.nextFloat() < chance) {
                style.withStrikethrough(true);
                chance *= chance;
            }
            if (CoreConstants.RANDOM.nextFloat() < chance) {
                style.withObfuscated(true);
            }
            style.withColor(ChatFormatting.values()[CoreConstants.RANDOM.nextInt(ChatFormatting.values().length)]);
            event.getComponent().toFlatList(style);
        }
    }

    @SubscribeEvent
    public void aprilFools(PlayerEvent.NameFormat event) {
        if (CoreConstants.isAprilFools()) {
            event.setDisplayname(Component.nullToEmpty("§k" + event.getDisplayname())); //todo §k
        }
    }
}
