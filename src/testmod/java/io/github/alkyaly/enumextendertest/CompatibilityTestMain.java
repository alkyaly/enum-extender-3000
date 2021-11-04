package io.github.alkyaly.enumextendertest;

import com.chocohead.mm.api.ClassTinkerers;
import io.github.alkyaly.enumextender.EnumExtender;
import net.fabricmc.api.ModInitializer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

import java.util.Arrays;
import java.util.Map;

public class CompatibilityTestMain implements ModInitializer {
    @Override
    public void onInitialize() {
        Rarity mm = ClassTinkerers.getEnum(Rarity.class, "MM_RARITY");
        TestMain.LOGGER.info("Enum: " + mm + " Ordinal: " + mm.ordinal());
        Rarity ee = EnumExtender.addToEnum(Rarity.class, null, "EE_RARITY", Map.of("color", ChatFormatting.AQUA));
        TestMain.LOGGER.info("Enum: " + ee + " Ordinal: " + ee.ordinal());
        TestMain.LOGGER.info(Arrays.toString(Rarity.values()));
    }
}
