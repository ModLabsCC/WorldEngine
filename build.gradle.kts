import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.*

plugins {
    kotlin("jvm") version "2.4.10"
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-SNAPSHOT"
    id("cc.modlabs.kpaper-gradle") version "2026.7.18.0717+kpaper.2026.7.18.0716"
    id("maven-publish")
}

val pluginVersion: String by project

val dailyVersion = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).run {
    "${get(Calendar.YEAR)}.${get(Calendar.MONTH) + 1}.${get(Calendar.DAY_OF_MONTH)}"
}

group = "cc.modlabs.worldengine"
version = System.getenv("VERSION_OVERRIDE") ?: "$pluginVersion-$dailyVersion"
val minecraftVersion: String by project
val slf4jVersion: String by project

val dotenvKotlinVersion: String by project

val kotlinxCoroutinesCoreVersion: String by project
val kotlinxCollectionsImmutableVersion: String by project

val gsonVersion: String by project

val mcCoroutineVersion: String by project

repositories {
    maven("https://repo-api.modlabs.cc/repo/maven/maven-mirror/")
    maven("https://repo.papermc.io/repository/maven-public/")
}


dependencies {
    paperweight.paperDevBundle("$minecraftVersion.build.60-beta")

    compileOnly("me.clip:placeholderapi:2.12.2")
    testImplementation(kotlin("test"))
}

kpaper {
    javaVersion.set(25)
    registrationBasePackage.set("cc.modlabs.worldengine")

    deliver (
        "com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:$mcCoroutineVersion",
        "com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:$mcCoroutineVersion",

        "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesCoreVersion",
        "org.jetbrains.kotlinx:kotlinx-collections-immutable:$kotlinxCollectionsImmutableVersion",
        "com.google.code.gson:gson:$gsonVersion",
        "com.github.wajda:lzstring4java:0.1",

        "io.github.cdimascio:dotenv-kotlin:$dotenvKotlinVersion", // - .env support
        "org.slf4j:slf4j-api:$slf4jVersion",
    )
}

tasks.register<Jar>("sourcesJar") {
    description = "Generates the sources jar for this project."
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

publishing {
    repositories {
        maven {
            name = "ModLabs"
            url = uri("https://repo-api.modlabs.cc/repo/maven/maven-public/")
            credentials {
                username = System.getenv("NEXUS_USER") ?: "modlabs"
                password = System.getenv("REPO_TOKEN")
            }
        }
        mavenLocal()
    }
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.named("reobfJar"))
            artifact(tasks.named("sourcesJar"))

            pom {
                name.set("WorldEngine")
                description.set("World management for Paper: worlds, generators, and a small API for other plugins.")
                url.set("https://github.com/ModLabsCC/WorldEngine")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/ModLabsCC/WorldEngine/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("ModLabsCC")
                        name.set("ModLabsCC")
                        email.set("contact@modlabs.cc")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/ModLabsCC/WorldEngine.git")
                    developerConnection.set("scm:git:git@github.com:ModLabsCC/WorldEngine.git")
                    url.set("https://github.com/ModLabsCC/WorldEngine")
                }
            }
        }
    }
}

tasks {
    withType<ProcessResources> {
        dependsOn("generateDependenciesFile")

        from(File(layout.buildDirectory.asFile.get(), "generated-resources")) {
            include(".dependencies")
        }

        expand(
            "version" to project.version,
            "name" to project.name,
        )
    }

}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/kotlin")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}