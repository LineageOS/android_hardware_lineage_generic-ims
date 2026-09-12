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
