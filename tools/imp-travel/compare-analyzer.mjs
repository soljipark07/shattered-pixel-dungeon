// GPL-3.0-or-later; comparison aliases adapted from the provided v4.0.2 analyzer verification tooling.
import fs from "node:fs";import path from "node:path";import vm from "node:vm";import assert from "node:assert/strict";import {execFileSync} from "node:child_process";import {createHash} from "node:crypto";import {gzipSync,gunzipSync} from "node:zlib";
const canon=s=>String(s??'').replace(/[^a-zA-Z0-9]/g,'').toLowerCase();
const simple=s=>String(s??'').split('.').at(-1).replace(/\$Seed$/,'Seed').split('$').at(-1);
const sort=a=>a.map(x=>JSON.stringify(x)).sort();
const effect=s=>canon(simple(s))||'-';
const source=r=>({ghost_quest:'ghost_reward',wandmaker_quest:'wandmaker_reward',blacksmith_quest:'blacksmith_reward',imp_quest:'imp_reward',imp_shop_cache:'shop',sacrificial_prize:'sacrificial_fire'})[r.source]||(r.source==='mimic'?/GoldenMimic/.test(r.owner)?'golden_mimic':/CrystalMimic/.test(r.owner)?'crystal_mimic':'mimic':r.source==='statue'?/ArmoredStatue/.test(r.owner)?'armored_statue':'statue':r.source==='vault_heap'?'vault_treasure':r.source);
const aliases={Gauntlet:'StoneGauntlet',Ration:'Food',ArcaneStylus:'Stylus',ExoticPotionOfStrength:'PotionOfAdrenalineSurge',ExoticPotionOfHealing:'PotionOfShielding',ExoticPotionOfMindVision:'PotionOfMagicalSight',ExoticPotionOfFrost:'PotionOfSnapFreeze',ExoticPotionOfLiquidFlame:'PotionOfDragonsBreath',ExoticPotionOfToxicGas:'PotionOfCorrosiveGas',ExoticPotionOfHaste:'PotionOfStamina',ExoticPotionOfInvisibility:'PotionOfShroudingFog',ExoticPotionOfLevitation:'PotionOfStormClouds',ExoticPotionOfParalyticGas:'PotionOfEarthenArmor',ExoticPotionOfPurity:'PotionOfCleansing',ExoticPotionOfExperience:'PotionOfDivineInspiration',ExoticScrollOfUpgrade:'ScrollOfEnchantment',ExoticScrollOfIdentify:'ScrollOfDivination',ExoticScrollOfRemoveCurse:'ScrollOfAntiMagic',ExoticScrollOfMirrorImage:'ScrollOfPrismaticImage',ExoticScrollOfRecharging:'ScrollOfMysticalEnergy',ExoticScrollOfTeleportation:'ScrollOfPassage',ExoticScrollOfLullaby:'ScrollOfSirensSong',ExoticScrollOfMagicMapping:'ScrollOfForesight',ExoticScrollOfRage:'ScrollOfChallenge',ExoticScrollOfRetribution:'ScrollOfPsionicBlast',ExoticScrollOfTerror:'ScrollOfDread',ExoticScrollOfTransmutation:'ScrollOfMetamorphosis'};
let catalog;
const kind=s=>canon(aliases[s]||aliases[simple(s)]||catalog.items[s]?.nameEn||simple(s));
const mobKind=s=>/VaultElemental\$(Fire|Frost|Shock)$/.test(s)?canon(s.split('$').at(-1)+'Elemental'):canon(simple(s).replace(/^Vault/,''));
const getArg=(name,fallback)=>{const i=process.argv.indexOf(name);return i<0?fallback:process.argv[i+1];};
const fixturePath=getArg('--fixtures',new URL('./analyzer-reference.json.gz',import.meta.url).pathname);
const htmlPath=getArg('--generate-html',null);
const classpath=getArg('--classpath',null);
if(!classpath)throw Error('--classpath is required');
const summaryPath=getArg('--summary',new URL('../../build/imp-regression/analyzer-summary.json',import.meta.url).pathname);
const cache=path.join(path.dirname(summaryPath),'analyzer-records');fs.mkdirSync(cache,{recursive:true});
let reference;
if(htmlPath){
 const html=fs.readFileSync(htmlPath,'utf8');
 const scripts=[...html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/g)];
 const packed=scripts.find(x=>x[2].startsWith('globalThis.SCOUT_EMBED='))[2].trim();
 const embedded=JSON.parse(packed.slice('globalThis.SCOUT_EMBED='.length).replace(/;$/,''));
 const worker=scripts.find(x=>x[1].includes('scout-embedded-engine'))[2];
 globalThis.SCOUT_EMBED={};
 vm.runInThisContext(worker.replace(' return runner;',' runner.raw={initSync,scout,level_map,engine_info}; return runner;'));
 const api=SCOUT_EMBED.createRunner().raw;
 const bytes=Buffer.from(embedded.wasm,'base64');api.initSync({module:bytes});
 const info=JSON.parse(api.engine_info());
 const cases=[];
 for(const seed of ['AAA-AAB-LKQ','AAA-AAA-CRC','AAA-AAA-AAA','AAA-AAA-AAB','AAA-AAA-AAC','FVX-YTN-SLC','GFX-PZH-DCH','DQM-NZN-XZG'])cases.push({seed,mask:0,hero:'WARRIOR'});
 for(const c of info.challenges)cases.push({seed:'AAA-AAB-LKQ',mask:c.mask,hero:'WARRIOR'});
 for(const mask of [104,504,511,136])cases.push({seed:'AAA-AAB-LKQ',mask,hero:'WARRIOR'});
 for(const hero of ['MAGE','ROGUE','HUNTRESS','DUELIST','CLERIC'])cases.push({seed:'AAA-AAB-LKQ',mask:0,hero});
 reference={schema:'solji-imp-analyzer-parity/v1',htmlSha256:createHash('sha256').update(html).digest('hex'),
  engineSha256:createHash('sha256').update(bytes).digest('hex'),analyzerFile:path.basename(htmlPath),
  catalog:{items:Object.fromEntries(Object.entries(embedded.catalog.items).map(([k,v])=>[k,{nameEn:v.nameEn}]))},cases:[]};
 for(const c of cases){
  const request={seed:c.seed,trinket:'none',trinketPlan:[],challenges:info.challenges.filter(x=>c.mask&x.mask).map(x=>x.name)};
  const report=JSON.parse(api.scout(JSON.stringify(request)));
  const maps=[];
  for(let depth=1;depth<=24;depth++){
   if([10,20].includes(depth))continue;
   const m=JSON.parse(api.level_map(JSON.stringify({...request,depth,branch:0})));
   maps.push(compact(m));
  }
  const imp=report.quests.find(q=>q.quest==='imp').depth;
  maps.push(compact(JSON.parse(api.level_map(JSON.stringify({...request,depth:imp,branch:1})))));
  reference.cases.push({...c,request,report:{items:report.items,itemMappings:report.itemMappings,trinketOrder:report.trinketOrder,quests:report.quests},maps});
 }
 fs.writeFileSync(fixturePath,gzipSync(JSON.stringify(reference),{level:9}));
}else reference=JSON.parse(gunzipSync(fs.readFileSync(fixturePath)));
catalog=reference.catalog;
let mapCount=0,knownItems=0,unknownItems=0,gearCount=0;
const terrainDifferences=[];
const results=[];
for(let n=0;n<reference.cases.length;n++){
 const c=reference.cases[n],tag=c.seed+' mask='+c.mask+' '+c.hero;
 const args=['-Xmx1500m','-Dimp.heroClass='+c.hero,'-cp',classpath,'com.shatteredpixel.shatteredpixeldungeon.AnalyzerExportLauncher',
  '--seed',c.seed,'--challenges',String(c.mask),'--floors','1-24','--vault','--map-contents','--format','json'];
 const output=execFileSync('java',args,{maxBuffer:24*1024*1024});
 fs.writeFileSync(path.join(cache,n+'.json'),output);
 const records=JSON.parse(output).records;
 const actual=records.filter(r=>r.record==='item'&&r.searchable&&r.simple_class!=='Dart'&&!(r.source==='vault_heap'&&r.room==='VaultFinalRoom'))
  .map(r=>[r.depth,kind(r.simple_class),r.search_upgrade,r.cursed,effect(r.glyph||r.enchantment),source(r)]);
 const expected=c.report.items.filter(i=>i.category!=='trinket'&&i.id!=='dart')
  .map(i=>[i.depth,kind(i.name),i.upgrade,i.cursed,effect(i.effect?.name),i.source]);
 assert.deepEqual(sort(actual),sort(expected),'all equipment, artifacts and quest rewards '+tag);gearCount+=actual.length;
 const identification=records.find(r=>r.record==='run_init').identification;
 const trinkets=records.find(r=>r.record==='trinket_choices');
 assert.ok(trinkets,'actual trinket deck exported '+tag);
 assert.deepEqual(trinkets.choices.map(kind),c.report.trinketOrder.map(i=>kind(i.name)), 'trinket choices '+tag);
 for(const family of ['potions','scrolls','rings'])assert.deepEqual(
  sort(identification[family].map(i=>[kind(i.class),canon(i.label)])),
  sort(c.report.itemMappings[family].map(i=>[kind(i.name),canon(i.appearance)])), 'unidentified names '+family+' '+tag);
 for(const m of c.maps){
  const o=records.find(r=>r.record==='level'&&r.depth===m.depth&&r.branch===m.branch),label=tag+' '+m.depth+'/'+m.branch;
  assert.ok(o,'saved level exported '+label);
  assert.deepEqual([o.width,o.height,o.entrance,o.exit,o.feeling.toLowerCase()],
   [m.width,m.height,m.entrance,m.branch===1&&m.exit==null?m.entrance:m.exit,m.feeling], 'map dimensions/transitions '+label);
  if(o.map_hash!==m.hash){
   // Precisely recorded pre-existing map-only discrepancy; do not ignore other cells.
   assert.ok(c.seed==='AAA-AAB-LKQ'&&(c.mask&8)&&m.depth===24&&m.branch===0&&m.gardenGrass?.includes(1900),
    'unexplained terrain difference '+label);
   assert.equal(o.map[1900],15,'observed high-grass value '+label);
   const corrected=[...o.map];const cells=[1900];corrected[1900]=2;
   assert.equal(corrected.reduce((h,n)=>(Math.imul(h,31)+n)|0,1),m.hash,'terrain difference beyond known garden grass '+label);
   terrainDifferences.push({seed:c.seed,mask:c.mask,depth:m.depth,branch:m.branch,cells,
    reason:'recorded map-only discrepancy: analyzer grass2 versus official/APK highgrass15 at cell1900; all item contents still checked exactly'});
  }
  const heaps=m.heaps.map(h=>({...h,items:h.items.filter(i=>!['scrollofupgrade','potionofstrength'].includes(kind(i.kind)))})).filter(h=>h.items.length);
  assert.deepEqual(sort(o.heaps.map(h=>[h.cell,canon(h.kind)])),sort(heaps.map(h=>[h.cell,canon(h.kind)])), 'heap cells/containers '+label);
  for(const h of heaps){
   const other=o.heaps.find(x=>x.cell===h.cell);assert.equal(other.items.length,h.items.length,'heap count '+label+' '+h.cell);
   const pool=other.items.map(i=>({...i,canonical:kind(i.kind)}));
   for(const i of h.items.filter(i=>i.deterministic===true)){
    const ix=pool.findIndex(x=>x.canonical===kind(i.kind)&&x.quantity===i.quantity);
    assert.ok(ix>=0,'exact item '+label+' '+h.cell+' '+i.kind+' vs '+JSON.stringify(pool));pool.splice(ix,1);knownItems++;
   }
   for(const i of h.items.filter(i=>i.deterministic!==true)){
    const ix=pool.findIndex(x=>x.quantity===i.quantity&&allowedUnknown(i.kind,x.kind));
    assert.ok(ix>=0,'analyzer-declared unknown family/count '+label+' '+h.cell+' '+i.kind);pool.splice(ix,1);unknownItems++;
   }
   assert.equal(pool.length,0,'no additional item '+label+' '+h.cell);
  }
  for(const mob of m.mobs)assert.ok(o.mobs.some(x=>x.cell===mob.cell&&mobKind(x.class)===mobKind(mob.kind)), 'known mob '+label+' '+mob.cell+' '+mob.kind);
  mapCount++;
 }
 const wealth=actual.filter(i=>i[1]==='ringofwealth');
 results.push({seed:c.seed,mask:c.mask,hero:c.hero,maps:c.maps.length,equipment:actual.length,wealth});
 console.log('PASS analyzer '+tag+' ('+c.maps.length+' maps, '+actual.length+' equipment/reward candidates)');
}
const summary={analyzerFile:reference.analyzerFile,htmlSha256:reference.htmlSha256,engineSha256:reference.engineSha256,
 scenarios:results.length,maps:mapCount,equipment:gearCount,knownHeapItems:knownItems,analyzerDeclaredUnknownItems:unknownItems,terrainDifferences,
 trinket:'none',trinketPlan:[],naturalUpgradesRemoved:true,naturalStrengthPotionsRemoved:true,results};
