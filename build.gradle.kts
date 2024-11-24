plugins {
    kotlin("jvm") version "2.0.21"
}

val rapidsAndRiversVersion = "2024112412131732446804.1b3dcc636bed"
val flywayCoreVersion = "10.21.0"
val postgresqlVersion = "42.7.4"
val kotliqueryVersion = "1.9.0"
val hikariCPVersion = "6.1.0"
val junitJupiterVersion = "5.11.3"
val tbdLibsVersion = "2024.11.15-09.09-08ca346b"
val mockkVersion = "1.13.12"

repositories {
    val githubPassword: String? by project
    mavenCentral()
    /* ihht. https://github.com/navikt/utvikling/blob/main/docs/teknisk/Konsumere%20biblioteker%20fra%20Github%20Package%20Registry.md
    så plasseres github-maven-repo (med autentisering) før nav-mirror slik at github actions kan anvende førstnevnte.
    Det er fordi nav-mirroret kjører i Google Cloud og da ville man ellers fått unødvendige utgifter til datatrafikk mellom Google Cloud og GitHub
    */
    maven {
        url = uri("https://maven.pkg.github.com/navikt/maven-release")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
    api("org.flywaydb:flyway-database-postgresql:$flywayCoreVersion")
    implementation("com.zaxxer:HikariCP:$hikariCPVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:$tbdLibsVersion")
    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:$tbdLibsVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("skipped", "failed")
        }
    }
    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "MainKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}
kotlin {
    jvmToolchain(21)
}