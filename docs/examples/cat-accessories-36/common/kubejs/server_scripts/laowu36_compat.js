// priority: 200
// SDK 1.0.0: compatibility is based on the public API contract, NOT the mod build number.
// Never write global in server_scripts (NeoForge disallows it).
const laowu36Compat = {api: null, compatible: false, ready: false, reason: ''}
try {
  laowu36Compat.api = Java.loadClass('cn.laowu.mod.api.CatAccessoryApi')
  let supported = false
  try { supported = laowu36Compat.api.supportsApi(3) }
  catch (legacyApi) { supported = laowu36Compat.api.apiVersion() === 3 } // accessories.36
  laowu36Compat.compatible = supported && laowu36Compat.api.schemaVersion() === 1 &&
    typeof CatAccessoryEvents !== 'undefined'
  if (!laowu36Compat.compatible) laowu36Compat.reason = 'Requires script API v3 compatibility, schema 1 and CatAccessoryEvents'
} catch (error) {
  laowu36Compat.reason = 'Mod API is unavailable: ' + String(error)
}
if (!laowu36Compat.compatible) console.warn('[laowu36] Native accessories retained. ' + laowu36Compat.reason)
