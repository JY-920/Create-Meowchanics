import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8').replaceAll('\r\n','\n');
let checks=0,shared;
const check=(value,why)=>{checks++;assert.ok(value,why);};
for(const port of ['forge-1.20.1','neoforge-1.21.1']) {
 const java=name=>read(port+'/src/main/java/cn/laowu/mod/'+name+'.java');
 const flight=java('CatPilotFlight'),rules=java('CatPilotFlightRules'),carrier=java('entity/CatFlightCarrier');
 const pose=java('mixin/PilotSkyhookPoseMixin'),client=java('client/CatPilotFlightClient'),network=java('network/ModNetwork');
 check(flight.includes('AllItems.WRENCH.isIn')&&flight.includes('player.isShiftKeyDown()'),'real wrench and preserved sneak inventory');
 check(flight.includes('!cat.isOwnedBy(player)')&&flight.includes('cat.isBaby()')&&flight.includes('distanceToSqr(player) > 25'),'owner/adult/proximity authority');
 check(flight.includes('getBlockCollisions')&&flight.includes('getY() + 3.35'),'headroom guard covers calibrated passenger height');
 check(flight.includes('ServerChainConveyorHandler.hangingPlayers.containsKey'),'do not hijack an active real chain ride');
 check(flight.includes('CatLaserCommands.cancel(cat)')&&flight.includes('cat.setTarget(null)'),'clear previous movement/combat orders');
 check(flight.includes('Math.max(0, used - 2)')&&flight.includes('cat.getTarget() != null'),'recover on idle ground, not in combat');
 check(!flight.slice(flight.indexOf('boolean start(')).includes('putInt(USED'),'remount never resets fuel');
 check(carrier.includes('sender.getVehicle() != this')&&carrier.includes('rider() != sender'),'actual rider authentication');
 check(carrier.includes('now == lastPacketTime')&&carrier.includes('validInput('),'rate-limited finite input validation');
 check(carrier.includes('inputTime <= 10')&&carrier.includes('hasChunksAt'),'stale-input / unloaded-chunk guard');
 check(carrier.includes('getControllingPassenger() { return null; }'),'no client-authoritative vehicle packets');
 check(carrier.includes('CAT_HEIGHT = 2.5')&&carrier.includes('passenger instanceof Cat ? CAT_HEIGHT : 0'),'same-root upper cat/lower player calibrated to wrench jaw');
 check(carrier.includes('if (!gliding())')&&carrier.includes('used >= capacity'),'latching exhaustion glide');
 check(carrier.includes('getMaxBuildHeight() + 32'),'bounded flight ceiling');
 check(!carrier.includes('SLOW_FALLING')&&!carrier.includes('passenger.discard()'),'no added slow falling, never delete passengers');
 check(rules.includes('(5 + 0.25')&&rules.includes('(4 + 0.06')&&!rules.includes('Math.min(30')&&!rules.includes('scale(1.5)'),'uncapped flight formulas and motion');
 check(flight.includes('Math.min(duration(cat), data.getLong(USED))')&&carrier.includes('getLong(CatPilotFlight.USED)'),'legacy fuel capped on recovery and flight');
 check(!client.includes('tickCount % 2')&&client.includes('local.getYRot()'),'every-tick input and immediate local view yaw');
 check(rules.includes('previous.length(), desiredSpeed')&&!rules.includes('previous.scale(0.65)'),'speed easing no longer delays heading');
 check(java('client/HissingCatRenderer').includes('CatPilotFlightClient.viewYaw')&&java('client/HissingCatRenderer').includes('new CatPilotHarnessLayer(this)'),'predicted rendering and physical harness layer wired');
 const harness=java('client/CatPilotHarnessLayer');
 check(harness.includes('"body_band"')&&!harness.includes('"hanger"')&&!harness.includes('"grip"'),'one body band only: no lower ring or connector');
 check(harness.includes('instanceof CatFlightCarrier')&&harness.includes('cat.isBaby()')&&harness.includes('cat.isInvisible()'),'ring is only visible for mounted adult cats');
 check(harness.includes('model.applyBodyPoseDelta(pose)')&&harness.includes('pose.pushPose()')&&harness.includes('pose.popPose()'),'harness follows live torso with an isolated pose stack');
 const opening=java('CommonEvents').slice(java('CommonEvents').indexOf('boolean flightOpening'),java('CommonEvents').indexOf('// Other owned cats'));
 check(opening.includes('player.isShiftKeyDown()')&&!opening.includes('!held.isEmpty()'),'legacy pilot backpack supports empty-hand sneak-use');
 check(opening.indexOf('if (event.getLevel().isClientSide) return;')<opening.indexOf('if (inventory.isEmpty()) return;'),'legacy contents checked on server only, after consuming client interaction');
 check(opening.includes('if (inventory.isEmpty()) return;')&&opening.includes('containerId, playerInventory, inventory'),'same inspected nonempty inventory is passed into menu');
 check(rules.includes('Math.min(-0.035')&&rules.includes('!gliding && (up || down)'),'glide cannot regain height from jump/momentum');
 check(java('CommonEvents').includes('pilotLogout(')&&java('CommonEvents').includes('carrier.release(true)'),'logout cleanup');
 check(java('CareerCatBehavior').includes('!CatPilotFlight.carried(cat)'),'no combat while carrying');
 check(java('LaoWuMod').includes('CAT_FLIGHT_CARRIER')&&java('client/ClientModEvents').includes('CAT_FLIGHT_CARRIER'),'registered and client-renderable carrier');
 check(client.includes('forwardImpulse')&&client.includes('leftImpulse')&&client.includes('keyJump.isDown()'),'real local movement keys');
 check(network.includes('PilotFlightInputPacket')&&java('network/PilotFlightInputPacket').includes('carrier.input'),'serverbound packet wired');
 check(pose.includes('PlayerSkyhookRenderer.class, remap = false')&&pose.includes('setHangingPose(left, model)'),'exact Create pose, not replacement animation');
 check(pose.includes('player.getVehicle() instanceof CatFlightCarrier')&&pose.includes('player.getMainArm()'),'pose scoped to carriers with correct handedness');
 check(!pose.includes('updatePlayerList')&&!pose.includes('hangingPlayers'),'never mutate Create global chain passenger state');
 const config=JSON.parse(read(port+'/src/main/resources/laowu.mixins.json'));
 check(config.client.includes('PilotSkyhookPoseMixin')&&!config.mixins.includes('PilotSkyhookPoseMixin'),'pose mixin client-only');
 for(const lang of ['zh_cn','en_us']) {
  const text=JSON.parse(read(port+'/src/main/resources/assets/laowu/lang/'+lang+'.json'));
  for(const key of ['item.laowu.career_suit.snapshot.flight_time','item.laowu.career_suit.snapshot.flight_speed','entity.laowu.cat_flight_carrier','message.laowu.pilot_flight.remaining','message.laowu.pilot_flight.gliding','message.laowu.pilot_flight.blocked'])check(!!text[key],lang+': '+key);
  for(const suit of ['flight','engineering']) {
   check(!text['item.laowu.'+suit+'_suit.tooltip.condition4']&&!text['item.laowu.'+suit+'_suit.tooltip.behaviour4'],'no special fourth tooltip section');
   for(let i=1;i<=3;i++) check(text['item.laowu.'+suit+'_suit.tooltip.condition'+i]===text['item.laowu.honey_suit.tooltip.condition'+i],'same section titles as existing suits');
  }
  const summary=text['item.laowu.flight_suit.tooltip.behaviour2'];
  check(!/旧背包|新背包|legacy backpack|new backpack/i.test(summary),'requested tooltip omits legacy backpack migration; compatibility is still tested in game');
 }
 const common=[flight,rules,pose,client,harness].join('\n');
 if(shared)check(shared===common,'identical flight math/control/pose across ports');
 shared=common;
}
console.log('PASS: '+checks+' passenger flight authority, persistence, pose, translation and dual-loader wiring checks');
