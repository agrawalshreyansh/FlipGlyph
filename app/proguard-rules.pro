# Vendor SDK: unverified whether it relies on reflection (its AIDL stub does), so keep it
# whole rather than risk R8 silently breaking real hardware output for a few saved KB.
-keep class com.nothing.** { *; }
