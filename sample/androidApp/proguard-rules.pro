# Сохраняем Activity-точки входа для macrobenchmark
-keep class com.ukenoveldiyar.rcc.CompiledCounterActivity { *; }
-keep class com.ukenoveldiyar.rcc.InterpretedCounterActivity { *; }
-keep class com.ukenoveldiyar.sample.MainActivity { *; }

# ProfileInstaller — нужен для macrobenchmark DROP_SHADER_CACHE
-keep class androidx.profileinstaller.** { *; }

# Compose runtime — внутренние API нужны интерпретатору
-keep class androidx.compose.runtime.** { *; }

# Сохраняем аннотации
-keepattributes *Annotation*
-keepattributes Signature

# R8 может свободно инлайнить и оптимизировать rcc-runtime
