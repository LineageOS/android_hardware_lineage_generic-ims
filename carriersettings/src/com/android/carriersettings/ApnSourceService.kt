/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.carriersettings

import android.content.ContentValues
import android.provider.Telephony.Carriers
import android.service.carrier.ApnService
import android.util.Log
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_ALL
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_BIP
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_CBS
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_DEFAULT
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_DUN
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_EMERGENCY
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_ENTERPRISE
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_FOTA
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_HIPRI
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_IA
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_IMS
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_MCX
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_MMS
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_OEM_PAID
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_OEM_PRIVATE
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_RCS
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_SUPL
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_UT
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_VSIM
import com.android.carriersettings.ApnItem.ApnType.APN_TYPE_XCAP

class ApnSourceService : ApnService() {
    companion object {
        private const val TAG = "ApnSourceService"
    }

    override fun onRestoreApns(subId: Int): List<ContentValues?> {
        Log.i(TAG, "onRestoreApns")
        val subInfo = CarrierUtils(this).getCurrentCarrierInfo(subId)
        val carrierId = subInfo?.second
        if (carrierId == null) {
            Log.i(TAG, "onRestoreApns: null carrierId for subId $subId, abort.")
            return emptyList()
        }
        val settings = PbConfigLoader.readSettingsFromAssets(subInfo.first)
        if (settings == null) {
            Log.i(TAG, "onRestoreApns: no settings for subId $subId, abort.")
            return emptyList()
        }
        val apns = settings.apns ?: CarrierApns.getDefaultInstance()
        val contentValuesList = mutableListOf<ContentValues>()
        for (apn in apns.apnList) {
            contentValuesList += apn.contentValues(carrierId)
        }
        Log.i(TAG, contentValuesList.toString())
        return contentValuesList
    }
}
