import assert from 'node:assert/strict';
import fs from 'node:fs';
const root=new URL('../',import.meta.url);
const read=p=>fs.readFileSync(new URL(p,root),'utf8').replaceAll('\r\n','\n');
let checks=0;const check=(v,m)=>{checks++;assert.ok(v,m);};
for(const port of ['forge-1.20.1','neoforge-1.21.1']){
 const j=f=>read(port+'/src/main/java/cn/laowu/mod/'+f+'.java');
 const watch=j('CatAgentWatch');
 for(const token of ['SCAN_TICKS = 20, VISUAL_TICKS = 40','MAX_RADIUS = 128','Math.max(0, intelligence) * .64',
   'CatStat.INTELLIGENCE','CatOutfitType.AGENT','findSeat(cat) != null','SeatEntity','CatSupportRules.canAssist',
   'target instanceof Enemy','MobCategory.MONSTER','CatTeamRules.canHarm','radius * radius',
   'getEntitiesOfClass(Mob.class','watch.sources.removeIf','detected.put(target.getUUID(), target)',
   'send(target, null, 0)','VISUAL_TICKS','syncTo(LivingEntity target, ServerPlayer player)'])
   check(watch.includes(token),'server rule '+port+' '+token);
 for(const forbidden of ['setGlowingTag(', 'MobEffects.GLOWING','getScoreboard(', 'setForced(', 'hasLineOfSight('])
   check(!watch.includes(forbidden),'watch is read-only and sees through walls: '+forbidden);
 check(j('CareerCatBehavior').includes('CatAgentWatch.tick(cat)')&&j('CareerCatBehavior').includes('CatAgentWatch.stop(cat)'),'career tick and unequip hooks');
 check(j('CommonEvents').includes('CatAgentWatch.flush(level)')&&j('CommonEvents').includes('CatAgentWatch.syncTo(watched, observer)'),'world flush and late-tracking hooks');
 const cache=j('client/CatAgentWatchClient');
 for(const token of ['WeakHashMap<>','entityId, long until','getUUID()','entity.level()','entity.getId()','mark.until <=','duration == 0','!entity.isAlive()','entity.isRemoved()'])
   check(cache.includes(token),'client cache safety '+token);
 const mixins=JSON.parse(read(port+'/src/main/resources/laowu.mixins.json'));
 for(const type of ['CatTeamGlowMixin','CatTeamGlowColourMixin']){
   check(mixins.client.includes(type)&&!mixins.mixins.includes(type),'render injection client-only');
   check(j('mixin/'+type).includes('CatAgentWatchClient.visible'),'real render path '+type);
 }
 check(j('mixin/CatTeamGlowColourMixin').includes('CatAgentWatchClient.RED'),'red renderer override');
 check(j('client/ClientPacketHandler').includes('CatAgentWatchClient.receive'),'packet reaches cache');
 const net=j('network/ModNetwork'),packet=j('network/AgentWatchPacket');
 check(net.includes('AgentWatchPacket')&&net.includes('agentWatch('),'payload registered and sent');
 check(packet.includes('UUID')&&packet.includes('duration'),'payload carries full identity and expiry');
 check(port.startsWith('forge')?net.includes('NetworkDirection.PLAY_TO_CLIENT'):net.includes('playToClient(AgentWatchPacket.TYPE'),'server-to-client direction');
 for(const lang of ['zh_cn','en_us']){
   const t=JSON.parse(read(port+'/src/main/resources/assets/laowu/lang/'+lang+'.json'));
   check(!!t['item.laowu.career_suit.snapshot.watch_radius'],'Shift watch radius');
   check(/128/.test(t['item.laowu.agent_suit.tooltip.work']),'default range cap');
   check(/128/.test(t['item.laowu.agent_suit.tooltip.behaviour2'])&&/0.64/.test(t['item.laowu.agent_suit.tooltip.behaviour2']),'Ctrl formula');
   check(!/[×=]/.test(t['item.laowu.agent_suit.tooltip.work']),'no formula in concise work summary');
 }
}
console.log('PASS: '+checks+' agent watch server, client identity, packet, render mixin and tooltip guards');
