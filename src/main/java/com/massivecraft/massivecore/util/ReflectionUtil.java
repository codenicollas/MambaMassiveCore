package com.massivecraft.massivecore.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Bukkit;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.massivecraft.massivecore.collections.MassiveList;
import com.massivecraft.massivecore.comparator.ComparatorNaturalOrder;
import com.massivecraft.massivecore.predicate.Predicate;
import com.massivecraft.massivecore.predicate.PredicateAnd;

import sun.misc.Unsafe;

public class ReflectionUtil {

    // -------------------------------------------- //
    // CONSTANTS
    // -------------------------------------------- //

    private static final int STRATEGY_DIRECT = 1;
    private static final int STRATEGY_HANDLE = 2;
    private static final int STRATEGY_UNSAFE = 3;
    private static final int STRATEGY_NONE = 0;

    private static final int FINAL_REMOVAL_STRATEGY;

    private static Field fieldDotModifiers = null;
    private static java.lang.invoke.MethodHandle modifiersHandle = null;
    private static Unsafe theUnsafe = null;
    private static long modifiersOffset = -1L;

    static {
        int strategy = STRATEGY_NONE;

        try {
            Field f = Field.class.getDeclaredField("modifiers");
            f.setAccessible(true);
            f.getInt(f);
            fieldDotModifiers = f;
            strategy = STRATEGY_DIRECT;
        } catch (Throwable ignored) {
        }

        if (strategy == STRATEGY_NONE) {
            try {
                Field lookupField = java.lang.invoke.MethodHandles.Lookup.class
                        .getDeclaredField("IMPL_LOOKUP");
                lookupField.setAccessible(true);
                java.lang.invoke.MethodHandles.Lookup lookup = (java.lang.invoke.MethodHandles.Lookup) lookupField
                        .get(null);
                modifiersHandle = lookup.findSetter(Field.class, "modifiers", int.class);
                strategy = STRATEGY_HANDLE;
            } catch (Throwable ignored) {
            }
        }

        if (strategy == STRATEGY_NONE) {
            try {
                Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                theUnsafe = (Unsafe) unsafeField.get(null);

                Field modField = Field.class.getDeclaredField("modifiers");
                modifiersOffset = theUnsafe.objectFieldOffset(modField);
                strategy = STRATEGY_UNSAFE;
            } catch (Throwable ignored) {
            }
        }

        if (strategy == STRATEGY_NONE) {
            System.err.println("[ReflectionUtil] AVISO: Nenhuma estratégia de remoção " +
                    "de 'final' disponível neste JVM. Campos final permanecerão imutáveis.");
        }

        FINAL_REMOVAL_STRATEGY = strategy;
    }

    private static final Class<?>[] EMPTY_ARRAY_OF_CLASS = {};
    private static final Object[] EMPTY_ARRAY_OF_OBJECT = {};

    // -------------------------------------------- //
    // MAKE ACCESSIBLE
    // -------------------------------------------- //

    public static void makeAccessible(Field field) {
        try {
            field.setAccessible(true);
        } catch (Throwable e) {
            System.err.println("[ReflectionUtil] Não foi possível chamar setAccessible " +
                    "no campo '" + field.getName() + "': " + e.getMessage());
        }

        if (!Modifier.isFinal(field.getModifiers()))
            return;

        try {
            switch (FINAL_REMOVAL_STRATEGY) {
                case STRATEGY_DIRECT:
                    fieldDotModifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                    break;

                case STRATEGY_HANDLE:
                    modifiersHandle.invokeExact(field, field.getModifiers() & ~Modifier.FINAL);
                    break;

                case STRATEGY_UNSAFE:
                    theUnsafe.putInt(field, modifiersOffset,
                            field.getModifiers() & ~Modifier.FINAL);
                    break;

                default:
                    break;
            }
        } catch (Throwable e) {
            System.err.println("[ReflectionUtil] Falha ao remover modificador 'final' " +
                    "do campo '" + field.getName() + "': " + e.getMessage());
        }
    }

