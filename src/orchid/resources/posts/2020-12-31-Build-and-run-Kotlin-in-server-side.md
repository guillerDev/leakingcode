---
author: Guillermo Robles title: Build and run Kotlin in server side... featuredImage:
assets/media/posts/google-cloud-platform.png permalink: ':year/:slug' tags:

- guides
- kotlin
- ktor
- server-side
- gcloud

draft: false

---

### ...with [Google Cloud Platform](https://cloud.google.com/).

Kotlin is a modern language with a rich syntax that has gained tremendous popularity among Android developers in the
last years. At [Google I/O 19](https://developer.android.com/kotlin/first), Google announced that Kotlin will be first
platform language in Android.

Kotlin has not been only designed for Android. Actually, it can target
different [platforms](https://kotlinlang.org/docs/reference/mpp-supported-platforms.html)
such as JVM (Java Virtual Machine), JavaScript, armv7 and armv8, x64 and x86, web assembly among other targets.

In this post, I am going to show how affordable is to develop with Kotlin and a bit of YML :) a back-end service that
follows [The Twelve-Factor App](https://12factor.net) with a very little learning curve for those developers who are
familiarized already with Kotlin.

Firstly, in my [git repository](https://github.com/guillerDev/gcloudrundemo)
has [codebase](https://12factor.net/codebase) that illustrates this showcase.

All [software dependencies](https://12factor.net/dependencies) are declared using a build automation tool
like [Gradle](https://gradle.org/), three main dependencies are [Ktor](https://ktor.io/)
, [Jib](https://github.com/GoogleContainerTools/jib) and
[logback](http://logback.qos.ch/). Ktor is a web framework built by and for Kotlin developers. Jib is going to help us
to achieve containerization with [docker](https://www.docker.com/) with no need to write a dockerfile. Jib will create
a [distroless docker image](https://github.com/GoogleContainerTools/distroless)
which only contains our application and its dependencies for runtime environment. Logback is a logging framework that
will stream to [stackdriver](https://cloud.google.com/products/operations)
our [application behavior](https://12factor.net/logs).

Moreover [cloud build](https://cloud.google.com/cloud-build/) will provide
[continuous deployment](https://12factor.net/build-release-run) to our infrastructure and
[cloud run](https://cloud.google.com/run/) will bring our docker image to a runtime environment which it is fully
managed, and it auto-scales horizontally.

Ktor provides an intuitive DSL for defining our application routing.

{% highlight 'kotlin' %}

      fun Routing.gets() {
          get("/hello") {
              call.respondText("Hi! ktor is running!")
        }
      }

{% endhighlight %}

This routing is very simple, it defines a GET method in HTTP for path "/hello", it responds in plain text with
"Hi! ktor is running!", you can try out [here](https://ktor-t4xwpg5bfq-ew.a.run.app/hello) More extended documentation
and learning material can be found in [Ktor official channel](https://ktor.io/learn/).

If you have worked with Gradle before, this build configuration will seem straightforward for you. It describes plugins
and dependency definitions. The most important thing to recall is JVM target compatibility. Jib distroless image is base
in OpenJDK 11 which is the last long support version. We need to define the target compatibility in case that build
environment is using a higher JVM version. Also, Jib needs to have a reference for an entry point to start up the
container, I have defined mainClass which is ***io.ktor.server.jetty.DevelopmentEngine***

{% highlight 'groovy' %}

    buildscript {
        ext.kotlin_version = '1.5.31'
        ext.ktor_version = '1.6.4'
        ext.jib_version = '3.1.4'
        ext.logback_version = '1.2.6'

        repositories {
            jcenter()
            mavenCentral()
        }
        dependencies {
            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
        }
    }
    plugins {
        id "com.google.cloud.tools.jib" version "1.8.0"
        id "org.jmailen.kotlinter" version "3.2.0"
        id "org.jetbrains.kotlin.jvm" version "1.5.31"
    }
    apply plugin: 'application' // JVM plugin

    mainClassName = "io.ktor.server.jetty.DevelopmentEngine"

    sourceSets {
        main.kotlin.srcDirs = ['src/main/kotlin']
        main.resources.srcDirs = ['src/main/resources']
        test.kotlin.srcDirs = ['src/test/kotlin']
    }

    group 'com.leakingcode'
    version '0.2'

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation "io.ktor:ktor-server-core:$ktor_version"
        implementation "io.ktor:ktor-server-jetty:$ktor_version"
        implementation "ch.qos.logback:logback-classic:$logback_version"

        testImplementation "org.jetbrains.kotlin:kotlin-test"
        testImplementation "org.jetbrains.kotlin:kotlin-test-junit"
        testImplementation "io.ktor:ktor-server-test-host:$ktor_version"
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }

    // base jib is targeting distroless jdk 11 gcr.io/distroless/java:11
    targetCompatibility = 11

    jib {
        container {
            mainClass = mainClassName
        }

{% endhighlight %}

Cloud build takes a configuration file in Yaml that describes how is CI/CD (Continuous integration and continuous
delivery). First step checks the application, it runs unit test and lint. Second step uploads the unit test results to
static web server. In the third step, jib creates an image which will be deployed by executing the last step.

{% highlight 'yaml' %}

    steps:
      - name: 'openjdk:11'
        id: Check
        args: ['sh', 'check.sh']
        env:
          - 'REPO_NAME=$REPO_NAME'
          - 'PROJECT_ID=$PROJECT_ID'
          - 'COMMIT_SHA=$COMMIT_SHA'
          - 'BRANCH_NAME=$BRANCH_NAME'
          - 'BUCKET_NAME=$_BUCKET_NAME'
    
      - name: 'gcr.io/cloud-builders/gsutil'
        id: Store_unit_test_results
        args: ['-q', 'cp', '-r', '/workspace/build/reports/tests/test/',
        'gs://$_BUCKET_NAME/$BRANCH_NAME/$COMMIT_SHA']
        waitFor: ['Check']
    
      - name: 'openjdk:11'
        id: Jib
        args: ['./gradlew', 'check', 'jib', '--image', 'gcr.io/$PROJECT_ID/$BRANCH_NAME:$SHORT_SHA']
    
      - name: 'gcr.io/cloud-builders/gcloud'
        id: Deploy
        args: ['beta', 'run', 'deploy', '$_SERVICE_NAME',
        '--platform=managed', '--region=europe-west1',
        '--allow-unauthenticated', '--image=gcr.io/$PROJECT_ID/$BRANCH_NAME:$SHORT_SHA']
    
    artifacts:
      objects:
        location: 'gs://$_BUCKET_NAME/$BRANCH_NAME/$COMMIT_SHA'
        paths: ["'/workspace/output.txt'"]

{% endhighlight %}

To make Gcloud Build work and deploy to Gcloud Run, it needs to
have a [trigger](https://cloud.google.com/build/docs/automating-builds/create-manage-triggers). I have set up a trigger
that executes for every push to master in my [repo](https://github.com/guillerDev/gcloudrundemo).

## Conclusion

This is an easy approach for software developers who do not want to be concerned about infrastructure, security,
deployments and scalability. Just with few lines of code is possible to set up a decent infrastructure at Google Cloud,
which is ready to deploy for every single commit into a git repository.





