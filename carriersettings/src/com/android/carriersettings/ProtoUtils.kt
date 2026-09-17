/*
 * SPDX-FileCopyrightText: The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.carriersettings

import android.content.ContentValues
import android.os.PersistableBundle
import android.provider.Telephony.Carriers
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
import com.android.carriersettings.ApnItem.Protocol.PROTOCOL_IP
import com.android.carriersettings.ApnItem.Protocol.PROTOCOL_IPV4V6
import com.android.carriersettings.ApnItem.Protocol.PROTOCOL_IPV6
import com.android.carriersettings.ApnItem.Protocol.PROTOCOL_NON_IP
import com.android.carriersettings.ApnItem.Protocol.PROTOCOL_UNSTRUCTURED
import com.android.carriersettings.ApnItem.Protocol.UNRECOGNIZED
import com.android.carriersettings.CarrierConfig.Config.ValueCase.BOOLEAN_VALUE
import com.android.carriersettings.CarrierConfig.Config.ValueCase.BUNDLE_VALUE
import com.android.carriersettings.CarrierConfig.Config.ValueCase.DOUBLE_VALUE
import com.android.carriersettings.CarrierConfig.Config.ValueCase.INTEGER_ARRAY_VALUE
import com.android.carriersettings.CarrierConfig.Config.ValueCase.INTEGER_VALUE
import com.android.carriersettings.CarrierConfig.Config.ValueCase.LONG_VALUE
import com.android.carriersettings.CarrierConfig.Config.ValueCase.STRING_ARRAY_VALUE
import com.android.carriersettings.CarrierConfig.Config.ValueCase.STRING_VALUE
import com.android.carriersettings.CarrierId.MvnoDataCase.GID
import com.android.carriersettings.CarrierId.MvnoDataCase.ICCID
import com.android.carriersettings.CarrierId.MvnoDataCase.IMSI
import com.android.carriersettings.CarrierId.MvnoDataCase.MVNODATA_NOT_SET
import com.android.carriersettings.CarrierId.MvnoDataCase.SPN

private const val TAG = "ProtoUtils"

fun CarrierList.find(id: ExtendedCarrierIdentifier): Pair<String, CarrierId>? {
    for (carrierMap in entryList) {
        val carrierId = carrierMap.carrierId
        if (id.mcc + id.mnc == carrierId.mccMnc) {
            Log.i(TAG, "Checking carrier ${carrierId.mccMnc}")
            when (carrierId.mvnoDataCase) {
                SPN -> {
                    id.spn?.let {
                        if (it.equals(
                                carrierId.spn, ignoreCase = true
                            )
                        ) {
                            Log.i(TAG, "Carrier ${carrierId.mccMnc} matched!")
                            return Pair(carrierMap.canonicalName, carrierId)
                        }
                    }
                }

                IMSI -> {
                    id.imsi?.let {
                        if (it.matches(
                                carrierId.imsi.replace("[xX]*$", "[0-9]*")
                                    .replace("[xX]", "[0-9]")
                                    .toRegex()
                            )
                        ) {
                            Log.i(TAG, "Carrier ${carrierId.mccMnc} matched!")
                            return Pair(carrierMap.canonicalName, carrierId)
                        }
                    }
                }

                GID -> {
                    id.gid1?.let {
                        val gid = carrierId.gid
                        if (it.length >= gid.length && gid.equals(
                                it.substring(0, gid.length), ignoreCase = true
                            )
                        ) {
                            Log.i(TAG, "Carrier ${carrierId.mccMnc} matched!")
                            return Pair(carrierMap.canonicalName, carrierId)
                        }
                    }
                }

                ICCID -> if (id.iccId.startsWith(carrierId.iccid)) {
                    Log.i(TAG, "Carrier ${carrierId.mccMnc} matched!")
                    return Pair(carrierMap.canonicalName, carrierId)
                }

                MVNODATA_NOT_SET -> return Pair(carrierMap.canonicalName, carrierId)
            }
        }
    }
    return null
}

fun CarrierConfig.toBundle(): PersistableBundle {
    val bundle = PersistableBundle()
    for (config in configList) {
        Log.i(TAG, "Adding key ${config.key}")
        when (config.valueCase) {
            STRING_VALUE -> bundle.putString(config.key, config.stringValue)
            INTEGER_VALUE -> bundle.putInt(config.key, config.integerValue)
            LONG_VALUE -> bundle.putLong(config.key, config.longValue)
            BOOLEAN_VALUE -> bundle.putBoolean(config.key, config.booleanValue)
            DOUBLE_VALUE -> bundle.putDouble(config.key, config.doubleValue)
            STRING_ARRAY_VALUE -> {
                bundle.putStringArray(
                    config.key, arrayOf<String>() + config.stringArrayValue.itemList
                )
            }

            INTEGER_ARRAY_VALUE -> {
                bundle.putIntArray(config.key, intArrayOf() + config.integerArrayValue.itemList)
            }

            BUNDLE_VALUE -> bundle.putPersistableBundle(config.key, config.bundleValue.toBundle())
            else -> {}
        }
    }
    return bundle
}

val CarrierId.mvnoTypeForApn: Pair<String, String>
    get() = Pair(
        when (mvnoDataCase) {
            SPN -> "spn"
            IMSI -> "imsi"
            GID -> "gid"
            // Google falls back to the default in this case for some reason,
            // should we do it too?
            ICCID -> "iccid"
            else -> ""
        }, when (mvnoDataCase) {
            SPN -> spn
            IMSI -> imsi
            GID -> gid
            // Google falls back to the default in this case for some reason,
            // should we do it too?
            ICCID -> iccid
            else -> ""
        }
    )

fun ApnItem.contentValues(carrierId: CarrierId): ContentValues {
    val contentValues = ContentValues()
    contentValues.put(Carriers.APN, value)
    contentValues.put(Carriers.NAME, name)
    contentValues.put(Carriers.MCC, carrierId.mccMnc.substring(0, 3))
    contentValues.put(Carriers.MNC, carrierId.mccMnc.substring(3))
    contentValues.put(Carriers.NUMERIC, carrierId.mccMnc)
    val mvno = carrierId.mvnoTypeForApn
    contentValues.put(Carriers.MVNO_TYPE, mvno.first)
    contentValues.put(Carriers.MVNO_MATCH_DATA, mvno.second)
    contentValues.put(Carriers.EDITED_STATUS, Carriers.UNEDITED)
    contentValues.put(
        Carriers.TYPE,
        typeList.joinToString(separator = ",") {
            when (it) {
                APN_TYPE_ALL -> "*"
                APN_TYPE_DEFAULT -> "default"
                APN_TYPE_MMS -> "mms"
                APN_TYPE_SUPL -> "supl"
                APN_TYPE_DUN -> "dun"
                APN_TYPE_HIPRI -> "hipri"
                APN_TYPE_FOTA -> "fota"
                APN_TYPE_IMS -> "ims"
                APN_TYPE_CBS -> "cbs"
                APN_TYPE_IA -> "ia"
                APN_TYPE_EMERGENCY -> "emergency"
                APN_TYPE_XCAP -> "xcap"
                APN_TYPE_UT -> "ut"
                APN_TYPE_RCS -> "rcs"
                APN_TYPE_MCX -> "mcx"
                APN_TYPE_VSIM -> "vsim"
                APN_TYPE_BIP -> "bip"
                APN_TYPE_ENTERPRISE -> "enterprise"
                APN_TYPE_OEM_PAID -> "oem_paid"
                APN_TYPE_OEM_PRIVATE -> "oem_private"
                else -> ""
            }
        })
    contentValues.put(Carriers.PROTOCOL, protocol.stringForApn())
    contentValues.put(Carriers.ROAMING_PROTOCOL, roamingProtocol.stringForApn())
    contentValues.put(Carriers.SERVER, server)
    contentValues.put(Carriers.PROXY, proxy)
    contentValues.put(Carriers.PORT, port)
    contentValues.put(Carriers.USER, user)
    contentValues.put(Carriers.PASSWORD, password)
    contentValues.put(Carriers.AUTH_TYPE, authType)
    contentValues.put(Carriers.MMSC, mmsc)
    contentValues.put(Carriers.MMSPROXY, mmscProxy)
    contentValues.put(Carriers.MMSPORT, mmscProxyPort)
    contentValues.put(
        Carriers.BEARER_BITMASK,
        if (hasBearerBitMask()) bearerBitMask.toBitMask() else 0
    )
    contentValues.put(Carriers.MTU, mtu)
    if (hasProfileId())
        contentValues.put(Carriers.PROFILE_ID, profileId)
    contentValues.put(Carriers.MAX_CONNECTIONS, maxConns)
    contentValues.put(Carriers.WAIT_TIME_RETRY, waitTime)
    contentValues.put(Carriers.TIME_LIMIT_FOR_MAX_CONNECTIONS, maxConnsTime)
    contentValues.put(Carriers.MODEM_PERSIST, modemCognitive)
    contentValues.put(Carriers.USER_VISIBLE, userVisible)
    contentValues.put(Carriers.USER_EDITABLE, userEditable)
    contentValues.put(Carriers.APN_SET_ID, apnSetId)
    contentValues.put(
        Carriers.SKIP_464XLAT, when (skip464Xlat) {
            ApnItem.Skip464XLAT.SKIP464XLAT_DEFAULT -> Carriers.SKIP_464XLAT_DEFAULT
            ApnItem.Skip464XLAT.SKIP464XLAT_DISABLE -> Carriers.SKIP_464XLAT_DISABLE
            ApnItem.Skip464XLAT.SKIP464XLAT_ENABLE -> Carriers.SKIP_464XLAT_ENABLE
            ApnItem.Skip464XLAT.UNRECOGNIZED -> throw IllegalArgumentException("Unknown Skip464XLAT enum value")
        }
    )
    contentValues.put(
        Carriers.LINGERING_NETWORK_TYPE_BITMASK,
        if (hasLingeringNetworkTypeBitmask()) lingeringNetworkTypeBitmask.toBitMask() else 0
    )
    contentValues.put(Carriers.ALWAYS_ON, alwaysOn)
    // The following two values are under a condition in Google's CarrierSettings, but both
    // of those are true when the Android version is at least V (which we can guarantee).
    contentValues.put(Carriers.INFRASTRUCTURE_BITMASK, infrastructureBitmask)
    contentValues.put(Carriers.ESIM_BOOTSTRAP_PROVISIONING, esimBootstrapProvisioning)
    return contentValues
}

fun ApnItem.Protocol.stringForApn(): String {
    return when (this) {
        PROTOCOL_IP -> "IP"
        PROTOCOL_IPV6 -> "IPV6"
        PROTOCOL_IPV4V6 -> "IPV4V6"
        PROTOCOL_NON_IP -> "NON-IP"
        PROTOCOL_UNSTRUCTURED -> "UNSTRUCTURED"
        UNRECOGNIZED -> throw IllegalArgumentException("Unknown Protocol enum value")
    }
}

fun String.toBitMask(): Int {
    var value = 0
    split("|").forEach {
        val mask = it.toInt()
        if (mask > 0) value = value or (1 shl (mask - 1))
    }
    return value
}