fs.writeFileSync(summaryPath,JSON.stringify(summary,null,2));
console.log('PASS direct analyzer comparison: '+JSON.stringify({...summary,results:undefined}));

function compact(m){return {depth:m.depth,branch:m.branch,width:m.width,height:m.height,
 hash:m.terrain.reduce((h,n)=>(Math.imul(h,31)+n)|0,1),entrance:m.entrance,exit:m.exit,feeling:m.feeling,
 heaps:m.contents.heaps,mobs:m.contents.mobs.filter(x=>!x.approximate),
 gardenGrass:m.terrain.flatMap((tile,cell)=>tile===2&&m.rooms.some(r=>r.kind==='secret_garden'&&cell%m.width>r.bounds[0]&&cell%m.width<r.bounds[2]&&Math.floor(cell/m.width)>r.bounds[1]&&Math.floor(cell/m.width)<r.bounds[3])?[cell]:[])};}
function allowedUnknown(expected,actual){
 const k=String(actual),s=simple(actual);
 if(['VelvetPouch','ScrollHolder','PotionBandolier','MagicalHolster'].includes(expected))return /\.bags\./.test(k);
 if(expected==='RuntimeLaboratoryPotion')return /\.potions\./.test(k);
 if(expected==='RuntimeLibraryScroll')return /\.scrolls\./.test(k);
 if(expected==='RuntimeVaultConsumable')return /\.(potions|scrolls|stones|plants)\./.test(k);
 throw Error('Unrecognized analyzer uncertainty: '+expected+' actual '+s);
}
