// Isolated, synthesized fixture. No downloaded song or production resource.
// npm install --prefix build/netmusic-test-tools --no-save --package-lock=false --ignore-scripts @breezystack/lamejs@1.2.7
import fs from 'node:fs';
import { Mp3Encoder } from '../build/netmusic-test-tools/node_modules/@breezystack/lamejs/dist/lamejs.js';
const encoder = new Mp3Encoder(1, 22050, 64);
const chunks = [];
for (let start=0; start<22050*12; start+=1152) {
  const samples = new Int16Array(Math.min(1152,22050*12-start));
  for (let i=0;i<samples.length;i++) samples[i]=Math.round(Math.sin((start+i)*2*Math.PI*440/22050)*200);
  chunks.push(Buffer.from(encoder.encodeBuffer(samples)));
}
chunks.push(Buffer.from(encoder.flush()));
const target = new URL('../build/netmusic-test-tools/tone.mp3', import.meta.url);
fs.writeFileSync(target, Buffer.concat(chunks));
console.log('Generated synthetic 440 Hz MP3: ' + fs.statSync(target).size + ' bytes');
