import assert from 'node:assert/strict';
import fs from 'node:fs';
const root = new URL('../', import.meta.url);
const read = p => fs.readFileSync(new URL(p, root), 'utf8').replaceAll('\r\n', '\n');
let count = 0, common;
const check = (value, message) => { count++; assert.ok(value, message); };
for (const port of ['forge-1.20.1', 'neoforge-1.21.1']) {
  const java = p => read(port + '/src/main/java/cn/laowu/mod/' + p + '.java');
  const item = java('compat/netmusic/NetMusicDiscCompat');
  const songs = java('CatMusicSongs'), records = java('CatMusicRecords');
  const packet = java('network/MusicRecordPacket'), client = java('client/CatMusicRecordClient');
  const bridge = java('client/NetMusicAudioBridge');
  check(item.includes('isLoaded("netmusic")') && item.includes('stack.isEmpty() || !loaded()'), 'Optional loader gate before linking API');
  check(item.includes('getMethod("getSongInfo", ItemStack.class)'), 'Native metadata reader supports NBT and components');
  check(!item.includes('net.minecraft.client') && !item.includes('.openConnection(') && !item.includes('.openStream('), 'Server metadata reader performs no client or network work');
  check(item.includes('(long) seconds * 20') && item.includes('seconds <= 0'), 'Overflow-safe duration and rejection of unrecorded discs');
  check(item.includes('MAX_URL = 4096') && item.includes('MAX_TITLE = 256') && item.includes('Character.isHighSurrogate'), 'Bounded protocol fields and safe Unicode title clipping');
  check(songs.indexOf('NetMusicDiscCompat.read') < songs.indexOf('return null;', songs.indexOf('NetMusicDiscCompat.read')), 'Song reader includes native optional integration');
  check(records.includes('state.song.networkUrl(), state.song.title()'), 'Server sends native song metadata');
  check(records.includes('CatProfileData.INVENTORY_SLOTS') && !records.includes('CatChestData.open'), 'Existing nine-slot playlist only');
  check(packet.includes('writeUtf(p.networkUrl, NetMusicDiscCompat.MAX_URL)') && packet.includes('readUtf(NetMusicDiscCompat.MAX_URL)'), 'URL bound enforced by both codec directions');
  check(packet.includes('this(entityId, uuid, sound, sequence, remaining, "", "")'), 'Original ordinary-record constructor retained');
  check(bridge.includes('new ThreadPoolExecutor(2, 2') && bridge.includes('new ArrayBlockingQueue<>(32)'), 'Bounded asynchronous URL opening');
  check(bridge.includes('netmusic.client.audio.NetMusicAudioStream') && !bridge.includes('new URLConnection'), 'Reuse actual registered NetMusic decoder/resolvers');
  check(bridge.includes('future.orTimeout(30, TimeUnit.SECONDS)'), 'Hanging source cannot leave future pending forever');
  check(bridge.includes('cancelled.get() || !future.complete(stream)') && bridge.includes('opened.getAndSet(null)'), 'Cancellation covers both late and already-opened streams');
  check(bridge.includes('closed.compareAndSet(false, true)'), 'Idempotent close under cat and sound-engine races');
  check(client.includes('current.sequence == packet.sequence()') && client.includes('current.refresh(packet.remaining()); return;'), 'Heartbeat latches current song instead of redownloading');
  // Vanilla streaming is verified behaviorally by NetMusicClientProbe through the real SoundEngine.
  check(client.includes('if (released) return CompletableFuture.failedFuture'), 'Stopped sound cannot reopen');
  check(client.includes('request.close()') && client.includes('checkWorld(mc)') && client.includes('previous.finish();'), 'Stop and world changes release only owned sounds');
  check(client.includes('x = cat.getX(); y = cat.getY() + .5; z = cat.getZ();') && client.includes('SoundSource.RECORDS'), 'Moving positional source uses records volume');
  check(!read(port+'/build.gradle').includes('netmusic:'), 'No mandatory NetMusic build dependency');
  const normal = [item.replaceAll('net.minecraftforge.fml.', 'LOADER.').replaceAll('net.neoforged.fml.', 'LOADER.'), bridge, records].join('\n');
  if (common) check(normal === common, 'Loader ports share metadata, lifetime and playlist behavior'); else common = normal;
}
console.log('PASS: '+count+' NetMusic optional compatibility, bounded async playback and port parity checks');
