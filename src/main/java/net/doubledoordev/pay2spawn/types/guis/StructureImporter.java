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

package net.doubledoordev.pay2spawn.types.guis;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.doubledoordev.pay2spawn.Pay2Spawn;
import net.doubledoordev.pay2spawn.network.StructureImportMessage;
import net.doubledoordev.pay2spawn.util.Helper;
import net.doubledoordev.pay2spawn.util.shapes.IShape;
import net.doubledoordev.pay2spawn.util.shapes.PointI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

/**
 * @author Dries007
 */
public class StructureImporter {
    final StructureImporter instance = this;
    final HashSet<PointI> points = new HashSet<>();
    final HashSet<IShape> selection = new HashSet<>();
    private final StructureTypeGui callback;
    private final JDialog dialog;
    public JPanel panel1;
    public JList<String> pointList;
    public JLabel helpText;
    public JComboBox<Mode> modeComboBox;
    public JButton addFromSelectionButton;
    public JButton removeFromSelectionButton;
    public JCheckBox renderSelectionOnlyCheckBox;
    public JButton clearSelectionButton;
    public JButton importButton;
    public JCheckBox disableAlreadyImportedShapesCheckBox;
    PointI[] tempPointsArray = points.toArray(new PointI[points.size()]);
    Mode mode = Mode.SINGLE;
    PointI p1; // For BOX mode
    PointI p2; // For BOX mode

