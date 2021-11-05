# Enum Extender 3000

Create enum constants easily.

## Adding to your dependencies

Kotlin DSL:
```kotlin
repositories {
    //[...]
    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io/")
    }
    //[...]
}

dependencies {
    //[...]
    modImplementation(include("com.github.alkyaly:enum-extender-3000:master-SNAPSHOT")!!)
    //[...]
}
```

Groovy DSL:
```groovy
repositories {
    //[...]
    maven {
        name 'Jitpack'
        url 'https://jitpack.io/'
    }
    //[...]
}

dependencies {
    //[...]
    modImplementation(include('com.github.alkyaly:enum-extender-3000:master-SNAPSHOT'))
    //[...]
}
```

## Usage

Use the `EnumExtender.addToEnum` method, it takes the enum class to add, optionally an enum subclass to extend, the new constant name and a map of fieldname->value.<br>
If the enum you're trying to add has an abstract method, you can define a subclass with `EnumExtender.defineExtendedEnum`<br>
which takes the enum to extend and an array of method nodes of the methods to override.

<br>
Example:

```java
@Override
public void onInitialize() {
    Rarity rarity = EnumExtender.addToEnum(Rarity.class, null, "MY_RARITY", Map.of("color", ChatFormatting.AQUA)); //simple enum

    //enum with abstract method    

    //method that will override the abstract method
    MethodNode node = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "canEnchant",
                "(Lnet/minecraft/world/item/Item;)Z",
                null,
                null
    );
    node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); //get first parameter (0=this, 1=parameter)
    node.instructions.add(new TypeInsnNode(Opcodes.INSTANCEOF, "net/minecraft/world/item/EnderEyeItem")); //item instanceof EnderEyeItem
    node.instructions.add(new InsnNode(Opcodes.IRETURN)); //return the result of the above
    EnchantmentCategory cat = EnumExtender.addToEnum(
        EnchantmentCategory.class,
        EnumExtender.defineExtendedEnum(
            EnchantmentCategory.class,
            node
        ),
        "ENDER_EYE_CATEGORY",
        null
    );
}
```

