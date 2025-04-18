/*
 * Copyright (c) 2023 to present Androidacy and contributors. Names, logos, icons, and the Androidacy name are all trademarks of Androidacy and may not be used without license. See LICENSE for more information.
 */

package com.fox2code.mmm.manager

import com.fox2code.mmm.MainApplication
import com.fox2code.mmm.markdown.MarkdownUrlLinker.Companion.urlLinkify
import com.fox2code.mmm.repo.RepoModule
import com.fox2code.mmm.utils.io.PropUtils
import com.fox2code.mmm.utils.io.net.Http
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.nio.charset.StandardCharsets

class LocalModuleInfo(id: String?) : ModuleInfo(id!!) {
    var updateVersion: String? = null

    var updateVersionCode = Long.MIN_VALUE

    var updateZipUrl: String? = null
    private var updateChangeLogUrl: String? = null

    var remoteModuleInfo: RepoModule? = null
        get() {
            if (field == null) {
                try {
                    field = MainApplication.getInstance().repoModules[id]
                } catch (e: IOException) {
                    // ignore
                }
            }
            return field
        }

    var updateChangeLog = ""

    var updateChecksum: String? = null
    fun checkModuleUpdate() {
        if (updateJson != null && flags and FLAG_MM_REMOTE_MODULE == 0) {
            try {
                val jsonUpdate = JSONObject(
                    String(
                        Http.doHttpGet(
                            updateJson!!, true
                        ), StandardCharsets.UTF_8
                    )
                )
                updateVersion = jsonUpdate.optString("version")
                updateVersionCode = jsonUpdate.getLong("versionCode")
                updateZipUrl = jsonUpdate.getString("zipUrl")
                updateChangeLogUrl = jsonUpdate.optString("changelog")
                try {
                    var desc = String(
                        Http.doHttpGet(
                            updateChangeLogUrl!!, true
                        ), StandardCharsets.UTF_8
                    )
                    if (desc.length > 1000) {
                        desc = desc.substring(0, 1000)
                    }
                    updateChangeLog = desc
                } catch (ioe: Exception) {
                    updateChangeLog = ""
                    updateChecksum = null
                    updateVersion = null
                    updateVersionCode = Long.MIN_VALUE
                }
                updateChecksum = jsonUpdate.optString("checksum")
                val updateZipUrlForReals = updateZipUrl
                if (updateZipUrlForReals!!.isEmpty()) {
                    updateVersion = null
                    updateVersionCode = Long.MIN_VALUE
                    updateZipUrl = null
                    updateChangeLog = ""
                }
                updateVersion = PropUtils.shortenVersionName(
                    updateZipUrlForReals.trim { it <= ' ' }, updateVersionCode
                )
                if (updateChangeLog.length > 1000) updateChangeLog = updateChangeLog.substring(1000)
                updateChangeLog = urlLinkify(updateChangeLog)
            } catch (e: Exception) {
                updateVersion = null
                updateVersionCode = Long.MIN_VALUE
                updateZipUrl = null
                updateChangeLog = ""
                updateChecksum = null
                Timber.w(e, "Failed update checking for module: %s", id)
            }
        }
    }
}