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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.doubledoordev.pay2spawn.random.RandomRegistry;
import net.minecraft.nbt.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Map;

import static net.doubledoordev.pay2spawn.types.StructureType.BLOCKID_KEY;
import static net.doubledoordev.pay2spawn.util.Constants.*;

/**
 * This is nearly full Json (gson) to NBT converter.
 * Not working:
 * NBT IntArrays and ByteArrays to Json.
 * Json null to NBT
 *
 * @author Dries007
 */
public class JsonNBTHelper {
    /**
     * To avoid idiocy later we need to store all things as a string with the type in the string. :(
     * Please tell your users about this!
     *
     * @see net.doubledoordev.pay2spawn.util.JsonNBTHelper#parseJSON(com.google.gson.JsonPrimitive)
     */
    public static JsonElement parseNBT(Tag element) {
        return switch (element.getId()) {
            // 0 = END
            case BYTE -> new JsonPrimitive(NBTTypes[element.getId()] + ":" + ((ByteTag) element).getAsByte());
            case SHORT -> new JsonPrimitive(NBTTypes[element.getId()] + ":" + ((ShortTag) element).getAsShort());
            case INT -> new JsonPrimitive(NBTTypes[element.getId()] + ":" + ((IntTag) element).getAsInt());
            case LONG -> new JsonPrimitive(NBTTypes[element.getId()] + ":" + ((LongTag) element).getAsLong());
            case FLOAT -> new JsonPrimitive(NBTTypes[element.getId()] + ":" + ((FloatTag) element).getAsFloat());
            case DOUBLE -> new JsonPrimitive(NBTTypes[element.getId()] + ":" + ((DoubleTag) element).getAsDouble());
            case BYTE_ARRAY -> parseNBT((ByteArrayTag) element);
            case STRING -> new JsonPrimitive(NBTTypes[element.getId()] + ":" + ((StringTag) element).getAsString());
            case LIST -> parseNBT((ListTag) element);
            case COMPOUND -> parseNBT((CompoundTag) element);
            case INT_ARRAY -> parseNBT((IntArrayTag) element);
            default -> null;
        };
    }

    public static JsonPrimitive parseNBT(IntArrayTag nbtArray) {
        JsonArray jsonArray = new JsonArray();
        for (int i : nbtArray.getAsIntArray()) jsonArray.add(new JsonPrimitive(i));
        return new JsonPrimitive(NBTTypes[nbtArray.getId()] + ":" + jsonArray.toString());
    }

    public static JsonPrimitive parseNBT(ByteArrayTag nbtArray) {
        JsonArray jsonArray = new JsonArray();
        for (int i : nbtArray.getAsByteArray()) jsonArray.add(new JsonPrimitive(i));
        return new JsonPrimitive(NBTTypes[nbtArray.getId()] + jsonArray.toString());
    }

