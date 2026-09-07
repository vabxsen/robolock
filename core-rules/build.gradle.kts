plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Deliberately a plain JVM module with no Android dependency. Domain logic that drifts into
// android.* stops compiling here, which is the point: the rule engine must stay unit-testable
// on the JVM and free of framework behaviour.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
