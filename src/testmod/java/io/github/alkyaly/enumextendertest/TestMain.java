package io.github.alkyaly.enumextendertest;

import io.github.alkyaly.enumextender.EnumExtender;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Arrays;
import java.util.Map;

public class TestMain implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Enum Extender 3000 - Test");
    private static final MappingResolver RESOLVER = FabricLoader.getInstance().getMappingResolver();

    public static final String ID = RESOLVER
            .mapFieldName("intermediary",
                    "net.minecraft.class_5762$class_5767",
                    "field_28347",
                    "I"
            );
    public static final String NAME = RESOLVER
            .mapFieldName("intermediary",
                    "net.minecraft.class_5762$class_5767",
                    "field_28348",
                    "Ljava/lang/String;"
            );
    public static final String COMMON = RESOLVER
            .mapFieldName("intermediary",
                    "net.minecraft.class_5762$class_5767",
                    "field_28349",
                    "Z"
            );


    public static final String ITEM = RESOLVER
            .mapClassName("intermediary", "net.minecraft.class_1792")
            .replace('.', '/');
    public static final String CAN_ENCHANT = RESOLVER.
            mapMethodName("intermediary",
                    "net.minecraft.class_1886",
                    "method_8177",
                    "(Lnet/minecraft/class_1792;)Z"
            );

    public static final String ENDER_EYE_ITEM = RESOLVER
            .mapClassName("intermediary", "net.minecraft.class_1777")
            .replace('.', '/');

    @Override
    public void onInitialize() {
        int size = Axolotl.Variant.values().length;
        Axolotl.Variant variant = EnumExtender.addToEnum(
                Axolotl.Variant.class,
                null,
                "MY_VAR_1",
                Map.of(ID, 10, NAME, "l", COMMON, false)
        );

        assertThat(size == variant.ordinal());
        assertThat(variant.getId() == 10 && "l".equals(variant.getName()));

        Axolotl.Variant var2 = EnumExtender.addToEnum(
                Axolotl.Variant.class,
                null,
                "MY_VAR_2",
                Map.of(ID, 34892, NAME, "dfjiskoj", COMMON, false)
        );
        assertThat(var2.getId() == 34892 && "dfjiskoj".equals(var2.getName()));
        assertThat(ArrayUtils.contains(Axolotl.Variant.values(), variant) && ArrayUtils.contains(Axolotl.Variant.values(), var2));

        LOGGER.info(Arrays.toString(Axolotl.Variant.values()));

        MethodNode node = new MethodNode(
                Opcodes.ACC_PUBLIC,
                CAN_ENCHANT,
                "(L" + ITEM + ";)Z",
                null,
                null
        );
        node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        node.instructions.add(new TypeInsnNode(Opcodes.INSTANCEOF, ENDER_EYE_ITEM));
        node.instructions.add(new InsnNode(Opcodes.IRETURN));

        EnchantmentCategory cat = EnumExtender.addToEnum(
                EnchantmentCategory.class,
                EnumExtender.defineExtendedEnum(EnchantmentCategory.class, node),
                "MY_CAT",
                null
        );
        assertThat(cat.canEnchant(Items.ENDER_EYE) && !cat.canEnchant(Items.GLOW_ITEM_FRAME));
        LOGGER.info(Arrays.toString(EnchantmentCategory.values()));
    }

    private static void assertThat(boolean cond) {
        if (!cond)
            throw new IllegalArgumentException();
    }
}
