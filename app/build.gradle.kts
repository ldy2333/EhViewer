import com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
import com.mikepenz.aboutlibraries.plugin.DuplicateRule.GROUP
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val isRelease: Boolean
    get() = gradle.startParameter.taskNames.any { it.contains("Release") }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.aboutlibrariesPlugin)
    alias(libs.plugins.composeCompilerReportGenerator)
}

android {
    compileSdk = 34
    ndkVersion = "26.2.11394342"

    splits {
        abi {
            isEnable = true
            reset()
            if (isRelease) {
                include("arm64-v8a", "x86_64", "armeabi-v7a", "x86")
                isUniversalApk = true
            } else {
                include("arm64-v8a", "x86_64")
            }
        }
    }

    val signConfig = signingConfigs.create("release") {
        storeFile = File(projectDir.path + "/keystore/androidkey.jks")
        storePassword = "000000"
        keyAlias = "key0"
        keyPassword = "000000"
        enableV3Signing = true
        enableV4Signing = true
    }

    val commitSha = providers.exec {
        commandLine = "git rev-parse --short=7 HEAD".split(' ')
    }.standardOutput.asText.get().trim()

    val buildTime by lazy {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneOffset.UTC)
        formatter.format(Instant.now())
    }

    val repoName = providers.exec {
        commandLine = "git remote get-url origin".split(' ')
    }.standardOutput.asText.get().trim().removePrefix("https://github.com/").removePrefix("git@github.com:")
        .removeSuffix(".git")

    defaultConfig {
        applicationId = "moe.tarsin.ehviewer"
        minSdk = 26
        targetSdk = 34
        versionCode = 180054
        versionName = "1.11.3.5"
        versionNameSuffix = "-cc"
        resourceConfigurations.addAll(
            listOf(
                "zh",
                "zh-rCN",
                "zh-rHK",
                "zh-rTW",
                "es",
                "ja",
                "ko",
                "fr",
                "de",
                "th",
                "tr",
                "nb-rNO",
            ),
        )
        buildConfigField("String", "RAW_VERSION_NAME", "\"$versionName${versionNameSuffix.orEmpty()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"$commitSha\"")
        buildConfigField("String", "REPO_NAME", "\"$repoName\"")
        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    flavorDimensions += "api"

    productFlavors {
        create("default")
        create("marshmallow") {
            minSdk = 22
            applicationIdSuffix = ".m"
            versionNameSuffix = "-M"
        }
    }

    externalNativeBuild {
        cmake {
            path = File("src/main/cpp/CMakeLists.txt")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        freeCompilerArgs = listOf(
            // https://kotlinlang.org/docs/compiler-reference.html#progressive
            "-progressive",
            "-Xjvm-default=all",
            "-Xlambdas=indy",
            "-Xcontext-receivers",

            "-opt-in=coil3.annotation.ExperimentalCoilApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.paging.ExperimentalPagingApi",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=splitties.experimental.ExperimentalSplittiesApi",
            "-opt-in=splitties.preferences.DataStorePreferencesPreview",
        )
    }

    lint {
        disable += setOf("MissingTranslation", "MissingQuantity")
        fatal += setOf("NewApi", "InlinedApi")
    }

    packaging {
        dex {
            useLegacyPackaging = false
        }
    }

    dependenciesInfo.includeInApk = false

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signConfig
            buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "BUILD_TIME", "\"\"")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    namespace = "com.hippo.ehviewer"
}

composeCompiler {
    // https://youtrack.jetbrains.com/issue/KT-67216
    suppressKotlinVersionCompatibilityCheck = libs.versions.kotlin.get()
    enableIntrinsicRemember = true
    enableNonSkippingGroupOptimization = true
    enableExperimentalStrongSkippingMode = true
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.addAll(
            "/META-INF/**",
            "/kotlin/**",
            "**.txt",
            "**.bin",
        )
    }
}

dependencies {
    // https://developer.android.com/jetpack/androidx/releases/activity
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.browser)

    // https://developer.android.com/jetpack/androidx/releases/compose-material3
    api(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.compiler)

    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.constraintlayout.compose)
    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.compose)

    // https://developer.android.com/jetpack/androidx/releases/navigation
    implementation(libs.androidx.navigation.compose)

    // https://developer.android.com/jetpack/androidx/releases/paging
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.recyclerview)

    // https://developer.android.com/jetpack/androidx/releases/room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)

    implementation(libs.androidx.work.runtime)
    implementation(libs.photoview) // Dead Dependency
    implementation(libs.directionalviewpager) // Dead Dependency
    implementation(libs.material)
    implementation(libs.material.motion.core)

    implementation(libs.bundles.splitties)

    // https://square.github.io/okhttp/changelogs/changelog/
    implementation(platform(libs.okhttp.bom))
    implementation(libs.bundles.okhttp)

    implementation(libs.okio.jvm)

    implementation(libs.logcat)

    implementation(libs.diff)

    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.accompanist.drawable.painter)

    implementation(libs.insetter) // Dead Dependency

    implementation(libs.reorderable)

    implementation(platform(libs.arrow.stack))
    implementation(libs.arrow.fx.coroutines)

    // https://coil-kt.github.io/coil/changelog/
    implementation(platform(libs.coil.bom))
    implementation(libs.bundles.coil)

    implementation(libs.telephoto.zoomable)

    implementation(libs.bundles.ktor)

    implementation("com.google.guava:guava:32.0.1-android")

    implementation(libs.bundles.kotlinx.serialization)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.jsoup)

    coreLibraryDesugaring(libs.desugar)

    implementation(libs.cronet.embedded)

    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)
}

kotlin {
    jvmToolchain(21)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
    arg("compose-destinations.codeGenPackageName", "com.hippo.ehviewer.ui")
}

aboutLibraries {
    duplicationMode = MERGE
    duplicationRule = GROUP
}

spotless {
    kotlin {
        // https://github.com/diffplug/spotless/issues/111
        target("src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}
