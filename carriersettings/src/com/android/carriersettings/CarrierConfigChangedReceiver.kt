/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.carriersettings

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log

class CarrierConfigChangedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CCChangedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive")
        val subId = intent.getIntExtra(CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX, -1)
        val slotId = intent.getIntExtra(CarrierConfigManager.EXTRA_SLOT_INDEX, -1)
        if (context.getSystemService(TelephonyManager::class.java)
                .getSimState(slotId) != TelephonyManager.SIM_STATE_READY
        ) {
            Log.i(TAG, "SIM not ready, ignore.")
            return
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId) || slotId < 0) {
            Log.i(TAG, "Invalid subId/slotId, ignore.")
            return
        }

        val resolver = context.contentResolver
        val carrierInfo = CarrierUtils(context).getCurrentCarrierInfo(subId)
        if (carrierInfo == null) {
            Log.i(TAG, "Null CarrierInfo, ignore.")
            return
        }
        val mvno = carrierInfo.second.mvnoTypeForApn
        var selection: String
        var selectArgs = arrayOf<String>()
        if (mvno.first.isEmpty() || mvno.second.isEmpty()) {
            selection = "numeric=? AND mvno_type='' AND edited=${Telephony.Carriers.UNEDITED}"
            selectArgs += carrierInfo.second.mccMnc
        } else {
            selection =
                "numeric=? AND mvno_type=? AND mvno_match_data=? COLLATE NOCASE AND edited=${Telephony.Carriers.UNEDITED}"
            selectArgs = arrayOf(carrierInfo.second.mccMnc, mvno.first, mvno.second)
        }

        Log.i(TAG, "Delete APNs: selection=$selection, selectionArgs=${selectArgs.contentToString()}}")
        Log.i(
            TAG, "Deleted ${
                resolver.delete(
                    Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "delete"),
                    selection,
                    selectArgs
                )
            } rows"
        )

        var contentValues = arrayOf<ContentValues>()
        for (apnItem in PbConfigLoader.readSettingsFromAssets(carrierInfo.first)?.apns?.apnList
            ?: emptyList()) {
            contentValues += apnItem.contentValues(carrierInfo.second)
        }

        Log.i(
            TAG,
            "Inserted ${resolver.bulkInsert(Telephony.Carriers.CONTENT_URI, contentValues)} rows"
        )
    }
}
