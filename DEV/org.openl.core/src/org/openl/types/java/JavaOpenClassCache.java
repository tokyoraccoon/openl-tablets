package org.openl.types.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections4.map.AbstractReferenceMap;
import org.apache.commons.collections4.map.ReferenceMap;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;

final class JavaOpenClassCache {

    private static class JavaOpenClassCacheHolder {
        private static final JavaOpenClassCache INSTANCE = new JavaOpenClassCache();
    }

    public static JavaOpenClassCache getInstance() {
        return JavaOpenClassCacheHolder.INSTANCE;
    }

    /**
     * Stores a strong references to common java classes that's why they will
     * not be garbage collected
     */
    private volatile Map<Class<?>, JavaOpenClass> javaClassCache;

    /**
     * Cache for all classes (including javaClassCache and generated by OpenL
     * classes) Uses soft references to prevent memory leak. Classes added to
     * javaClassCache will not be garbage collected. TODO use better cache
     * implementation instead
     */
    private Map<Class<?>, JavaOpenClass> cache = new ReferenceMap<Class<?>, JavaOpenClass>(
        AbstractReferenceMap.ReferenceStrength.SOFT,
        AbstractReferenceMap.ReferenceStrength.SOFT);

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private Map<Class<?>, JavaOpenClass> getJavaClassCache() {
        if (javaClassCache == null) {
            synchronized (this) {
                if (javaClassCache == null) {
                    javaClassCache = initializeJavaClassCache();
                }
            }
        }
        return javaClassCache;
    }

    private static Map<Class<?>, JavaOpenClass> initializeJavaClassCache() {
        Map<Class<?>, JavaOpenClass> javaClassCache = new HashMap<Class<?>, JavaOpenClass>();
        javaClassCache.put(int.class, JavaOpenClass.INT);
        javaClassCache.put(Integer.class, new JavaOpenClass(Integer.class, true));
        javaClassCache.put(long.class, JavaOpenClass.LONG);
        javaClassCache.put(Long.class, new JavaOpenClass(Long.class, true));
        javaClassCache.put(double.class, JavaOpenClass.DOUBLE);
        javaClassCache.put(Double.class, new JavaOpenClass(Double.class, true));
        javaClassCache.put(float.class, JavaOpenClass.FLOAT);
        javaClassCache.put(Float.class, new JavaOpenClass(Float.class, true));
        javaClassCache.put(short.class, JavaOpenClass.SHORT);
        javaClassCache.put(Short.class, new JavaOpenClass(Short.class, true));
        javaClassCache.put(char.class, JavaOpenClass.CHAR);
        javaClassCache.put(Character.class, new JavaOpenClass(Character.class, true));
        javaClassCache.put(byte.class, JavaOpenClass.BYTE);
        javaClassCache.put(Byte.class, new JavaOpenClass(Byte.class, true));
        javaClassCache.put(boolean.class, JavaOpenClass.BOOLEAN);
        javaClassCache.put(Boolean.class, new JavaOpenClass(Boolean.class, true));
        javaClassCache.put(void.class, JavaOpenClass.VOID);
        javaClassCache.put(String.class, JavaOpenClass.STRING);
        javaClassCache.put(Object.class, JavaOpenClass.OBJECT);
        javaClassCache.put(Class.class, JavaOpenClass.CLASS);
        javaClassCache.put(Date.class, new JavaOpenClass(Date.class, true));
        javaClassCache.put(BigInteger.class, new JavaOpenClass(BigInteger.class, true));
        javaClassCache.put(BigDecimal.class, new JavaOpenClass(BigDecimal.class, true));
        javaClassCache.put(BigDecimalValue.class, new JavaOpenClass(BigDecimalValue.class, true));
        javaClassCache.put(BigIntegerValue.class, new JavaOpenClass(BigIntegerValue.class, true));
        javaClassCache.put(ByteValue.class, new JavaOpenClass(ByteValue.class, true));
        javaClassCache.put(DoubleValue.class, new JavaOpenClass(DoubleValue.class, true));
        javaClassCache.put(FloatValue.class, new JavaOpenClass(FloatValue.class, true));
        javaClassCache.put(IntValue.class, new JavaOpenClass(IntValue.class, true));
        javaClassCache.put(LongValue.class, new JavaOpenClass(LongValue.class, true));
        javaClassCache.put(ShortValue.class, new JavaOpenClass(ShortValue.class, true));
        javaClassCache.put(org.openl.meta.StringValue.class,
            new JavaOpenClass(org.openl.meta.StringValue.class, true));
        return javaClassCache;
    }

    public JavaOpenClass get(Class<?> c) {
        JavaOpenClass openClass = getJavaClassCache().get(c);
        if (openClass != null) {
            return openClass;
        }
        Lock lock = readWriteLock.readLock();
        try {
            lock.lock();
            return cache.get(c);
        } finally {
            lock.unlock();
        }
    }

    public Collection<Class<?>> getNonJavaClasses() {
        Lock lock = readWriteLock.readLock();
        try {
            lock.lock();
            return Collections.unmodifiableCollection(cache.keySet());
        } finally {
            lock.unlock();
        }
    }

    public void put(Class<?> c, JavaOpenClass openClass) {
        if (getJavaClassCache().containsKey(c)) {
            return;
        }
        Lock lock = readWriteLock.writeLock();
        try {
            lock.lock();
            cache.put(c, openClass);
        } finally {
            lock.unlock();
        }
    }

    public void remove(Class<?> c) {
        if (getJavaClassCache().containsKey(c)) {
            return;
        }
        Lock lock = readWriteLock.writeLock();
        try {
            lock.lock();
            cache.remove(c);
        } finally {
            lock.unlock();
        }
    }
}
