---
author: Guillermo Robles
title: Build and run Kotlin in server side...
featuredImage: assets/media/posts/google-cloud-platform.png
permalink: ':year/:slug'
tags:
- guides
- kotlin
- ktor  
- server-side
- gcloud
draft: false
---
### ...with [Google Cloud Platform](https://cloud.google.com/).


Kotlin is a modern language with a rich syntax that has gained tremendous popularity among Android developers.
At [Google I/O 19](https://developer.android.com/kotlin/first), Google announced that Kotlin will be first platform language in Android.

Kotlin has not been only designed for Android.
It can target different [platforms](https://kotlinlang.org/docs/reference/mpp-supported-platforms.html) such as JVM (Java Virtual Machine), Java script, armv7 and armv8, x64 and x86, and web assembly.

This guide try to show how affordable can be to only use Kotlin (and a bit of YML :) ) to create a back-end service that aligns with [The Twelve-Factor App](https://12factor.net) with a very little learning curve for those who are familiarized with Android development.

First of all, there is this git repository with the [codebase](https://12factor.net/codebase) that illustrate this guide. All [software dependencies](https://12factor.net/dependencies) are declared using [Gradle](https://gradle.org/), the three main dependencies are [Ktor](https://ktor.io/), [Jib](https://github.com/GoogleContainerTools/jib) and [logback](http://logback.qos.ch/). Ktor is a web framework built by and for Kotlin developers. Jib is going to help us to achieve containerization with [docker](https://www.docker.com/) with no need to write any dockerfile. Jib will create a [distroless docker image](https://github.com/GoogleContainerTools/distroless) which only contains our application and its dependencies for runtime. Logback is a logging framework that will stream to [stackdriver](https://cloud.google.com/products/operations) our [application behavior](https://12factor.net/logs).

Moreover [cloud build](https://cloud.google.com/cloud-build/) will provide [continuous deployment](https://12factor.net/build-release-run) to our infrastructure, and [cloud run](https://cloud.google.com/run/) will bring our docker image to a runtime environment which it is fully managed and scales horizontally.



Let's start with Ktor, it provides an intuitive DSL for defining our application routing.

{% highlight 'kotlin' %}

      fun Routing.gets() {
          get("/hello") {
              call.respondText("Hi! ktor is running!")
        }
      }

{% endhighlight %}

This routing is very simple, it defines a GET method in HTTP for the path "/hello", it responds with "Hi! ktor is running!", documentation and learning docs can be found in [ktor official channel](https://ktor.io/learn/).


Jib used a JVm 12 LTS wihch is a distroless, meaning that very light etc.

If you are familiar with Gradle this build configuration will be straightforward, it describes plugin and dependency definitions.
The most important thing to recall is the target compatibility. Jib distroless image is base in OpenJDK 11 which is the last 
long supported version. We need to define the target compatibility in case that build environment is using a higher JVM version. 

{% highlight 'groovy' %}

    buildscript {
        ext.kotlin_version = '1.4.20'
        ext.ktor_version = '1.4.3'
        ext.jib_version = '2.6.0'
        ext.logback_version = '1.2.3'

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
    }
    apply plugin: 'kotlin'
    apply plugin: 'application' // JVM plugin

    mainClassName = "io.ktor.server.jetty.DevelopmentEngine"

    sourceSets {
        main.kotlin.srcDirs = ['src/main/kotlin']
        main.resources.srcDirs = ['src/main/resources']
        test.kotlin.srcDirs = ['src/test/kotlin']
    }

    group 'com.leakingcode'
    version '0.1'

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version"
        implementation "io.ktor:ktor-server-core:$ktor_version"
        implementation "io.ktor:ktor-server-jetty:$ktor_version"
        implementation "ch.qos.logback:logback-classic:$logback_version"
        testImplementation "org.jetbrains.kotlin:kotlin-test"
        testImplementation "org.jetbrains.kotlin:kotlin-test-junit"
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


Cloud build takes a configuration file in Yaml that describes how is the process deliver CI/CD. First step checks the 
application, it runs unit test and lint. Second step uploads the unit test results to static web server. 
In the third step, jib creates an image which will be deployed by executing the last step. 


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
      id: store_unit_test_results
      args: ['-q', 'cp', '-r', '/workspace/build/reports/tests/test/', 'gs://$_BUCKET_NAME/$BRANCH_NAME/$COMMIT_SHA']
      waitFor: ['Check']
    
    - name: 'openjdk:11'
      id: Jib
      args: ['./gradlew', 'check', 'jib', '--image', 'gcr.io/$PROJECT_ID/$BRANCH_NAME:$SHORT_SHA']
    
    - name: 'gcr.io/cloud-builders/gcloud'
      id: Deploy
      args: ['beta', 'run', 'deploy', '$_SERVICE_NAME', '--platform=managed', '--region=europe-west1', '--allow-unauthenticated', '--image=gcr.io/$PROJECT_ID/$BRANCH_NAME:$SHORT_SHA']
    
    artifacts:
    objects:
    location: 'gs://$_BUCKET_NAME/$BRANCH_NAME/$COMMIT_SHA'
    paths: ["'/workspace/output.txt'"]


{% endhighlight %}



## Conclusion

This is an easy approach for software developers who do not want to be concerned about infrastructure, security, deployments, scalability, etc.
Combining Kotlin+Ktor+JibCloud build+Cloud run allows us to have a proper set up for backend services that can be consumed by Android, iOS and web clients.





