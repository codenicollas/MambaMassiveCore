plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
}

group = "com.massivecraft"
version = "2.13.6"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()

    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly(fileTree("libs") { include("*.jar") })

    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")

    implementation("org.mongodb:mongo-java-driver:3.12.14")
    implementation("org.json:json:20240303")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.google.guava:guava:31.1-jre")
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }

    shadowJar {
        archiveBaseName.set("MassiveCore")
        archiveClassifier.set("")

        relocate("com.google.gson", "com.massivecraft.massivecore.xlib.gson")
        relocate("com.google.common", "com.massivecraft.massivecore.xlib.guava")
        relocate("com.mongodb", "com.massivecraft.massivecore.xlib.mongodb")
        relocate("org.bson", "com.massivecraft.massivecore.xlib.bson")
        relocate("org.json", "com.massivecraft.massivecore.xlib.org.json")
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(8)
    }
}