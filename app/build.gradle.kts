plugins {
    id("org.fcitx.fcitx5.android.app-convention")
    id("org.fcitx.fcitx5.android.native-app-convention")
    id("org.fcitx.fcitx5.android.build-metadata")
    id("org.fcitx.fcitx5.android.data-descriptor")
    id("org.fcitx.fcitx5.android.fcitx-component")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val packageBase = "org.fcitx.fcitx5.android"
val appIdBase = packageBase
val appIdPtSuffix = ".pt"
val flavorPt = "pt"
val appLabelDefault = "@string/app_name"
val originalPluginManifestAction = "$packageBase.plugin.MANIFEST"
val originalDebugPluginManifestAction = "$packageBase.debug.plugin.MANIFEST"
val originalIpcAction = "$packageBase.IPC"
val originalDebugIpcAction = "$packageBase.debug.IPC"
val imeSettingsActivity = "$packageBase.ui.main.MainActivity"

android {
    namespace = packageBase

    defaultConfig {
        applicationId = appIdBase
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = appLabelDefault
        manifestPlaceholders["originalPluginManifestAction"] = originalPluginManifestAction
        manifestPlaceholders["originalDebugPluginManifestAction"] = originalDebugPluginManifestAction
        manifestPlaceholders["originalIpcAction"] = originalIpcAction
        manifestPlaceholders["originalDebugIpcAction"] = originalDebugIpcAction
        buildConfigField("String", "ORIGINAL_PLUGIN_MANIFEST_ACTION", "\"$originalPluginManifestAction\"")
        buildConfigField("String", "ORIGINAL_DEBUG_PLUGIN_MANIFEST_ACTION", "\"$originalDebugPluginManifestAction\"")
        resValue("string", "ime_settings_activity", imeSettingsActivity)

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                targets(
                    // jni
                    "native-lib",
                    // copy fcitx5 built-in addon libraries
                    "copy-fcitx5-modules",
                    // android specific modules
                    "androidfrontend",
                    "androidkeyboard",
                    "androidnotification"
                )
            }
        }
    }

    flavorDimensions += "brand"
    productFlavors {
        create(flavorPt) {
            dimension = "brand"
            applicationIdSuffix = appIdPtSuffix
            buildConfigField("boolean", "IS_PT_BUILD", "true")
        }
    }

    buildFeatures {
        viewBinding = true
        resValues = true
    }

    buildTypes {
        release {
            resValue("mipmap", "app_icon", "@mipmap/ic_launcher")
            resValue("mipmap", "app_icon_round", "@mipmap/ic_launcher_round")
            resValue("string", "app_name", "@string/app_name_release")
            proguardFile("proguard-rules.pro")
        }
        debug {
            resValue("mipmap", "app_icon", "@mipmap/ic_launcher_debug")
            resValue("mipmap", "app_icon_round", "@mipmap/ic_launcher_round_debug")
            resValue("string", "app_name", "@string/app_name_debug")
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

    fun fallbackAliasFromPtTask(taskName: String): String? = when {
        "PtDebug" in taskName -> taskName.replace("PtDebug", "Debug")
        "PtRelease" in taskName -> taskName.replace("PtRelease", "Release")
        else -> null
}

afterEvaluate {
    val ptTasks = tasks.names
        .filter { it.contains("PtDebug") || it.contains("PtRelease") }
        .toList()
    ptTasks.forEach { ptTaskName ->
        val alias = fallbackAliasFromPtTask(ptTaskName) ?: return@forEach
        if (tasks.findByName(alias) != null) return@forEach
        val ptTask = tasks.findByName(ptTaskName) ?: return@forEach
        tasks.register(alias) {
            group = ptTask.group
            description = "Alias of $ptTaskName"
            dependsOn(ptTaskName)
        }
    }
}

fun String.capitalized(): String = replaceFirstChar { c ->
    if (c.isLowerCase()) c.titlecase() else c.toString()
}

fun registerPtApkCompatCopy(buildType: String) {
    val buildTypeCap = buildType.capitalized()
    val assembleTask = "assemblePt$buildTypeCap"
    val compatTask = "syncPt${buildTypeCap}ApkToLegacyDir"
    tasks.register(compatTask) {
        dependsOn(assembleTask)
        doLast {
            // clear old APKs in target folder to avoid confusion (delete only .apk files)
            val destDir = layout.buildDirectory.dir("outputs/apk/$buildType").get().asFile
            if (destDir.exists()) {
                destDir.listFiles()?.filter { it.isFile && it.extension == "apk" }?.forEach { file ->
                    try {
                        file.delete()
                    } catch (_: Exception) {
                        // ignore deletion failures
                    }
                }
            } else {
                destDir.mkdirs()
            }

            // copy new pt APKs into legacy location
            copy {
                from(layout.buildDirectory.dir("outputs/apk/pt/$buildType"))
                into(destDir)
                include("*.apk")
            }
        }
    }
    tasks.matching { it.name == assembleTask }.configureEach {
        finalizedBy(compatTask)
    }
}

registerPtApkCompatCopy("debug")
registerPtApkCompatCopy("release")

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                output.outputFileName.set(
                    output.outputFileName.get()
                        .replace("org.fcitx.fcitx5.android-", "org.fcitx.fcitx5.android.pt-")
                        .replace("-pt-", "-")
                )
            }
        }
    }
}

fcitxComponent {
    includeLibs = listOf(
        "fcitx5",
        "fcitx5-lua",
        "libime",
        "fcitx5-chinese-addons"
    )
    // exclude (delete immediately after install) tables that nobody would use
    excludeFiles = listOf("cangjie", "erbi", "qxm", "wanfeng").map {
        "usr/share/fcitx5/inputmethod/$it.conf"
    }
    installPrebuiltAssets = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    ksp(project(":codegen"))
    implementation(project(":lib:fcitx5"))
    implementation(project(":lib:fcitx5-lua"))
    implementation(project(":lib:libime"))
    implementation(project(":lib:fcitx5-chinese-addons"))
    implementation(project(":lib:common"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.autofill)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.paging)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
    implementation(libs.arrow.core)
    implementation(libs.arrow.functions)
    implementation(libs.imagecropper)
    implementation(libs.flexbox)
    implementation(libs.dependency)
    implementation(libs.timber)
    implementation(libs.splitties.bitflags)
    implementation(libs.splitties.dimensions)
    implementation(libs.splitties.resources)
    implementation(libs.splitties.views.dsl)
    implementation(libs.splitties.views.dsl.appcompat)
    implementation(libs.splitties.views.dsl.constraintlayout)
    implementation(libs.splitties.views.dsl.coordinatorlayout)
    implementation(libs.splitties.views.dsl.recyclerview)
    implementation(libs.splitties.views.recyclerview)
    implementation(libs.aboutlibraries.core)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)
    implementation(libs.xz)
    implementation(libs.pictureselector)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.lifecycle.testing)
    androidTestImplementation(libs.junit)
}

configurations {
    all {
        // remove Baseline Profile Installer or whatever it is...
        exclude(group = "androidx.profileinstaller", module = "profileinstaller")
        // remove unwanted splitties libraries...
        exclude(group = "com.louiscad.splitties", module = "splitties-appctx")
        exclude(group = "com.louiscad.splitties", module = "splitties-systemservices")
    }
}
