import {readFileSync} from 'node:fs';
import assert from 'node:assert/strict';

let checks = 0;
const verify = (condition, message) => { checks++; assert.ok(condition, message); };
const read = path => readFileSync(new URL('../' + path, import.meta.url), 'utf8');
for (const loader of ['forge-1.20.1', 'neoforge-1.21.1']) {
    const base = loader + '/src/main/';
    const code = name => read(base + 'java/cn/laowu/mod/' + name + '.java');
    verify(code('genetics/CatBreedingMode').includes('MUTATION(5, null, 0.40F)'), loader + ' mode');
    verify(code('create/BreedingBoxBlockEntity').includes(
        'firstTraits, secondTraits, mode, mutationChance, level.random)'), loader + ' box passes actual food');
    verify(code('create/BreedingBoxBlockEntity').includes(
        'tier().mutationChance(), mode, firstAttributes, firstTraits'), loader + ' shared effective chance');
    verify(code('genetics/CatTraitProfile').includes(
        'chooseWeightedTrait(mutationPool, mode, random)'), loader + ' mode reaches rarity draw');
    verify(code('genetics/CatTraitProfile').includes(
        'chooseWeightedTrait(compatible, CatBreedingMode.NORMAL, random)'), loader + ' founders unchanged');
    for (const lang of ['zh_cn', 'en_us']) {
        const json = JSON.parse(read(base + 'resources/assets/laowu/lang/' + lang + '.json'));
        const tooltip = json['item.laowu.mutation_cat_food.tooltip.summary'];
        verify(tooltip.includes('5') && tooltip.includes('40%'), loader + lang + ' item description');
        verify(lang === 'zh_cn' ? tooltip.includes('优良、卓越') : tooltip.includes('Good and Excellent'),
            loader + lang + ' rarity advantage described');
        const source = json['gui.laowu.breeding_box.mutation.source'];
        verify(source.includes('40%') && !source.includes('20%'), loader + lang + ' GUI source');
        verify(lang === 'zh_cn' ? json['gui.laowu.breeding_box.mutation.other'].includes('优良、卓越')
            : json['gui.laowu.breeding_box.mutation.other'].includes('Good and Excellent'),
            loader + lang + ' GUI explains new-trait bonus');
        verify(json['gui.laowu.laser_wheel.title'], loader + lang + ' prior wheel changes preserved');
    }
    const dataPath = loader.startsWith('forge') ? 'recipes' : 'recipe';
    const recipe = JSON.parse(read(base + 'resources/data/laowu/' + dataPath + '/mutation_cat_food_mixing.json'));
    verify(recipe.type === 'create:mixing' && recipe.ingredients.length === 9,
        loader + ' nine-input heated mixing recipe');
    verify(recipe.ingredients.filter(input => input.item === 'laowu:breeding_cat_food').length === 8
        && recipe.ingredients.filter(input => input.item === 'minecraft:poisonous_potato').length === 1,
        loader + ' eight breeding foods and one poisonous potato per batch');
    verify(recipe.ingredients.every(input => !('count' in input)),
        loader + ' repeated single-item ingredients enforce quantities on both serializers');
    verify((recipe.heatRequirement ?? recipe.heat_requirement) === 'heated', loader + ' heat unchanged');
    verify(recipe.results.length === 1 && recipe.results[0].count === 8
        && (recipe.results[0].item ?? recipe.results[0].id) === 'laowu:mutation_cat_food',
        loader + ' eight mutation foods per batch');
}
for (const name of ['CatBreedingMode', 'CatTraitProfile', 'CatBreedingLogic'])
    verify(read('forge-1.20.1/src/main/java/cn/laowu/mod/genetics/' + name + '.java').replaceAll('\r', '')
        === read('neoforge-1.21.1/src/main/java/cn/laowu/mod/genetics/' + name + '.java').replaceAll('\r', ''),
        'loader parity ' + name);
console.log('PASS: ' + checks + ' mutation-food wiring/description/recipe checks');
