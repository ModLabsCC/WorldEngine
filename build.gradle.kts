import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("jvm") version "2.3.20"
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    kotlin("plugin.serialization") version "2.3.20"
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

val fruxzAscendVersion: String by project
val fruxzStackedVersion: String by project

val kotlinxCoroutinesCoreVersion: String by project
val kotlinxCollectionsImmutableVersion: String by project

val gsonVersion: String by project

val mcCoroutineVersion: String by project

repositories {
    maven("https://repo-api.modlabs.cc/repo/maven/maven-mirror/")
    maven("https://papermc.io/repo/repository/maven-public/")
}

val deliverDependencies = listOf(
    "com.google.code.findbugs:jsr305:3.0.2",
    "com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:$mcCoroutineVersion",
    "com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:$mcCoroutineVersion",

    "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesCoreVersion",
    "org.jetbrains.kotlinx:kotlinx-collections-immutable:$kotlinxCollectionsImmutableVersion",
    "com.google.code.gson:gson:$gsonVersion",

    "dev.fruxz:ascend:$fruxzAscendVersion",
    "dev.fruxz:stacked:$fruxzStackedVersion",

    "io.github.cdimascio:dotenv-kotlin:$dotenvKotlinVersion", // - .env support
    "org.slf4j:slf4j-api:$slf4jVersion",
)

val includedDependencies = mutableListOf<String>()

fun Dependency?.deliver() = this?.apply {
    val computedVersion = version ?: kotlin.coreLibrariesVersion
    includedDependencies.add("${group}:${name}:${computedVersion}")
}

paperweight {
    reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

dependencies {
    paperweight.paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")

    compileOnly("me.clip:placeholderapi:2.12.2")

    implementation(kotlin("stdlib")).deliver()
    implementation(kotlin("reflect")).deliver()

    deliverDependencies.forEach { dependency ->
        implementation(dependency).deliver()
    }
}

tasks.register("generateDependenciesFile") {
    group = "build"
    description = "Writes dependencies to file"

    val dependenciesFile = File(layout.buildDirectory.asFile.get(), "generated-resources/.dependencies")
    outputs.file(dependenciesFile)
    doLast {
        dependenciesFile.parentFile.mkdirs()
        dependenciesFile.writeText(includedDependencies.joinToString("\n"))
    }
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
    build {
        dependsOn(reobfJar)
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

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

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        freeCompilerArgs.addAll(
            listOf(
                "-opt-in=kotlin.RequiresOptIn"
            )
        )
    }
}