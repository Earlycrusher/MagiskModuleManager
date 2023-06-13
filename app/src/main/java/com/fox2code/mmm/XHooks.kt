package com.fox2code.mmm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.WebView
import androidx.annotation.Keep
import com.fox2code.mmm.manager.ModuleManager
import com.fox2code.mmm.repo.RepoManager

/**
 * Class made to expose some manager functions to xposed modules.
 * It will not be obfuscated on release builds
 */
@Suppress("UNUSED_PARAMETER")
@Keep
enum class XHooks {
    ;

    companion object {
        @JvmStatic
        @Keep
        fun onRepoManagerInitialize() {
            // Call addXRepo here if you are an XPosed module
        }

        @JvmStatic
        @Keep
        fun onRepoManagerInitialized() {
        }

        @JvmStatic
        @Keep
        fun isModuleActive(moduleId: String?): Boolean {
            return ModuleManager.isModuleActive(moduleId!!)
        }

        @Suppress("DEPRECATION")
        @JvmStatic
        @Keep
        @Throws(PackageManager.NameNotFoundException::class)
        fun checkConfigTargetExists(context: Context, packageName: String, config: String) {
            if ("org.lsposed.manager" == config && "org.lsposed.manager" == packageName &&
                (isModuleActive("riru_lsposed") || isModuleActive("zygisk_lsposed"))
            ) return  // Skip check for lsposed as it is probably injected into the system.
            context.packageManager.getPackageInfo(packageName, 0)
        }

        @Suppress("UNUSED_PARAMETER")
        @JvmStatic
        @Keep
        fun getConfigIntent(context: Context, packageName: String?, config: String?): Intent? {
            return context.packageManager.getLaunchIntentForPackage(packageName!!)
        }

        @JvmStatic
        @Keep
        fun onWebViewInitialize(webView: WebView?, allowInstall: Boolean) {
            if (webView == null) throw NullPointerException("WebView is null!")
        }

        @Keep
        fun addXRepo(url: String?, fallbackName: String?): XRepo {
            return RepoManager.getINSTANCE_UNSAFE().addOrGet(url, fallbackName)
        }

        @Keep
        fun getXRepo(url: String?): XRepo {
            return RepoManager.getINSTANCE_UNSAFE()[url]
        }

        @get:Keep
        val xRepos: Collection<XRepo>
            get() = RepoManager.getINSTANCE_UNSAFE().xRepos
    }
}