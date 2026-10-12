import {readFileSync} from 'node:fs';
import assert from 'node:assert/strict';
for (const loader of ['forge-1.20.1','neoforge-1.21.1']) {
    const java = file => readFileSync(new URL('../'+loader+'/src/main/java/cn/laowu/mod/'+file+'.java',import.meta.url),'utf8');
    for(const menu of ['BREEDING_BOX','ADOPTION_BOX','WISH_ADOPTION_BOX']) {
        const registration=(loader.startsWith('forge')?'MenuScreens':'event')+'.register(LaoWuMod.'+menu+'_MENU.get()';
        assert.ok(java('client/ClientModEvents').includes(registration),loader+' core screen '+menu);
        assert.ok(!java('compat/create/CreateClientEvents').includes('MenuScreens.register(LaoWuMod.'+menu+'_MENU.get()'),loader+' no duplicate screen '+menu);
    }
    assert.match(java('LaoWuMod'),/LAOWU_TAB[\s\S]*?\.icon\(\(\) -> CAT_INGOT\.get\(\)\.getDefaultInstance\(\)\)/,loader+' main tab uses cat ingot');
    const itemRenderer=java('client/BreedingBoxItemRenderer');
    assert.ok(!/(?<!Standalone)BreedingBoxRenderer\./.test(itemRenderer),loader+' item renderer must not load Create-only renderer');
    for(const asset of ['model','texture'])assert.ok(itemRenderer.includes('StandaloneBreedingBoxRenderer.'+asset+'(tier)'),loader+' Create-independent item '+asset);
    const tooltip=java('client/CareerSuitTooltip');
    assert.ok(tooltip.includes('section == 2 && !CreateIntegration.isLoaded()'),loader+' Ctrl work respects Create availability');
    assert.ok(tooltip.includes('outfit == CatOutfitType.AGENT && CreateIntegration.isLoaded()'),loader+' standalone Shift omits seat watch');
    for(const locale of ['en_us','zh_cn']) {
        const lang=JSON.parse(readFileSync(new URL('../'+loader+'/src/main/resources/assets/laowu/lang/'+locale+'.json',import.meta.url),'utf8'));
        for(const suffix of ['no_work_without_create','exclusive','exclusive_details'])assert.ok(lang['item.laowu.career_suit.'+suffix],loader+' '+locale+' '+suffix);
        for(const outfit of ['flight','diving','transport'])assert.ok(lang['item.laowu.'+outfit+'_suit.tooltip.standalone'],loader+' '+locale+' independent '+outfit);
    }
}
console.log('PASS: core care GUIs and item renderers, cat ingot icon, and localized standalone suit work on both ports');
