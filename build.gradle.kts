plugins {
    id("net.neoforged.moddev") version "2.0.78"
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

neoForge {
    version = property("neo_version") as String

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }

    mods {
        register("newposts") {
            sourceSet(sourceSets["main"])
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
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
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = property("archives_base_name") as String
            from(components["java"])
        }
    }
}
