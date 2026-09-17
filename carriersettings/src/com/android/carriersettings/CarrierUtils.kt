/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.carriersettings

import android.content.Context
import android.service.carrier.CarrierIdentifier
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

class CarrierUtils(private val context: Context) {
    fun getCurrentCarrierInfo(subId: Int): Pair<String,CarrierId>? {
        val telephonyManager =
            context.getSystemService(TelephonyManager::class.java).createForSubscriptionId(subId)
        val operator = telephonyManager.simOperator
        val mcc = operator.substring(0..2)
        val mnc = operator.substring(3)
        val id = ExtendedCarrierIdentifier(
            id = CarrierIdentifier(
                mcc, mnc,
                telephonyManager.simOperatorName,
                telephonyManager.subscriberId,
                telephonyManager.groupIdLevel1,
                "",
            ),
            iccId = context.getSystemService(SubscriptionManager::class.java)
                .getActiveSubscriptionInfo(subId).iccId
        )
        return PbConfigLoader.getCarrierId(id)
    }
}