    public static JsonArray parseNBT(ListTag nbtArray) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < nbtArray.size(); i++) {
            switch (nbtArray.func_150303_d) {
                case 5 -> jsonArray.add(parseNBT(FloatTag.valueOf(nbtArray.getFloat(i))));
                case 6 -> jsonArray.add(parseNBT(DoubleTag.valueOf(nbtArray.getDouble(i))));
                case 8 -> jsonArray.add(parseNBT(StringTag.valueOf(nbtArray.getString(i))));
                case 10 -> jsonArray.add(parseNBT(nbtArray.getCompound(i)));
                case 11 -> jsonArray.add(parseNBT(new IntArrayTag(nbtArray.getIntArray(i))));
            }

        }
        return jsonArray;
    }

    public static JsonObject parseNBT(CompoundTag compound) {
        boolean isItemStack = isItemStack(compound);
        JsonObject jsonObject = new JsonObject();
        for (Object object : compound.getAllKeys()) {
            if (object.equals("id") && isItemStack) // Itemstack?
            {
                int id = compound.getShort("id");
                Item item = GameData.getItemRegistry().getObjectById(id);
                jsonObject.addProperty("id", NBTTypes[compound.get("id").getId()] + ":" + (item == GameData.getItemRegistry().getDefaultValue() ? id : GameData.getItemRegistry().getNameForObject(item)));
            } else if (object.equals(BLOCKID_KEY)) // Block?
            {
                int id = compound.getInt(BLOCKID_KEY);
                Block block = GameData.getBlockRegistry().getObjectById(id);
                jsonObject.addProperty(BLOCKID_KEY, NBTTypes[compound.get(BLOCKID_KEY).getId()] + ":" + (block == GameData.getBlockRegistry().getDefaultValue() ? id : GameData.getBlockRegistry().getNameForObject(block)));
            } else {
                jsonObject.add(object.toString(), parseNBT(compound.get(object.toString())));
            }
        }
        return jsonObject;
    }

    public static boolean isItemStack(CompoundTag compound) {
        try {
            return ItemStack.of(compound) != null;
        } catch (Exception ignored) {

        }
        return false;
    }

    public static Tag parseJSON(JsonElement element) {
        if (element.isJsonObject()) return parseJSON(element.getAsJsonObject());
        else if (element.isJsonArray()) return parseJSON(element.getAsJsonArray());
        else if (element.isJsonPrimitive()) return parseJSON(element.getAsJsonPrimitive());

        return null;
    }

    /**
     * There is no way to detect number types and NBT is picky about this. Lets hope the original type id is there, otherwise we are royally screwed.
     */
    public static Tag parseJSON(JsonPrimitive element) {
        String string = element.getAsString();
        if (string.contains(":")) {
            for (int id = 0; id < NBTTypes.length; id++) {
                if (string.startsWith(NBTTypes[id] + ":")) {
                    String value = string.replace(NBTTypes[id] + ":", "");
                    value = RandomRegistry.solveRandom(id, value);
                    switch (id) {
                        // 0 = END
                        case BYTE:
                            //return new ByteTag(Byte.parseByte(value));
                        case SHORT:
                            //return new ShortTag(Short.parseShort(value));
                        case INT:
                            return IntTag.valueOf(Integer.parseInt(value));
                        case LONG:
                            return LongTag.valueOf(Long.parseLong(value));
                        case FLOAT:
                            return FloatTag.valueOf(Float.parseFloat(value));
                        case DOUBLE:
                            return DoubleTag.valueOf(Double.parseDouble(value));
                        case BYTE_ARRAY:
                            return parseJSONByteArray(value);
                        case STRING:
                            return StringTag.valueOf(value);
                        // 9 = LIST != JsonPrimitive
                        // 10 = COMPOUND != JsonPrimitive
                        case INT_ARRAY:
                            return parseJSONIntArray(value);
                    }
                }
            }
        }

        // Now it becomes guesswork.
        if (element.isString()) return StringTag.valueOf(string);
        if (element.isBoolean()) return ByteTag.valueOf((byte) (element.getAsBoolean() ? 1 : 0));

        Number n = element.getAsNumber();
        if (n instanceof Byte) return ByteTag.valueOf(n.byteValue());
        if (n instanceof Short) return ShortTag.valueOf(n.shortValue());
        if (n instanceof Integer) return IntTag.valueOf(n.intValue());
        if (n instanceof Long) return LongTag.valueOf(n.longValue());
        if (n instanceof Float) return FloatTag.valueOf(n.floatValue());
        if (n instanceof Double) return DoubleTag.valueOf(n.doubleValue());

        try {
            return IntTag.valueOf(Integer.parseInt(element.toString()));
        } catch (NumberFormatException ignored) {

        }
        throw new NumberFormatException(element.getAsNumber() + " is was not able to be parsed.");
    }

    public static ByteArrayTag parseJSONByteArray(String value) {
        JsonArray in = JSON_PARSER.parse(value).getAsJsonArray();
        byte[] out = new byte[in.size()];
        for (int i = 0; i < in.size(); i++) out[i] = in.get(i).getAsByte();
        return new ByteArrayTag(out);
    }

    public static IntArrayTag parseJSONIntArray(String value) {
        JsonArray in = JSON_PARSER.parse(value).getAsJsonArray();
        int[] out = new int[in.size()];
        for (int i = 0; i < in.size(); i++) out[i] = in.get(i).getAsInt();
        return new IntArrayTag(out);
    }

    public static CompoundTag parseJSON(JsonObject data) {
        CompoundTag root = new CompoundTag();
        for (Map.Entry<String, JsonElement> entry : data.entrySet())
            root.put(entry.getKey(), parseJSON(entry.getValue()));
        return root;
    }

    public static ListTag parseJSON(JsonArray data) {
        ListTag list = new ListTag();
        for (JsonElement element : data) list.add(parseJSON(element));
        return list;
    }

    public static JsonElement cloneJSON(JsonElement toClone) {
        return JSON_PARSER.parse(toClone.toString());
    }

    public static JsonElement fixNulls(JsonElement element) {
        if (element.isJsonNull()) return new JsonPrimitive("");
        if (element.isJsonObject()) return fixNulls(element.getAsJsonObject());
        if (element.isJsonArray()) return fixNulls(element.getAsJsonArray());
        if (element.isJsonPrimitive()) return fixNulls(element.getAsJsonPrimitive());
        return null;
    }

    public static JsonPrimitive fixNulls(JsonPrimitive primitive) {
        if (primitive.isBoolean()) return new JsonPrimitive(primitive.getAsBoolean());
        if (primitive.isNumber()) return new JsonPrimitive(primitive.getAsNumber());
        if (primitive.isString()) return new JsonPrimitive(primitive.getAsString());
        return JSON_PARSER.parse(primitive.toString()).getAsJsonPrimitive();
    }

    public static JsonArray fixNulls(JsonArray array) {
        JsonArray newArray = new JsonArray();
        for (JsonElement element : array) newArray.add(fixNulls(element));
        return newArray;
    }

    public static JsonObject fixNulls(JsonObject object) {
        JsonObject newObject = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet())
            newObject.add(entry.getKey(), fixNulls(entry.getValue()));
        return newObject;
    }
}
