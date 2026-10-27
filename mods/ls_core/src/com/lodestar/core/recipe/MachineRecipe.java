package com.lodestar.core.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic data-driven machine recipe: up to N counted item inputs, chance outputs,
 * an optional fluid input and output, a processing time (ticks) and energy per tick.
 *
 * JSON:
 * {"type":"modid:kind","inputs":[{"item":"x","count":2}],"outputs":[{"item":"y","count":1,"chance":0.5}],
 *  "fluid_in":{"fluid":"minecraft:water","amount":250},"fluid_out":{...},"time":200,"energy":20}
 */
public class MachineRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final MachineRecipeKind kind;
    public final List<CountedIngredient> inputs;
    public final List<ChanceStack> outputs;
    public final FluidStack fluidIn;
    public final FluidStack fluidOut;
    public final FluidStack fluidIn2;
    public final FluidStack fluidOut2;
    public final int time;
    public final int energy;

    public MachineRecipe(ResourceLocation id, MachineRecipeKind kind, List<CountedIngredient> inputs, List<ChanceStack> outputs,
                         FluidStack fluidIn, FluidStack fluidOut, FluidStack fluidIn2, FluidStack fluidOut2, int time, int energy) {
        this.id = id;
        this.kind = kind;
        this.inputs = inputs;
        this.outputs = outputs;
        this.fluidIn = fluidIn;
        this.fluidOut = fluidOut;
        this.fluidIn2 = fluidIn2;
        this.fluidOut2 = fluidOut2;
        this.time = Math.max(1, time);
        this.energy = Math.max(0, energy);
    }

    /**
     * Matches inputs to the given handler slots (order independent, each ingredient to a distinct slot).
     * @return array mapping ingredient index to slot index, or null if no match.
     */
    @Nullable
    public int[] match(IItemHandler items, int[] slots) {
        if (inputs.size() > slots.length) {
            return null;
        }
        int[] assign = new int[inputs.size()];
        boolean[] used = new boolean[slots.length];
        // every non-empty slot must be consumed by some ingredient so that stray items don't match
        if (!assign(items, slots, 0, assign, used)) {
            return null;
        }
        for (int s = 0; s < slots.length; s++) {
            if (!used[s] && !items.getStackInSlot(slots[s]).isEmpty()) {
                return null;
            }
        }
        return assign;
    }

    private boolean assign(IItemHandler items, int[] slots, int idx, int[] assign, boolean[] used) {
        if (idx == inputs.size()) {
            return true;
        }
        CountedIngredient ci = inputs.get(idx);
        for (int s = 0; s < slots.length; s++) {
            if (used[s]) continue;
            ItemStack st = items.getStackInSlot(slots[s]);
            if (!st.isEmpty() && ci.ingredient.test(st) && st.getCount() >= ci.count) {
                used[s] = true;
                assign[idx] = s;
                if (assign(items, slots, idx + 1, assign, used)) {
                    return true;
                }
                used[s] = false;
            }
        }
        return false;
    }

    /** Loose test used for slot validity: could this stack be part of this recipe? */
    public boolean usesItem(ItemStack stack) {
        for (CountedIngredient ci : inputs) {
            if (ci.ingredient.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean usesFluid(Fluid fluid) {
        return (!fluidIn.isEmpty() && fluidIn.getFluid().isSame(fluid)) || (!fluidIn2.isEmpty() && fluidIn2.getFluid().isSame(fluid));
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).stack.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).stack;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return kind.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return kind.type();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public MachineRecipeKind kind() {
        return kind;
    }

    // ------------------------------------------------------------------ serialization

    public static FluidStack fluidFromJson(@Nullable JsonElement el) {
        if (el == null || !el.isJsonObject()) {
            return FluidStack.EMPTY;
        }
        JsonObject o = el.getAsJsonObject();
        ResourceLocation rl = new ResourceLocation(GsonHelper.getAsString(o, "fluid"));
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(rl);
        if (fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY) {
            throw new JsonParseException("Unknown fluid " + rl);
        }
        return new FluidStack(fluid, GsonHelper.getAsInt(o, "amount", 1000));
    }

    public static class Serializer implements RecipeSerializer<MachineRecipe> {
        private final MachineRecipeKind kind;

        public Serializer(MachineRecipeKind kind) {
            this.kind = kind;
        }

        @Override
        public MachineRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<CountedIngredient> inputs = new ArrayList<>();
            JsonArray in = GsonHelper.getAsJsonArray(json, "inputs", new JsonArray());
            for (JsonElement e : in) {
                JsonObject o = e.getAsJsonObject();
                JsonElement ingJson = o.has("ingredient") ? o.get("ingredient") : o;
                Ingredient ing = Ingredient.fromJson(ingJson);
                inputs.add(new CountedIngredient(ing, GsonHelper.getAsInt(o, "count", 1)));
            }
            List<ChanceStack> outputs = new ArrayList<>();
            JsonArray out = GsonHelper.getAsJsonArray(json, "outputs", new JsonArray());
            for (JsonElement e : out) {
                JsonObject o = e.getAsJsonObject();
                ItemStack st = ShapedRecipe.itemStackFromJson(o);
                outputs.add(new ChanceStack(st, GsonHelper.getAsFloat(o, "chance", 1.0f)));
            }
            FluidStack fin = fluidFromJson(json.get("fluid_in"));
            FluidStack fout = fluidFromJson(json.get("fluid_out"));
            FluidStack fin2 = fluidFromJson(json.get("fluid_in2"));
            FluidStack fout2 = fluidFromJson(json.get("fluid_out2"));
            int time = GsonHelper.getAsInt(json, "time", 200);
            int energy = GsonHelper.getAsInt(json, "energy", 20);
            return new MachineRecipe(id, kind, Collections.unmodifiableList(inputs), Collections.unmodifiableList(outputs), fin, fout, fin2, fout2, time, energy);
        }

        @Nullable
        @Override
        public MachineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<CountedIngredient> inputs = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Ingredient ing = Ingredient.fromNetwork(buf);
                inputs.add(new CountedIngredient(ing, buf.readVarInt()));
            }
            int m = buf.readVarInt();
            List<ChanceStack> outputs = new ArrayList<>();
            for (int i = 0; i < m; i++) {
                ItemStack st = buf.readItem();
                outputs.add(new ChanceStack(st, buf.readFloat()));
            }
            FluidStack fin = FluidStack.readFromPacket(buf);
            FluidStack fout = FluidStack.readFromPacket(buf);
            FluidStack fin2 = FluidStack.readFromPacket(buf);
            FluidStack fout2 = FluidStack.readFromPacket(buf);
            int time = buf.readVarInt();
            int energy = buf.readVarInt();
            return new MachineRecipe(id, kind, inputs, outputs, fin, fout, fin2, fout2, time, energy);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MachineRecipe r) {
            buf.writeVarInt(r.inputs.size());
            for (CountedIngredient ci : r.inputs) {
                ci.ingredient.toNetwork(buf);
                buf.writeVarInt(ci.count);
            }
            buf.writeVarInt(r.outputs.size());
            for (ChanceStack cs : r.outputs) {
                buf.writeItem(cs.stack);
                buf.writeFloat(cs.chance);
            }
            r.fluidIn.writeToPacket(buf);
            r.fluidOut.writeToPacket(buf);
            r.fluidIn2.writeToPacket(buf);
            r.fluidOut2.writeToPacket(buf);
            buf.writeVarInt(r.time);
            buf.writeVarInt(r.energy);
        }
    }
}
