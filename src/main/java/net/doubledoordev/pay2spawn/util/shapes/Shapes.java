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

package net.doubledoordev.pay2spawn.util.shapes;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * For structure spawning things
 *
 * @author Dries007
 */
public class Shapes {
    public static final String SHAPE_KEY = "shape";
    public static final HashMap<String, IShape> MAP = new HashMap<>();
    public static final ArrayList<String> LIST = new ArrayList<>();

    static {
        register(new Box());
        register(new Cylinder());
        register(new Pillar());
        register(new PointI());
        register(new Sphere());
    }

    private static void register(IShape shape) {
        LIST.add(shape.getClass().getSimpleName());
        MAP.put(shape.getClass().getSimpleName(), shape);
    }

    public static IShape loadShape(CompoundTag compound) {
        return MAP.get(compound.getString(SHAPE_KEY)).fromNBT(compound);
    }

    public static CompoundTag addShapeType(CompoundTag shapeData, Class<? extends IShape> clazz) {
        shapeData.putString(SHAPE_KEY, clazz.getSimpleName());
        return shapeData;
    }

    public static CompoundTag storeShape(IShape shape) {
        CompoundTag compound = shape.toNBT();
        compound.putString(SHAPE_KEY, shape.getClass().getSimpleName());
        return compound;
    }
}
