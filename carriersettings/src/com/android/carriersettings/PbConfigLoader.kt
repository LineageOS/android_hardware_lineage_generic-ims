/*
 * SPDX-FileCopyrightText: The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.carriersettings

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

class PbConfigLoader {
    companion object {
        const val TAG = "PbConfigLoader"
        private val cachedCarriers = mutableMapOf<ExtendedCarrierIdentifier, CarrierSettings>()
        private val cachedSettingsAssets = mutableMapOf<String, CarrierSettings>()

        private fun openPbFile(name: String): FileInputStream {
            Log.i(TAG, "Opening $name")
            return FileInputStream(
                File(
                    Environment.getSystemExtDirectory(),
                    "etc" + File.separator + "CarrierSettings" + File.separator + name + ".pb"
                )
            )
        }

        fun getVersion(): Pair<Long, Long> {
            val listVersion = CarrierList.parseFrom(openPbFile("carrier_list")).version
            val othersVersion = MultiCarrierSettings.parseFrom(openPbFile("others")).version
            return Pair(listVersion, othersVersion)
        }

        fun readSettingsFromAssets(name: String): CarrierSettings? {
            cachedSettingsAssets[name]?.let { return it }

            return runCatching {
                CarrierSettings.parseFrom(openPbFile(name))?.also {
                    cachedSettingsAssets[name] = it
                }
            }.getOrElse { exception ->
                if (exception !is FileNotFoundException) {
                    Log.e(TAG, "", exception)
                    null
                } else runCatching {
                    val multiCarrierSettings = MultiCarrierSettings.parseFrom(openPbFile("others"))
                    for (settings in multiCarrierSettings.settingList) {
                        if (settings.canonicalName == name) {
                            return CarrierSettings.newBuilder()
                                .mergeFrom(settings)
                                .setVersion(multiCarrierSettings.version)
                                .build()
                                .also {
                                    cachedSettingsAssets[name] = it
                                }
                        }
                    }
                    throw Exception("Name not found in others.pb")
                }.getOrElse {
                    Log.e(TAG, "Failed to read settings for $name", it)
                    null
                }
            }
        }

        fun readConfigFromPb(id: ExtendedCarrierIdentifier): CarrierSettings? {
            Log.i(TAG, "readConfigFromPb")
            cachedCarriers[id]?.let {
                Log.i(TAG, "Cache hit!")
                return it
            }
            val carrierList = CarrierList.parseFrom(openPbFile("carrier_list"))
            return carrierList.find(id)?.let { canonicalName ->
                readSettingsFromAssets(canonicalName)?.let {
                    cachedCarriers[id] = it
                    it
                }
            }
        }
    }
}