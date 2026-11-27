# JNI entry-point names are hard-coded by the exported C++ symbol names.
-keep class org.adamant.lab.AdamantNative { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
