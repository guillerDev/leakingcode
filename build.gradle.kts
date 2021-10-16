plugins {
    kotlin("jvm")  version "1.5.31"
    id ("com.eden.orchidPlugin") version "0.21.1"
}

group = "com.leakingcode.blog"
version = "1.1"

repositories {
    jcenter()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    val orchidVersion = "0.21.1"
    orchidImplementation("io.github.javaeden.orchid:OrchidBlog:$orchidVersion")
    orchidImplementation("io.github.javaeden.orchid:OrchidPluginDocs:$orchidVersion")
    orchidImplementation("io.github.javaeden.orchid:OrchidPosts:$orchidVersion")
    orchidImplementation("io.github.javaeden.orchid:OrchidFutureImperfect:$orchidVersion")
    orchidImplementation("io.github.javaeden.orchid:OrchidSearch:$orchidVersion")
    orchidRuntimeOnly("io.github.javaeden.orchid:OrchidSyntaxHighlighter:$orchidVersion")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

fun envOrProperty(name: String, required: Boolean = false): String? {
    val result = project.findProperty(name) as? String ?: System.getenv(name)
    check(result != null || required.not()) { "Missing required environment property:\n  export $name=\"...\"" }
    return result
}

orchid {
    val isProd = envOrProperty("ENV") == "prod"
    theme = "FutureImperfect"
    version = "${project.version}"
    environment = if (isProd) "production" else "debug"
    baseUrl = when(envOrProperty("ENV")) {
        "prod" -> envOrProperty("BASE_URL", required = true)
        "dev" -> envOrProperty("BASE_URL", required = true)
        else -> "http://localhost:8080"
    }
    srcDir = envOrProperty("SRC_DIR")
    destDir = envOrProperty("DEST_DIR")
}
