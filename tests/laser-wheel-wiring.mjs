import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
let checks = 0;
const verify = (ok, message) => { checks++; assert.ok(ok, message); };
const read = path => readFileSync(new URL('../' + path, import.meta.url), 'utf8');
for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
    const base = loader + '/src/main/java/cn/laowu/mod/';
    const code = name => read(base + name + '.java');
    const pointer = code('item/CatLaserPointerItem'), screen = code('client/CatLaserWheelScreen');
    verify(pointer.includes('CatLaserWheelScreen.open()') && !pointer.includes('CatTeamPreview.toggle()'), loader + ' sneak opens wheel');
    verify(pointer.includes('InteractionResultHolder.consume(stack)'), loader + ' no new use swing');
    verify(screen.includes('sectionAt(x - centerX, y - centerY, radius)'), loader + ' shared selection geometry');
    verify(screen.includes('CatLaserWheelLayout.spans(radius)'), loader + ' precomputed draw spans');
    verify(screen.includes('glfwGetMouseButton') && screen.includes('openingPressHeld'), loader + ' opening hold guard');
    verify(screen.includes('requestLaserSettings(-1)') && screen.includes('ready = false;'), loader + ' authoritative query and acknowledgement');
    verify(screen.includes('isPauseScreen() { return false; }'), loader + ' unpaused world');
    const preferences = code('CatCombatPreferences');
    verify(preferences.includes('player.getUUID()') && preferences.includes('action < -1 || action > 1'), loader + ' bounded owner-only write');
    verify(preferences.includes('getMainHandItem') && preferences.includes('getOffhandItem'), loader + ' held laser validation');
    verify(preferences.includes('server.overworld().getDataStorage()'), loader + ' shared world data, not dimension or item state');
    const combat = code('CatCombatControl');
    for (const guard of ['!cat.isPassenger()', '!CatPoseData.isPancake(cat)', '!cat.isOrderedToSit()',
            '!cat.isInSittingPose()', '!CatProfileData.isBeingViewed(cat)', 'CatOutfitType.TRANSPORT',
            '!CatLaserCommands.hasOrder(cat)', 'CatTeamRules.canHarm(cat, target)', 'target instanceof Enemy',
            'neutral.isAngryAt(cat)', 'cat.hasLineOfSight(target)', 'cat.tickCount + 20', 'MAX_OWNER_DISTANCE_SQR'])
        verify(combat.includes(guard), loader + ' combat guard ' + guard);
    verify(combat.includes('!nowDefending(cat, selected)'), loader + ' keeps defence / explicit target when disabling auto mode');
    verify(code('CommonEvents').includes('CatCombatControl.tick(cat)'), loader + ' server tick installed');
    const render = code('client/CatHealthBarRenderer');
    verify(render.includes('cat.getHealth(), cat.getMaxHealth()') && render.includes('cat.getPosition(partial)'), loader + ' real synced health / interpolated position');
    verify(render.includes('cat -> cat.isTame() && cat.isAlive()'), loader + ' pet-only health; no wild cats');
    verify(!render.includes('getOwnerUUID()') && !render.includes('isOwnedBy('), loader + ' includes other players pets');
    verify(render.includes('sh, z, center, tint') && render.includes('u0, v0, tint'),
        loader + ' shared fill tint reaches every rendered vertex');
    verify(render.includes(loader.startsWith('forge') ? '.color(tint >> 16 & 255' : '.setColor(tint)'),
        loader + ' correct loader vertex colour API');
    verify(render.includes('CatHealthBarLayout.draw('), loader + ' shared verified drawing plan');
    verify(!render.includes('ModNetwork') && !render.includes('sendToServer'), loader + ' no rendering packets');
    verify(render.includes('mc.options.hideGui') && render.includes('!cat.isInvisibleTo(mc.player)'), loader + ' overlay visibility');
    verify(code('client/CatLaserEffects').includes('CatHealthBarRenderer.render('), loader + ' world render installed');
    verify(code('client/ClientInputEvents').includes('CatLaserWheelScreen.inputTick()'), loader + ' release polling installed');
    verify(code('network/ModNetwork').includes('LaserSettingsRequestPacket') && code('network/ModNetwork').includes('LaserSettingsSyncPacket'), loader + ' both network directions');
    for (const lang of ['zh_cn','en_us']) {
        const json = JSON.parse(read(loader + '/src/main/resources/assets/laowu/lang/' + lang + '.json'));
        for (const key of ['title','section.0','section.1','section.2','on','off','loading','aggressive','defensive','close','help'])
            verify(typeof json['gui.laowu.laser_wheel.' + key] === 'string', loader + ' translation ' + lang + key);
    }
}
for (const name of ['CatCombatControl','client/CatLaserWheelScreen','client/CatLaserWheelLayout','client/CatHealthBarLayout'])
    verify(read('forge-1.20.1/src/main/java/cn/laowu/mod/'+name+'.java').replaceAll('\r','')
        === read('neoforge-1.21.1/src/main/java/cn/laowu/mod/'+name+'.java').replaceAll('\r',''), 'loader parity '+name);
const atlas = loader => readFileSync(new URL('../'+loader+'/src/main/resources/assets/laowu/textures/gui/cat_health_bar.png',import.meta.url));
verify(createHash('sha256').update(atlas('forge-1.20.1')).digest('hex')
    === createHash('sha256').update(atlas('neoforge-1.21.1')).digest('hex'), 'same supplied atlas on both loaders');
console.log('PASS: '+checks+' laser-wheel wiring checks (source guards, not a live AI simulation)');