    public StructureImporter(final StructureTypeGui callback) {
        this.callback = callback;

        modeComboBox.setModel(new DefaultComboBoxModel<>(Mode.values()));
        pointList.setModel(new AbstractListModel<String>() {
            @Override
            public int getSize() {
                tempPointsArray = points.toArray(new PointI[points.size()]);
                return tempPointsArray.length;
            }

            @Override
            public String getElementAt(int index) {
                tempPointsArray = points.toArray(new PointI[points.size()]);
                return tempPointsArray[index].toString();
            }
        });
        modeComboBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mode = (Mode) modeComboBox.getSelectedItem();
                helpText.setText(mode.helpText);
            }
        });
        addFromSelectionButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (points) {
                    synchronized (selection) {
                        for (IShape shape : selection) points.addAll(shape.getPoints());
                        if (p1 != null && p2 != null) {
                            int minX = Math.min(p1.getX(), p2.getX());
                            int minY = Math.min(p1.getY(), p2.getY());
                            int minZ = Math.min(p1.getZ(), p2.getZ());
                            int diffX = Math.max(p1.getX(), p2.getX()) - minX;
                            int diffY = Math.max(p1.getY(), p2.getY()) - minY;
                            int diffZ = Math.max(p1.getZ(), p2.getZ()) - minZ;

                            for (int x = 0; x <= diffX; x++) {
                                for (int y = 0; y <= diffY; y++) {
                                    for (int z = 0; z <= diffZ; z++) {
                                        PointI p = new PointI(minX + x, minY + y, minZ + z);
                                        points.add(p);
                                    }
                                }
                            }
                        }
                        p1 = null;
                        p2 = null;
                        selection.clear();
                    }
                }
                pointList.updateUI();
            }
        });
        removeFromSelectionButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (points) {
                    synchronized (selection) {
                        for (IShape shape : selection) points.removeAll(shape.getPoints());
                        if (p1 != null && p2 != null) {
                            int minX = Math.min(p1.getX(), p2.getX());
                            int minY = Math.min(p1.getY(), p2.getY());
                            int minZ = Math.min(p1.getZ(), p2.getZ());
                            int diffX = Math.max(p1.getX(), p2.getX()) - minX;
                            int diffY = Math.max(p1.getY(), p2.getY()) - minY;
                            int diffZ = Math.max(p1.getZ(), p2.getZ()) - minZ;

                            for (int x = 0; x <= diffX; x++) {
                                for (int y = 0; y <= diffY; y++) {
                                    for (int z = 0; z <= diffZ; z++) {
                                        PointI p = new PointI(minX + x, minY + y, minZ + z);
                                        points.remove(p);
                                    }
                                }
                            }
                        }
                        p1 = null;
                        p2 = null;
                        selection.clear();
                    }
                }
                pointList.updateUI();
            }
        });
        clearSelectionButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (selection) {
                    selection.clear();
                }
                pointList.updateUI();
                updateBtns();
            }
        });
        importButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (FMLEnvironment.dist.isClient()) {
                    if (Minecraft.getInstance().player != null) {
                        CompoundTag root = new CompoundTag();
                        root.putInt("x", -Helper.round(Minecraft.getInstance().player.getX()));
                        root.putInt("y", -Helper.round(Minecraft.getInstance().player.getY()));
                        root.putInt("z", -Helper.round(Minecraft.getInstance().player.getZ()));
                        ListTag list = new ListTag();
                        synchronized (points) {
                            for (PointI point : points) list.add(point.toNBT());
                        }
                        root.put("list", list);
                        if (Helper.checkTooBigForNetwork(root)) return;
                        Pay2Spawn.getSnw().sendToServer(new StructureImportMessage(root));
                        dialog.dispose();
                    }
                }
            }
        });
        disableAlreadyImportedShapesCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                callback.disabled = disableAlreadyImportedShapesCheckBox.isSelected();
            }
        });

        dialog = new JDialog();
        dialog.setContentPane(panel1);
        dialog.setModal(true);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setTitle("Structure importer");
        dialog.setPreferredSize(new Dimension(600, 750));
        dialog.setSize(400, 750);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        new ForgeEventbusDialogThing(dialog, this);
        helpText.setText(mode.helpText);
        dialog.pack();
        dialog.setVisible(true);

        updateBtns();
    }

    @SubscribeEvent
    public void renderEvent(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            if (selection.size() == 0 && points.size() == 0 && p1 == null && p2 == null)
                return;
            event.getPoseStack().pushPose();
            var stack = event.getPoseStack();
            Tesselator tess = Tesselator.getInstance();
            BufferBuilder buffer = tess.getBuilder();
//        GL11.glPushMatrix();
//        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderSystem.disableDepthTest();
            RenderSystem.disableTexture();

//        event.getPoseStack().translate(-RenderManager.renderPosX, -RenderManager.renderPosY, 1 - RenderManager.renderPosZ);
//        GL11.glScalef(1.0F, 1.0F, 1.0F);

            if (!renderSelectionOnlyCheckBox.isSelected()) {
                synchronized (points) {
                    RenderSystem.lineWidth(1);
                    RenderSystem.setShaderColor(0, 1, 0, 1);
                    for (PointI point : points) {
                        point.render(stack, tess, buffer);
                    }
                }
            }

            synchronized (selection) {
                RenderSystem.lineWidth(2);
                RenderSystem.setShaderColor(1, 0, 0, 1);
                for (IShape point : selection) {
                    point.render(stack, tess, buffer);
                }
            }

            if (pointList.getSelectedIndex() != -1 && tempPointsArray.length < pointList.getSelectedIndex()) {
                RenderSystem.setShaderColor(0, 0, 1, 1);
                tempPointsArray[pointList.getSelectedIndex()].render(stack, tess, buffer);
            }

            if (mode == Mode.BOX && p1 != null) {
                Helper.renderPoint(stack, tess, p1, buffer, 246.0 / 255.0, 59.0 / 255.0, 246.0 / 255.0);
            }

            if (mode == Mode.BOX && p2 != null) {
                Helper.renderPoint(stack, tess, p2, buffer, 59.0 / 243.0, 243.0 / 255.0, 246.0 / 255.0);
            }

//        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderSystem.enableDepthTest();
            RenderSystem.enableTexture();
            // tess.renderingWorldRenderer = true;
//        GL11.glPopMatrix();
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public void clickEvent(PlayerInteractEvent e) {
        if (e.getEntity().getMainHandItem() == null || e.getEntity().getMainHandItem().getItem() != Items.STICK)
            return;

//        e.setCanceled(true);


        if (e instanceof PlayerInteractEvent.LeftClickBlock)
            click(Click.LEFT, e.getPos().getX(), e.getPos().getY(), e.getPos().getZ());
        else if (e instanceof PlayerInteractEvent.RightClickBlock)
            click(Click.RIGHT, e.getPos().getX(), e.getPos().getY(), e.getPos().getZ());
    }

    private void click(Click click, int x, int y, int z) {
        switch (mode) {
            case SINGLE -> {
                synchronized (selection) {
                    if (click == Click.LEFT) selection.remove(new PointI(x, y, z));
                    if (click == Click.RIGHT) selection.add(new PointI(x, y, z));
                }
            }
            case BOX -> {
                synchronized (selection) {
                    if (click == Click.LEFT) p1 = new PointI(x, y, z);
                    if (click == Click.RIGHT) p2 = new PointI(x, y, z);
                }
            }
        }
        updateBtns();
    }

    private void updateBtns() {
        addFromSelectionButton.setEnabled(selection.size() != 0 || (p1 != null && p2 != null));
        removeFromSelectionButton.setEnabled(selection.size() != 0 || (p1 != null && p2 != null));
        clearSelectionButton.setEnabled(selection.size() != 0 || (p1 != null && p2 != null));
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        final JScrollPane scrollPane1 = new JScrollPane();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(scrollPane1, gbc);
        pointList = new JList();
        scrollPane1.setViewportView(pointList);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(panel2, gbc);
        modeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        modeComboBox.setModel(defaultComboBoxModel1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(modeComboBox, gbc);
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(0);
        label1.setHorizontalTextPosition(0);
        label1.setText("A stick is used as the \"wand\" for this!");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.ipadx = 5;
        gbc.ipady = 5;
        panel2.add(label1, gbc);
        helpText = new JLabel();
        helpText.setHorizontalAlignment(0);
        helpText.setHorizontalTextPosition(0);
        helpText.setText("HELP TEXT");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.ipadx = 5;
        gbc.ipady = 5;
        panel1.add(helpText, gbc);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(panel3, gbc);
        addFromSelectionButton = new JButton();
        addFromSelectionButton.setEnabled(false);
        addFromSelectionButton.setText("Add from selection");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(addFromSelectionButton, gbc);
        removeFromSelectionButton = new JButton();
        removeFromSelectionButton.setEnabled(false);
        removeFromSelectionButton.setText("Remove from selection");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(removeFromSelectionButton, gbc);
        renderSelectionOnlyCheckBox = new JCheckBox();
        renderSelectionOnlyCheckBox.setText("Render selection only");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel3.add(renderSelectionOnlyCheckBox, gbc);
        clearSelectionButton = new JButton();
        clearSelectionButton.setEnabled(false);
        clearSelectionButton.setText("Clear selection");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(clearSelectionButton, gbc);
        disableAlreadyImportedShapesCheckBox = new JCheckBox();
        disableAlreadyImportedShapesCheckBox.setText("Disable already imported shapes ");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel3.add(disableAlreadyImportedShapesCheckBox, gbc);
        importButton = new JButton();
        importButton.setText("Import relative to player!");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel1.add(importButton, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    enum Mode {
        SINGLE("Single block mode", "Right click => add block, Left click => remove block"),
        BOX("Box mode", "Right click => Point 1, Left click => Point 2");

        public final String name;
        public final String helpText;

        Mode(String name, String helpText) {
            this.name = name;
            this.helpText = helpText;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    enum Click {
        LEFT, RIGHT
    }
}
