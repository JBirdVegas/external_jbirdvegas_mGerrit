-keep class com.google.gerrit.extensions.common.** {
    <fields>;
}

# Explicitly preserve all serialization members. The Serializable interface
# is only a marker interface, so it wouldn't save them.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


# Preserve all native method names and the names of their classes.
-keepclasseswithmembernames class * {
    native <methods>;
}


-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
