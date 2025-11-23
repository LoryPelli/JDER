import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.compose") version "1.5.10"
    kotlin("plugin.serialization") version "1.9.20"
}

group = "com.jder"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}

compose.desktop {
    application {
        mainClass = "com.jder.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "JDER"
            description = "Java Diagrammi E-R - Editor di diagrammi Entity-Relationship"

            windows {
                menuGroup = "JDER"
                upgradeUuid = "BF5E4F2A-4F3B-4F3B-8F3B-4F3B4F3B4F3B"
            }

            macOS {
                bundleID = "com.jder.app"
            }

            linux {
                packageName = "jder"
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.jder.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
