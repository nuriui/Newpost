plugins {
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    java
    `maven-publish`
}

version = property("mod_version") as String
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

minecraft {
    mappings("official", "1.20.1")
    copyIdeResources.set(true)

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create("newposts") { source(sourceSets["main"]) }
            }
        }
        create("server") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create("newposts") { source(sourceSets["main"]) }
            }
        }
    }
}

repositories {
    mavenCentral()
}

jarJar.enable()

dependencies {
    val minecraftVersion = property("minecraft_version") as String
    val forgeVersion = property("forge_version") as String
    "minecraft"("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

    jarJar(implementation("org.jsoup:jsoup:[1.18.1,1.19)")!!)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Specification-Title" to "newposts",
            "Specification-Vendor" to "Teashoe",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Teashoe",
        )
    }
    finalizedBy("reobfJar")
}

tasks.named<Jar>("jarJar") {
    archiveClassifier.set("")
    finalizedBy("reobfJarJar")
}

reobf {
    create("jarJar")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = property("archives_base_name") as String
            from(components["java"])
        }
    }
}
