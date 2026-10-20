/*
 * SPDX-FileCopyrightText: The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.carriersettings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager

class BootCompletedReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            Log.e(TAG, "context is null!")
            return
        }
        var shouldReloadConfigs = false
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        // Check the version of the protobuf configs
        val savedListVersion = preferences.getLong("list_version", 0)
        val savedOthersVersion = preferences.getLong("others_version", 0)
        val currentVersion = PbConfigLoader.getVersion()
        if (currentVersion.first > savedListVersion || currentVersion.second > savedOthersVersion) {
            Log.i(
                TAG,
                "Carrier settings updated! list: $savedListVersion -> ${currentVersion.first}, " +
                        "others: $savedOthersVersion -> ${currentVersion.second}"
            )
            shouldReloadConfigs = true
        }

        // Check the hash of the override
        // Internally, Resources.getXml() uses a string type, so use that here as we can't directly
        // hash the XML object nor the PersistableBundle it represents.
        val currentOverrideHash =
            context.resources.getString(R.xml.carrier_config_override).hashCode()
        val savedOverrideHash = preferences.getInt("override_hash", 0)
        if (currentOverrideHash != savedOverrideHash) {
            Log.i(
                TAG,
                "Carrier config override changed! hash: $savedOverrideHash -> $currentOverrideHash"
            )
            shouldReloadConfigs = true
        }

        if (shouldReloadConfigs) {
            val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)
            context.getSystemService(SubscriptionManager::class.java).activeSubscriptionIdList.forEach {
                carrierConfigManager.notifyConfigChangedForSubId(it)
            }
            preferences.edit {
                putLong("list_version", currentVersion.first)
                putLong("others_version", currentVersion.second)
                putInt("override_hash", currentOverrideHash)
            }
        }
    }
}