import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import assert from 'node:assert/strict';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let checks = 0;
function check(value, description) { checks++; assert.ok(value, description); }
const careers = ['terminator', 'fishing', 'flight', 'fire', 'honey', 'transport', 'dynamite', 'engineering', 'medical'];
const texts = {};
for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
  const read = name => readFileSync(path.join(root, loader, 'src/main/java/cn/laowu/mod', name), 'utf8');
  const behavior = read('CareerCatBehavior.java');
  const events = read('CommonEvents.java');
  const config = read('ServerConfig.java');
  const global = read('GlobalConfig.java');
  const screen = read('client/WorldSettingsScreen.java');
  const tooltip = read('client/CareerSuitTooltip.java');
  const attrs = read('genetics/CatAttributeEffects.java');
  const thorns = read('genetics/CatTraitEffects.java');
  const explosion = read('DynamiteCatLastStand.java');
  const impact = read('CatProjectileDamage.java');
  const clientSettings = read('client/ClientWorldSettings.java');
  const suitSettings = read('CatSuitSettings.java');
  const suitSetting = read('CatSuitSetting.java');
  check(!/configuredAttackDamage|scaleCareerDamage/.test(behavior), loader + ': no outer damage multiplication');
  for (const type of ['MechanicalLaserProjectile', 'HoneyMissileProjectile', 'DynamiteProjectile', 'FishingRodProjectile']) {
    check(behavior.replaceAll('\r\n', '\n').includes('new ' + type + '(level, cat,\n                    (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE))'), loader + ': ' + type + ' captures native attribute damage');
    const projectile = read('entity/' + type + '.java');
    check(!/scaleCareerDamage|configuredAttackDamage|careerDamageCoefficient/.test(projectile), loader + ': ' + type + ' must not rescale impact');
    check(/tag\.putFloat\(DAMAGE_TAG, attackDamage\)/.test(projectile), loader + ': ' + type + ' retains captured damage in NBT');
    check(/damageSources\(\)\.(mobProjectile|thrown)\(this, cat\)/.test(projectile), loader + ': projectile has its own direct entity');
  }
  check(!events.includes('applyCareerDamageMultiplier') && !events.includes('scaleCareerDamage'), loader + ': remove extra event multiplication');
  for (const type of ['MechanicalLaserProjectile', 'HoneyMissileProjectile', 'DynamiteProjectile']) {
    const projectile = read('entity/' + type + '.java');
    check(projectile.includes('CatProjectileDamage.hurt(target, level.damageSources().mobProjectile(this, cat),'),
      loader + ': ' + type + ' uses scoped ordinary projectile damage');
    check(!projectile.includes('target.hurt(') && !/target\.(?:push|knockback|setDeltaMovement)\(/.test(projectile),
      loader + ': ' + type + ' has no unguarded impact or explicit shove');
  }
  const fishing = read('entity/FishingRodProjectile.java');
  check(fishing.includes('target.hurt(level.damageSources().thrown(this, cat), attackDamage)')
    && fishing.includes('target.setDeltaMovement(') && !fishing.includes('CatProjectileDamage'),
    loader + ': fishing retains ordinary hit and deliberate knockback');
  check(!explosion.includes('CatProjectileDamage'), loader + ': last-stand self-explosion is not a projectile change');
  check(!read('entity/LogisticsSupportProjectile.java').includes('.hurt('), loader + ': support remains non-damaging');
  check(impact.includes('source.getEntity() instanceof net.minecraft.world.entity.animal.Cat cat')
    && impact.includes('&& cn.laowu.mod.accessory.CatAccessories.projectileKnockback(cat)')
    && impact.includes('() -> target.hurt(source, amount)'), loader + ': optional accessory enables knockback without rescaling damage');
  check(impact.includes('finally') && impact.includes('current.remove()')
    && !/setDeltaMovement|KNOCKBACK_RESISTANCE|getPersistentData/.test(impact),
    loader + ': no victim motion overwrite or persistent immunity');
  check(events.includes('suppressCareerProjectileKnockback(LivingKnockBackEvent event)')
    && events.includes('if (CatProjectileDamage.suppressesKnockback(event.getEntity())) event.setCanceled(true);'),
    loader + ': loader event cancels only scoped victim');
  const coefficientHook = behavior.slice(behavior.indexOf('private static void applyAttackCoefficient'), behavior.indexOf('private static int careerAttackIntervalTicks'));
  check(coefficientHook.includes('ServerConfig.careerDamageCoefficient(outfit) - 1.0D'), loader + ': K replaces original total modifier');
  check(coefficientHook.includes('ATTACK_MODIFIER_ID'), loader + ': keep stable modifier identity');
  check(coefficientHook.includes(loader.startsWith('forge-') ? 'MULTIPLY_TOTAL' : 'ADD_MULTIPLIED_TOTAL'), loader + ': original vanilla multiply operation');
  check((behavior.match(/applyAttackCoefficient\(cat, outfit\)/g) || []).length === 2, loader + ': equip and live tick apply the same K');
  check(!behavior.includes('profile.damageMultiplier()'), loader + ': no second profile multiplier');
  check(behavior.includes('float damage = (float) cat.getAttributeValue(Attributes.ATTACK_DAMAGE);')
     && behavior.includes('candidate.hurt(level.damageSources().mobAttack(cat), damage)'), loader + ': fire uses already configured native attack attribute');
  check(behavior.includes('cat.doHurtTarget(target)'), loader + ': pilot uses ordinary melee event path');
  check(behavior.includes('ServerConfig.careerDamageCoefficient(outfit)'), loader + ': profile defaults replaced by active coefficient');
  check(behavior.includes('Attributes.ATTACK_DAMAGE') && behavior.includes('sanitizeValue(')
     && behavior.includes('CatAttributeEffects.attackDamage(attack) * settings.value(CatSuitSetting.DAMAGE)) : 0.0D'), loader + ': preview applies K once, before native cap');
  check(explosion.includes('ServerConfig.scaleDamage(cat.getAttributeValue(Attributes.ATTACK_DAMAGE),'), loader + ': self-detonation uses the configured attack attribute');
  check(explosion.includes('damageSources().explosion('), loader + ': explosion source unchanged');
  check(!thorns.includes('scaleCareerDamage') && thorns.includes('cat.getAttributeValue(Attributes.ATTACK_DAMAGE),') && thorns.includes('damageSources().thorns(cat)'), loader + ': thorns uses the configured attack attribute');
  check(behavior.includes('CatSuitSettings.current(outfit).intervalTicks(ServerConfig.scale(CatStat.SPEED, speed))')
     && suitSettings.includes('Math.min(ServerConfig.MAX_MULTIPLIER, Math.round(interval))'),
     loader + ': shared configured interval with safe rounding and no old 60-tick cap');
  check(behavior.includes('else if (cat.tickCount % 20 == 0)')
     && behavior.includes('applyAttributes(cat, outfit, false)'), loader + ': already-equipped suit bonuses update in place');
  for (const setting of ['HEALTH', 'ARMOR', 'TOUGHNESS'])
    check(behavior.includes('profile.value(CatSuitSetting.' + setting + ')'), loader + ': live physical bonus ' + setting);
  check(attrs.includes('value += ServerConfig.careerStatBonus(context.outfit(), stat)')
     && !attrs.includes('CareerCatBehavior.'), loader + ': six effective-stat bonuses use config, not old constants');
  check(attrs.includes('return (int) Math.max(1L, Math.min(24L, Math.round('), loader + ': ordinary interval long clamp');
  check(config.includes('MAX_MULTIPLIER = 999_999.0D') && config.includes('Double.isFinite(value)'), loader + ': shared finite range');
  check(global.includes('v == -1.0D || ServerConfig.validMultiplier(v)'), loader + ': exact inherit sentinel');
  check(config.includes('outfit.defaultDamageCoefficient(), 0.0D, MAX_MULTIPLIER'), loader + ': per-suit defaults');
  check(config.includes('"career_damage_coefficients"') && !config.includes('"career_damage_multipliers"'), loader + ': old factor section cannot masquerade as K');
  check(tooltip.includes('item.laowu.career_suit.configured_damage') && tooltip.includes('detail.replace("_K", "_" + coefficient)'), loader + ': Ctrl formula displays the live coefficient');

  check(screen.includes('careerFields.values().stream().allMatch(inputs -> inputs.size() == CatSuitSetting.values().length')
     && screen.includes('validSuitInput(entry.getKey(), entry.getValue())'), loader + ': all hidden suits/sections validated');
  const tabChange = screen.slice(screen.indexOf('private void changePage'), screen.indexOf('private void updatePageVisibility'));
  check(!/init\(\)|clearWidgets\(\)/.test(tabChange), loader + ': tabs retain draft inputs');
  check(screen.includes('box.setEditable(editable && !isLocked(key))'), loader + ': global override locks field');
  check(screen.includes('resetCareers.visible = careerPage')
    && screen.includes('resetCareers.active = !ClientWorldSettings.editableSuitDefaults(draft, selectedCareer).isEmpty()'),
    loader + ': reset exists only on career tab and respects available edits');
  const reset = screen.slice(screen.indexOf('private void resetCareerDefaults'), screen.indexOf('private void updatePageVisibility'));
  check(reset.includes('careerFields.get(selectedCareer).get(setting).setValue(Double.toString(value))')
    && !/ModNetwork|draft\.put|clearWidgets|init\(/.test(reset),
    loader + ': reset changes career text only; save is explicit');
  check(clientSettings.includes('!draft.getBoolean("can_edit")')
    && clientSettings.includes('!locks.getBoolean(ServerConfig.careerDamageKey(outfit))')
    && clientSettings.includes('outfit.defaultDamageCoefficient()'),
    loader + ': reset uses server permission and lock snapshot, not local global overrides');
  check(screen.includes('int[] points = {0, 50, 100, 150}'), loader + ': four requested previews');
  check(screen.includes('CareerCatBehavior.snapshot(selectedCareer, base, settings)')
    && screen.includes('CatSuitSettings.read(selectedCareer, setting -> Double.parseDouble(inputs.get(setting).getValue()))'),
    loader + ': preview all unsaved suit settings with shared calculator');
  check(screen.includes('outfit == selectedCareer && (setting.stat() != null) == suitBonusesPage')
    && screen.includes('box.setY(top + 80 + (index / 2) * 22)'), loader + ': only selected suit and section visible');
  check(screen.includes('setting.appliesTo(outfit) && !isLocked(setting.lockKey(outfit))'),
    loader + ': per-parameter editability and support-career attack exclusion');
  check(tooltip.includes('item.laowu.career_suit.configured_interval')
    && tooltip.includes('if (section == 3) detail = bonusDescription(settings)')
    && tooltip.includes('settings.value(setting)'), loader + ': Ctrl interval and bonuses are live, not cached defaults');
  check(config.includes('!setting.valid(settings.getDouble(setting.id()))')
    && config.includes('if (!GlobalConfig.isLocked(setting.lockKey(outfit)))'),
    loader + ': server validates per-setting type/range and its own locks');
  check(config.includes('setting.write(tag, outfit, careerSetting(outfit, setting))')
    && global.includes('value -> setting.validConfig(value, true)'), loader + ': sync and global spec cover all new settings');
  const render = screen.slice(screen.indexOf('@Override public void render'), screen.indexOf('private void drawLabel'));
  check((render.match(/renderBackground\(/g) || []).length === 1 && !render.includes('super.render('), loader + ': Modern UI background only once');
  for (const lang of ['zh_cn', 'en_us']) {
    const values = JSON.parse(readFileSync(path.join(root, loader, 'src/main/resources/assets/laowu/lang', lang + '.json'), 'utf8'));
    for (const id of careers) check(!!values['screen.laowu.world.career.' + id], loader + ': translated ' + id);
    for (const id of careers.filter(id => !['transport', 'medical'].includes(id)))
      check(values['item.laowu.' + id + '_suit.tooltip.behaviour1'].includes('_K'), loader + ': dynamic formula ' + id);
    for (const key of ['tab_attributes', 'tab_careers', 'career_help', 'career_note', 'career_example', 'career_reset', 'career_reset_help'])
      check(!!values['screen.laowu.world.' + key], loader + ': translated ' + key);
    for (const match of suitSetting.matchAll(/^    [A-Z_]+\("([^"]+)"/gm)) {
      check(!!values['screen.laowu.world.suit_setting.' + match[1]], loader + ': setting name ' + match[1]);
      check(!!values['screen.laowu.world.suit_help.' + match[1]], loader + ': setting help ' + match[1]);
    }
    for (const key of ['configured_interval', 'configured_bonus', 'no_bonus'])
      check(!!values['item.laowu.career_suit.' + key], loader + ': dynamic tooltip ' + key);
    check(values['screen.laowu.world.multiplier_help'].includes('999999'), loader + ': help has new limit');
    const relevant = Object.fromEntries(Object.entries(values).filter(([key]) => key.startsWith('screen.laowu.world.') || key === 'item.laowu.career_suit.configured_damage'));
    if (texts[lang]) check(JSON.stringify(texts[lang]) === JSON.stringify(relevant), loader + ': loader translation parity');
    texts[lang] = relevant;
  }
}
console.log('PASS: ' + checks + ' source-wiring checks; formula coefficient paths, tab/reset/lock guards, projectile knockback routing, high-value clamps and translations. This does not simulate a running world.');
