package com.reborn.modularmachinery.test;

import com.reborn.modularmachinery.recipe.IOType;
import com.reborn.modularmachinery.recipe.RecipeModifier;
import com.reborn.modularmachinery.recipe.RecipeModifiers;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecipeModifierTest {

    @Test
    public void testEmptyModifiers() {
        RecipeModifiers mods = RecipeModifiers.EMPTY;
        assertEquals(100.0, mods.apply(RecipeModifier.Target.DURATION, null, 100.0, false));
        assertEquals(50.0, mods.apply(RecipeModifier.Target.ENERGY, IOType.INPUT, 50.0, false));
    }

    @Test
    public void testDurationAddAndMultiply() {
        RecipeModifier add = new RecipeModifier(RecipeModifier.Target.DURATION, null, 20.0F, RecipeModifier.Operation.ADD, false);
        RecipeModifier mul = new RecipeModifier(RecipeModifier.Target.DURATION, null, 0.5F, RecipeModifier.Operation.MULTIPLY, false);

        RecipeModifiers mods = RecipeModifiers.of(List.of(add, mul));
        // (100 + 20) * 0.5 = 60
        double result = mods.applyDuration(100.0);
        assertEquals(60.0, result, 0.001);
    }

    @Test
    public void testTargetAndIoFiltering() {
        RecipeModifier itemOutMul = new RecipeModifier(RecipeModifier.Target.ITEM, IOType.OUTPUT, 2.0F, RecipeModifier.Operation.MULTIPLY, false);
        RecipeModifier itemInAdd = new RecipeModifier(RecipeModifier.Target.ITEM, IOType.INPUT, 5.0F, RecipeModifier.Operation.ADD, false);
        RecipeModifiers mods = RecipeModifiers.of(List.of(itemOutMul, itemInAdd));

        // Item output multiplied by 2
        assertEquals(20.0, mods.apply(RecipeModifier.Target.ITEM, IOType.OUTPUT, 10.0, false), 0.001);
        // Item input added by 5
        assertEquals(15.0, mods.apply(RecipeModifier.Target.ITEM, IOType.INPUT, 10.0, false), 0.001);
        // Energy unaffected
        assertEquals(10.0, mods.apply(RecipeModifier.Target.ENERGY, IOType.INPUT, 10.0, false), 0.001);
        // Duration unaffected
        assertEquals(10.0, mods.applyDuration(10.0), 0.001);
    }
}
