package io.github.alkyaly.enumextender;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Objects;

public final class EnumExtender {

    public static final Logger LOGGER = LogManager.getLogger("Enum Extender 3000");

    //if you aren't using unsafe while modding, are you even modding? smh my head my head
    private static final sun.misc.Unsafe UNSAFE;
    private static final MethodHandle DEFINE_CLASS;

    /**
     * Creates a new enum constant and adds it to the $VALUES array
     *
     * @param enun         the enum to create a constant of
     * @param childEnum    pptional enum class for abstract enums
     * @param constantName the constant name
     * @param args         a map of field names and their values to set
     * @return the new constant
     */
    public static <T extends Enum<T>> T addToEnum(Class<T> enun, @Nullable Class<T> childEnum, String constantName, Map<String, Object> args) {
        try {
            T constant = createConstant(enun, childEnum, args);
            int len = changeValues(enun, constant) - 1;
            setEnumFields(constant, constantName, len);

            return constant;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Something went wrong while adding" + constantName + " to enum " + enun.getName());
        }
    }

    /**
     * @param enun      the enum to create a constant of
     * @param childEnum optional enum class for abstract enums
     * @param args      a map of field names and their values to set
     * @return the new constant
     */
    private static <T extends Enum<T>> T createConstant(Class<T> enun, @Nullable Class<T> childEnum, Map<String, Object> args) throws ReflectiveOperationException {
        if (childEnum != null)
            Preconditions.checkArgument(childEnum.getSuperclass() == enun,
                    "childEnum must be a subclass of the provided enum!");

        Method allocateInstance = UNSAFE.getClass().getDeclaredMethod("allocateInstance", Class.class);
        allocateInstance.setAccessible(true);
        //skips java enum constructor restriction by not calling it
        T constant = (T) allocateInstance.invoke(UNSAFE, childEnum != null ? childEnum : enun);

        //since we didn't call the constructor, we must initialize the fields manually
        if (args != null)
            for (Map.Entry<String, Object> entry : args.entrySet()) {
                Field field = constant.getClass().getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(constant, entry.getValue());
            }

        LOGGER.debug("Successfully created constant {} for enum {}", constant, enun.getName());
        return constant;
    }

    /**
     * Sets the synthetic values field in the supplied class with a new extended array.
     *
     * @param enumCls the class to set the new $VALUES array
     * @param putLast the constant to put on last
     * @return the new length
     */
    private static <T extends Enum<T>> int changeValues(Class<T> enumCls, T putLast) throws IllegalAccessException {
        Field valuesField = null;
        try {
            valuesField = enumCls.getDeclaredField("$VALUES"); //default, not intermediary name
        } catch (NoSuchFieldException e) {
            //eh, this might break if there is other synthetic static field in that class
            for (Field field : enumCls.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                if ((field.getModifiers() & 0x00001000) == 0) continue; //is synthetic
                valuesField = field;
                break;
            }
        }

        Objects.requireNonNull(valuesField, "Could not find $VALUES field in class " + enumCls.getName());
        valuesField.setAccessible(true);

        Object[] arr = (Object[]) valuesField.get(null);
        T[] nw = extendArray(arr, enumCls, putLast);
        //can't set with reflection
        UNSAFE.putObject(UNSAFE.staticFieldBase(valuesField), UNSAFE.staticFieldOffset(valuesField), nw);

        return nw.length;
    }

    /**
     * Makes a larger copy of supplied array and puts a new value on the last index.
     *
     * @param array   the array to extend
     * @param putLast the constant to put on last
     * @return the new array
     */
    private static <T extends Enum<T>> T[] extendArray(Object[] array, Class<T> cls, T putLast) {
        T[] nwArray = (T[]) Array.newInstance(cls, array.length + 1);
        System.arraycopy(array, 0, nwArray, 0, array.length);
        nwArray[nwArray.length - 1] = putLast;
        return nwArray;
    }

    /**
     * Sets ordinal and name for a constant with {@link sun.misc.Unsafe}
     *
     * @param constant the constant
     * @param name     the name
     * @param ordinal  the ordinal
     */
    private static <T extends Enum<T>> void setEnumFields(T constant, String name, int ordinal) throws NoSuchFieldException, SecurityException {
        Field ordField = Enum.class.getDeclaredField("ordinal");
        Field nameField = Enum.class.getDeclaredField("name");

        //can't reflect into java.lang.Enum because modules
        //let's set it with Unsafe instead
        UNSAFE.putInt(constant, UNSAFE.objectFieldOffset(ordField), ordinal);
        UNSAFE.putObject(constant, UNSAFE.objectFieldOffset(nameField), name);
    }

    /**
     * Defines a class extending the supplied enum.
     * Use for enums with abstract methods.
     * @param parent the extended enum
     * @param overridenMethods the methods that must be overridden
     * @return the defined class
     */
    public static <T extends Enum<T>> Class<T> defineExtendedEnum(Class<T> parent, MethodNode... overridenMethods) {
        Type type = Type.getType(parent);
        String name = type.getInternalName() + "$" + RandomStringUtils.random(8, true, true);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        writer.visit(Opcodes.V16,
                Opcodes.ACC_FINAL | Opcodes.ACC_ENUM,
                name,
                null,
                type.getInternalName(),
                null
        );
        for (MethodNode method : overridenMethods) {
            MethodVisitor visitor = writer.visitMethod(
                    method.access,
                    method.name,
                    method.desc,
                    null,
                    method.exceptions.toArray(new String[0])
            );
            method.accept(visitor);
            visitor.visitEnd();
        }
        writer.visitEnd();

        try {
            byte[] bytes = writer.toByteArray();
            return (Class<T>) DEFINE_CLASS.invokeExact(name, bytes, 0, bytes.length, parent.getClassLoader(), parent.getProtectionDomain());
        } catch (Throwable e) {
            throw new RuntimeException("Could not define class!", e);
        }
    }

    static {
        try {
            Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) theUnsafe.get(null);
        } catch (Throwable e) {
            throw new IllegalStateException("Could not get Unsafe instance via reflection!", e);
        }

        try {
            Field impl = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            MethodHandles.Lookup implLookup = (MethodHandles.Lookup) UNSAFE.getObject(UNSAFE.staticFieldBase(impl), UNSAFE.staticFieldOffset(impl));

            Class<?> internalUnsafe = Class.forName("jdk.internal.misc.Unsafe");
            Object theInternalUnsafe = implLookup.findStatic(internalUnsafe, "getUnsafe", MethodType.methodType(internalUnsafe)).invoke();

            DEFINE_CLASS = implLookup.bind(theInternalUnsafe, "defineClass", MethodType.methodType(
                    Class.class,
                    String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class
            ));
        } catch (Throwable t) {
            throw new IllegalStateException("Could not get jdk/internal/misc/Unsafe#defineClass method handle!", t);
        }
    }
}