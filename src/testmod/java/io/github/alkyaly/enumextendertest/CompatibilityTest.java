package io.github.alkyaly.enumextendertest;

import com.chocohead.mm.api.ClassTinkerers;
import net.minecraft.ChatFormatting;

public class CompatibilityTest implements Runnable {
    @Override
    public void run() {
        ClassTinkerers.enumBuilder("net.minecraft.world.item.Rarity", "Lnet/minecraft/ChatFormatting;")
                .addEnum("MM_RARITY", () -> new Object[]{ChatFormatting.BLACK})
                .build();
    }
}
