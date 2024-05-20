plugins {
    id("java")
    id("application")
}

group = "com.github.igormich.simple_plugin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junitupiter")
    annotationProcessor(project("plugin"))
    compileOnly(project("plugin"))
    //compileOnly(files("F:\\java\\CompilerPlugin\\plugin\\build\\libs\\plugin.jar"))
    //annotationProcessor(files("F:\\java\\CompilerPlugin\\plugin\\build\\libs\\plugin.jar"))
}

tasks.test {
    useJUnitPlatform()
}
application {
    mainClass = "com.github.igormich.simple_plugin.Main"
}

tasks.compileJava {
    options.isFork = true
    options.forkOptions.jvmArgs?.plusAssign(
        listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
            "--add-exports", "java.prefs/java.util.prefs=ALL-UNNAMED",
        )
    )
}