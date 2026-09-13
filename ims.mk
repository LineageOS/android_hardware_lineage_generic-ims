#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

GENERIC_IMS_PATH := hardware/lineage/generic-ims

# CarrierSettings
PRODUCT_PACKAGES += CarrierSettings

# IMS
PRODUCT_PACKAGES += \
    ImsStack \
    Iwlan \
    QualifiedNetworksService

$(call inherit-product, packages/modules/ImsMedia/imsmedia.mk)

# Overlay
PRODUCT_PACKAGES += \
    FrameworkResOverlayIms \
    TelephonyOverlayIms

# Permissions
PRODUCT_PACKAGES += android.hardware.telephony.ims.prebuilt.xml

# SEPolicy
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(GENERIC_IMS_PATH)/sepolicy/system_ext/private
