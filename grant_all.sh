#!/system/bin/sh
# ==============================================================================
# PermissionLab: Instant Privileged Permission Grant Script
# Run directly in root terminal (su) or via ADB shell:
#   adb shell < grant_all.sh
# ==============================================================================

PKG="com.permissionlab.app"
DEBUG_PKG="com.permissionlab.app.debug"

# Determine target package (detect if debug build is installed)
if pm list packages | grep -q "$DEBUG_PKG"; then
    TARGET_PKG="$DEBUG_PKG"
else
    TARGET_PKG="$PKG"
fi

echo "=========================================================="
echo " Granting Privileged & Special Permissions to: $TARGET_PKG"
echo "=========================================================="

# 1. Write Secure Settings (Requires ADB or Root)
echo "[1/7] Granting WRITE_SECURE_SETTINGS..."
pm grant "$TARGET_PKG" android.permission.WRITE_SECURE_SETTINGS 2>/dev/null || true

# 2. Post Notifications (Android 13+)
echo "[2/7] Granting POST_NOTIFICATIONS..."
pm grant "$TARGET_PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null || true

# 3. Usage Stats (AppOps)
echo "[3/7] Granting PACKAGE_USAGE_STATS via AppOps..."
appops set "$TARGET_PKG" GET_USAGE_STATS allow 2>/dev/null || true

# 4. Android 13+ Restricted Settings Unlock (AppOps 119)
echo "[4/7] Unlocking ACCESS_RESTRICTED_SETTINGS..."
appops set "$TARGET_PKG" ACCESS_RESTRICTED_SETTINGS allow 2>/dev/null || true

# 5. Overlay and System Settings
echo "[5/7] Granting Overlay & Write System Settings..."
appops set "$TARGET_PKG" SYSTEM_ALERT_WINDOW allow 2>/dev/null || true
appops set "$TARGET_PKG" WRITE_SETTINGS allow 2>/dev/null || true
appops set "$TARGET_PKG" MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true

# 6. Notification Listener Service
echo "[6/7] Enabling Notification Listener Service..."
cmd notification allow_listener "$TARGET_PKG/com.permissionlab.app.services.LabNotificationListenerService" 2>/dev/null || true

# 7. Accessibility Service
echo "[7/7] Enabling Accessibility Service..."
CURRENT_SERVICES=$(settings get secure enabled_accessibility_services)
ACC_SERVICE="$TARGET_PKG/com.permissionlab.app.services.LabAccessibilityService"
if [ -z "$CURRENT_SERVICES" ] || [ "$CURRENT_SERVICES" = "null" ]; then
    settings put secure enabled_accessibility_services "$ACC_SERVICE"
else
    case "$CURRENT_SERVICES" in
        *"$ACC_SERVICE"*) ;;
        *) settings put secure enabled_accessibility_services "$CURRENT_SERVICES:$ACC_SERVICE" ;;
    esac
fi
settings put secure accessibility_enabled 1

# 8. Whitelist from battery optimization
echo "[+] Whitelisting from battery optimization..."
cmd device_idle whitelist +"$TARGET_PKG" 2>/dev/null || true

echo "=========================================================="
echo " Done! All privileged permissions granted to $TARGET_PKG."
echo " Open PermissionLab and tap 'Refresh' to verify."
echo "=========================================================="