    public static void makeAccessible(Method method) {
        try {
            method.setAccessible(true);
        } catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static void makeAccessible(Constructor<?> constructor) {
        try {
            constructor.setAccessible(true);
        } catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    // -------------------------------------------- //
    // METHOD
    // -------------------------------------------- //

    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            Method ret = clazz.getDeclaredMethod(name, parameterTypes);
            makeAccessible(ret);
            return ret;
        } catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static boolean hasMethod(Class<?> clazz, String name) {
        return hasMethod(clazz, name, EMPTY_ARRAY_OF_CLASS);
    }

    public static boolean hasMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            getMethod(clazz, name, parameterTypes);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static Method getMethod(Class<?> clazz, String name) {
        return getMethod(clazz, name, EMPTY_ARRAY_OF_CLASS);
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Method method, Object target, Object... arguments) {
        try {
            return (T) method.invoke(target, arguments);
        } catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static <T> T invokeMethod(Method method, Object target, Object argument) {
        return invokeMethod(method, target, new Object[] { argument });
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeMethod(Method method, Object target) {
        return (T) invokeMethod(method, target, EMPTY_ARRAY_OF_OBJECT);
    }

    // -------------------------------------------- //
    // CONSTRUCTOR
    // -------------------------------------------- //

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<T> ret = (Constructor<T>) clazz.getDeclaredConstructor(parameterTypes);
            makeAccessible(ret);
            return ret;
        } catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static <T> Constructor<T> getConstructor(Class<?> clazz) {
        return getConstructor(clazz, EMPTY_ARRAY_OF_CLASS);
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeConstructor(Constructor<?> constructor, Object... arguments) {
        try {
            return (T) constructor.newInstance(arguments);
        } catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    public static <T> T invokeConstructor(Constructor<?> constructor, Object argument) {
        return invokeConstructor(constructor, new Object[] { argument });
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeConstructor(Constructor<?> constructor) {
        return (T) invokeConstructor(constructor, EMPTY_ARRAY_OF_OBJECT);
    }

    // -------------------------------------------- //
    // NEW INSTANCE
    // -------------------------------------------- //

    @SuppressWarnings({ "unchecked", "deprecation" })
    public static <T> T newInstance(Class<?> clazz) {
        try {
            return (T) clazz.newInstance();
        } catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    // -------------------------------------------- //
    // SINGLETON INSTANCE
    // -------------------------------------------- //

    public static <T> T getSingletonInstance(Class<?> clazz) {
        Method get = getMethod(clazz, "get");
        T ret = invokeMethod(get, null);
        if (ret == null)
            throw new NullPointerException("Singleton instance was null for: " + clazz);
        if (!clazz.isAssignableFrom(ret.getClass()))
            throw new IllegalStateException("Singleton instance was not of same or subclass for: " + clazz);
        return ret;
    }

    public static <T> T getSingletonInstanceFirstCombatible(Iterable<Class<?>> classes, T fallback) {
        for (Class<?> c : classes) {
            try {
                return ReflectionUtil.getSingletonInstance(c);
            } catch (Throwable t) {
                // Not Compatible
            }
        }
        return fallback;
    }

    public static boolean isSingleton(Class<?> clazz) {
        try {
            @SuppressWarnings("unused")
            Method get = getMethod(clazz, "get");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    // -------------------------------------------- //
    // ANNOTATION
    // -------------------------------------------- //

    public static <T extends Annotation> T getAnnotation(Field field, Class<T> annotationClass) {
        if (field == null)
            throw new NullPointerException("field");
        if (annotationClass == null)
            throw new NullPointerException("annotationClass");

        try {
            return field.getAnnotation(annotationClass);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    // -------------------------------------------- //
    // FIELD > GET
    // -------------------------------------------- //

    public static Field getField(Class<?> clazz, String name) {
        try {
            Field ret = clazz.getDeclaredField(name);
            makeAccessible(ret);
            return ret;
        } catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getField(Field field, Object object) {
        try {
            return (T) field.get(object);
        } catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    // -------------------------------------------- //
    // FIELD > SET
    // -------------------------------------------- //

    public static void setField(Field field, Object object, Object value) {
        try {
            field.set(object, value);
        } catch (Throwable e) {
            throw asRuntimeException(e);
        }
    }

    // -------------------------------------------- //
    // FIELD > SIMPLE
    // -------------------------------------------- //

    public static <T> T getField(Class<?> clazz, String name, Object object) {
        Field field = getField(clazz, name);
        return getField(field, object);
    }

    public static void setField(Class<?> clazz, String name, Object object, Object value) {
        Field field = getField(clazz, name);
        setField(field, object, value);
    }

    // -------------------------------------------- //
    // FIELD > TRANSFER
    // -------------------------------------------- //

    public static void transferField(Class<?> clazz, Object from, Object to, String name) {
        Field field = getField(clazz, name);
        Object value = getField(field, from);
        setField(field, to, value);
    }

    public static void transferFields(Class<?> clazz, Object from, Object to, List<String> fieldNames) {
        if (fieldNames == null) {
            fieldNames = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {
                fieldNames.add(field.getName());
            }
        }
        for (String fieldName : fieldNames) {
            transferField(clazz, from, to, fieldName);
        }
    }

    public static void transferFields(Class<?> clazz, Object from, Object to) {
        transferFields(clazz, from, to, null);
    }

    // -------------------------------------------- //
    // SUPERCLASSES
    // -------------------------------------------- //

    public static List<Class<?>> getSuperclasses(Class<?> clazz, boolean includeSelf) {
        List<Class<?>> ret = new ArrayList<>();
        if (!includeSelf)
            clazz = clazz.getSuperclass();
        while (clazz != null) {
            ret.add(clazz);
            clazz = clazz.getSuperclass();
        }
        return ret;
    }

    public static Class<?> getSuperclassPredicate(Class<?> clazz, boolean includeSelf,
            Predicate<Class<?>> predicate) {
        for (Class<?> superClazz : getSuperclasses(clazz, includeSelf)) {
            if (predicate.apply(superClazz))
                return superClazz;
        }
        return null;
    }

    public static Class<?> getSuperclassDeclaringMethod(Class<?> clazz, boolean includeSelf,
            final String methodName) {
        return getSuperclassPredicate(clazz, includeSelf, superClazz -> {
            for (Method method : superClazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName))
                    return true;
            }
            return false;
        });
    }

    public static Class<?> getSuperclassDeclaringField(Class<?> clazz, boolean includeSelf,
            final String fieldName) {
        return getSuperclassPredicate(clazz, includeSelf, superClazz -> {
            for (Field field : superClazz.getDeclaredFields()) {
                if (field.getName().equals(fieldName))
                    return true;
            }
            return false;
        });
    }

    // -------------------------------------------- //
    // GET PACKAGE CLASSES
    // -------------------------------------------- //

    @SuppressWarnings("unchecked")
    public static List<Class<?>> getPackageClasses(String packageName, ClassLoader classLoader,
            boolean recursive, Predicate<Class<?>>... predicates) {
        List<Class<?>> ret = new MassiveList<>();

        try {
            ClassPath classPath = ClassPath.from(classLoader);
            Predicate<Class<?>> predicateCombined = PredicateAnd.get(predicates);

            Collection<ClassInfo> classInfos = recursive
                    ? classPath.getTopLevelClassesRecursive(packageName)
                    : classPath.getTopLevelClasses(packageName);

            for (ClassInfo classInfo : classInfos) {
                String className = classInfo.getName();
                if (className.contains(" "))
                    continue;

                Class<?> clazz;
                try {
                    clazz = classInfo.load();
                } catch (NoClassDefFoundError ex) {
                    continue;
                }

                if (!predicateCombined.apply(clazz))
                    continue;

                ret.add(clazz);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        ret.sort(Comparator.comparing(Class::getName, ComparatorNaturalOrder.get()));

        return ret;
    }

    // -------------------------------------------- //
    // AS RUNTIME EXCEPTION
    // -------------------------------------------- //

    public static RuntimeException asRuntimeException(Throwable t) {
        if (t == null)
            return new IllegalStateException("(causa desconhecida - excecao nula)");
        if (t instanceof RuntimeException)
            return (RuntimeException) t;
        if (t instanceof InvocationTargetException) {
            Throwable cause = ((InvocationTargetException) t).getCause();
            return asRuntimeException(cause != null ? cause : t);
        }
        if (t instanceof ExceptionInInitializerError) {
            Throwable cause = ((ExceptionInInitializerError) t).getCause();
            return asRuntimeException(cause != null ? cause : t);
        }
        return new IllegalStateException(t.getClass().getName() + ": " + t.getMessage(), t);
    }

    // -------------------------------------------- //
    // BUKKIT VERSION
    // -------------------------------------------- //

    private static final String versionRaw;
    private static final int versionMajor;
    private static final int versionMinor;
    private static final int versionRelease;

    static {
        String pkg = Bukkit.getServer().getClass().getPackage().getName();
        String raw;
        int major = 0, minor = 0, release = 0;
        try {
            String[] pkgParts = pkg.split("\\.");
            raw = null;
            for (String part : pkgParts) {
                if (part.startsWith("v") && part.contains("_")) {
                    raw = part;
                    break;
                }
            }

            if (raw == null) {
                raw = Bukkit.getBukkitVersion().split("-")[0]; // "1.21.1"
                String[] nums = raw.split("\\.");
                major = Integer.parseInt(nums[0]);
                minor = nums.length > 1 ? Integer.parseInt(nums[1]) : 0;
                release = nums.length > 2 ? Integer.parseInt(nums[2]) : 0;
                raw = "v" + major + "_" + minor + "_R" + release;
            } else {
                // "v1_21_R1" → ["v1", "21", "R1"]
                String[] parts = raw.split("_");
                major = Integer.parseInt(parts[0].substring(1));
                minor = Integer.parseInt(parts[1]);
                release = Integer.parseInt(parts[2].substring(1));
            }
        } catch (Throwable e) {
            raw = "v0_0_R0";
            major = 0;
            minor = 0;
            release = 0;
            System.err.println("[ReflectionUtil] Não foi possível detectar a versão do Bukkit: "
                    + e.getMessage());
        }

        versionRaw = raw;
        versionMajor = major;
        versionMinor = minor;
        versionRelease = release;
    }

    public static String getVersionRaw() {
        return versionRaw;
    }

    public static int getVersionMajor() {
        return versionMajor;
    }

    public static int getVersionMinor() {
        return versionMinor;
    }

    public static int getVersionRelease() {
        return versionRelease;
    }

    /**
     * @deprecated Use {@link #getVersionMajor()}/{@link #getVersionMinor()}
     *             diretamente.
     */
    @Deprecated
    public static String getVersionRawPart(int index) {
        return getVersionRaw().split("_")[index];
    }

    // -------------------------------------------- //
    // FORCE LOAD CLASSES
    // -------------------------------------------- //

    public static void forceLoadClasses(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            forceLoadClass(clazz);
        }
    }

    public static void forceLoadClass(Class<?> clazz) {
        String className = clazz.getSimpleName();
        if (className.startsWith("Spaces are not allowed in class names.")) {
            System.out.println(className);
        }
    }

    // -------------------------------------------- //
    // CLASS EXISTENCE
    // -------------------------------------------- //

    public static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    // -------------------------------------------- //
    // TYPE CHECKS
    // -------------------------------------------- //

    public static boolean isRawTypeAssignableFromAny(Type goal, Type... subjects) {
        Class<?> classGoal = classify(goal);
        for (Type t : subjects) {
            if (isRawTypeAssignableFrom(classGoal, t))
                return true;
        }
        return false;
    }

    public static boolean isRawTypeAssignableFrom(Type a, Type b) {
        if (a == null || b == null)
            return false;

        Class<?> classifiedA = classify(a);
        Class<?> classifiedB = classify(b);

        if (classifiedA == null || classifiedB == null)
            return a.equals(b);

        return classifiedA.isAssignableFrom(classifiedB);
    }

    @SuppressWarnings("rawtypes")
    private static Class<?> classify(Type type) {
        while (!(type instanceof Class)) {
            if (!(type instanceof ParameterizedType))
                return null;
            type = ((ParameterizedType) type).getRawType();
        }
        return (Class) type;
    }
}