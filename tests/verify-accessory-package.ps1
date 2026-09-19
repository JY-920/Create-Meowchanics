$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$projectRoot = Split-Path $PSScriptRoot -Parent
$accessoryArt = (Get-Content -Raw -LiteralPath "$projectRoot/art/accessories-redrawn-v2/manifest.json" | ConvertFrom-Json).items
$careerArt = (Get-Content -Raw -LiteralPath "$projectRoot/art/career-accessories-v1/manifest.json" | ConvertFrom-Json).items
$careerArt += (Get-Content -Raw -LiteralPath "$projectRoot/art/general-accessories-v1/manifest.json" | ConvertFrom-Json).items
$accessoryArt += (Get-Content -Raw -LiteralPath "$projectRoot/art/accessories-art-v3/manifest.json" | ConvertFrom-Json).items
$specialistArt = (Get-Content -Raw -LiteralPath "$projectRoot/art/specialist-suits-v1/manifest.json" | ConvertFrom-Json).careers
& "$PSScriptRoot/run-animation-resource-compat.ps1"
function Read-ZipEntryText($entry) {
    if ($null -eq $entry) { throw 'Missing ZIP entry' }
    $reader = [IO.StreamReader]::new($entry.Open())
    try { $reader.ReadToEnd() } finally { $reader.Dispose() }
}
$vanilla = [IO.Compression.ZipFile]::OpenRead("$projectRoot/neoforge-1.21.1/build/moddev/artifacts/neoforge-21.1.219-client-extra-aka-minecraft-resources.jar")
try {
    foreach ($loader in @('forge-1.20.1','neoforge-1.21.1')) {
        $properties = Get-Content -LiteralPath "$projectRoot/$loader/gradle.properties"
        $publicMatch = $properties | Select-String '^mod_version=(.+)$' | Select-Object -First 1
        if (!$publicMatch) { throw "Missing mod_version in $loader/gradle.properties" }
        $public = $publicMatch.Matches[0].Groups[1].Value.Trim()
        # A public release intentionally has an empty (or absent) internal build ID.
        $internalMatch = $properties | Select-String '^internal_build=(.*)$' | Select-Object -First 1
        $internal = if ($internalMatch) { $internalMatch.Matches[0].Groups[1].Value.Trim() } else { '' }
        $version = if ($internal) { "$public-dev.$internal" } else { $public }
        $loaderSuffix = if ($loader.StartsWith('forge')) { 'forge-1.20.1' } else { 'neoforge-1.21.1' }
        $jarPath = "$projectRoot/$loader/build/libs/create-meowchanics-$version-$loaderSuffix.jar"
        $zip = [IO.Compression.ZipFile]::OpenRead($jarPath)
        try {
            foreach ($class in @('WishAdoptionBoxMenu','create/WishAdoptionOffer','create/WishAdoptionBoxBlock',
                    'create/WishAdoptionBoxBlockEntity','item/WishAdoptionBoxBlockItem','recipe/WishAdoptionBoxRecipe',
                    'client/WishAdoptionBoxRenderer','client/WishAdoptionBoxItemRenderer','client/WishAdoptionBoxScreen',
                    'accessory/CatAccessories','accessory/CatAccessoryRegistry','accessory/CatAccessoryItems',
                    'accessory/CatCommonAccessories','accessory/CatAccessoryDurability','mixin/LivingEntityCatAccessoryDamageMixin',
                    'mixin/LivingEntityAgentSmokeTargetMixin',
                    'accessory/CatAccessoryDefinition','accessory/CatAccessoryLoadout','accessory/CatAccessoryEvents',
                    'api/CatAccessoryApi','api/CatTraitApi','api/CatTraitHandle','api/CatTraitContext',
                    'genetics/CatTraitRegistry','genetics/CatTraitHooks','genetics/CatTraitScriptState',
                    'genetics/CatTraitType','genetics/ScriptedCatTrait','genetics/ScriptedCatTraitDefinition',
                    'compat/kubejs/CatTraitsKubePlugin',
                    'network/CatAccessoriesSyncPacket','network/CatEditorActionPacket','client/CatAccessoryPreview',
                    'api/CatAccessoryHandle','api/CatAccessoryContext','api/CatAccessoryProjectile',
                    'accessory/CatAccessoryHooks','accessory/CatAccessoryCharge','accessory/CatAccessoryStackData',
                    'compat/kubejs/CatAccessoriesKubePlugin', 'CatEngineeringBehavior',
                    'client/CatCrankPose', 'client/CatEngineeringAnimation', 'CatEngineeringCombat',
                    'entity/EngineeringCannon', 'entity/EngineeringCogwheelProjectile',
                    'client/EngineeringCannonRenderer', 'client/EngineeringCogwheelRenderer',
                    'CatArtilleryTactics', 'CatPilotFlight', 'CatPilotFlightRules', 'entity/CatFlightCarrier',
                    'CatDivingRules', 'CatDivingMount', 'entity/CatDivingCarrier', 'CatAgentCombatGoal', 'CatAgentSmoke', 'entity/AgentSmokeBomb',
                    'CatCockroachCombat', 'CatVisualStates', 'client/CatSupportAreas', 'client/CatPoseTransitions',
                    'accessory/CatAccessoryAuras', 'CatHealingSmoke', 'CatCockroachSplit', 'entity/CatHoneyPatch',
                    'client/CatHealingSmokeParticle', 'client/CatHoneyPatchRenderer',
                    'CatCockroachFarming', 'CatDivingAttack', 'CatAgentMeleeMotion', 'client/CatAgentAttackAnimation', 'network/AgentMeleePacket',
                    'CatAgentWatch', 'client/CatAgentWatchClient', 'network/AgentWatchPacket',
                    'CatCockroachSwarm', 'network/CockroachStatePacket', 'client/CatCockroachAnimation', 'client/CatRideAnimation',
                    'CatMusicRules', 'CatMusicDps', 'CatMusicSupport', 'CatMusicSupportGoal', 'CatMusicRecords', 'CatMusicSongs',
                    'client/CatMusicEffects', 'client/CatMusicRecordClient', 'network/MusicSupportPacket', 'network/MusicRecordPacket',
                    'CatCombatRole', 'CatMedicalSupportGoal', 'CatMedicalHealing', 'CatMedicalWork',
                    'client/MedicalPatientLayer', 'network/MedicalHealingPacket',
                    'client/CatMedicalAnimation', 'client/CatMedicalEffects', 'CatSupportRules', 'CatArtilleryMunition',
                    'CatCrankPower', 'mixin/CatHandCrankPowerMixin',
                    'client/CatPilotFlightClient', 'client/CatPilotHarnessLayer', 'mixin/PilotSkyhookPoseMixin', 'network/PilotFlightInputPacket')) {
                if (!$zip.GetEntry("cn/laowu/mod/$class.class")) { throw "Missing class $class" }
            }
            $recipeDir = if ($loader.StartsWith('forge')) { 'recipes' } else { 'recipe' }
            $suits=@('terminator','fishing','flight','fire','honey','transport','dynamite','engineering','medical','music','agent','diving','cockroach')
            foreach($name in @($suits | ForEach-Object {$_+'_suit'})+@('cat_component','cat_grenade')){
                $path="data/laowu/$recipeDir/${name}_sequenced_assembly.json"
                $entry=$zip.GetEntry($path)
                $packaged=Read-ZipEntryText $entry
                $source=Get-Content -LiteralPath "$projectRoot/$loader/src/main/resources/$path" -Raw
                if($packaged.Replace("`r`n","`n").Trim() -ne $source.Replace("`r`n","`n").Trim()){
                    throw "Packaged assembly differs from verified source: $name"
                }
                $model=Read-ZipEntryText $zip.GetEntry("assets/laowu/models/item/incomplete_$name.json") | ConvertFrom-Json
                $spritePath='assets/laowu/textures/'+$model.textures.layer0.Split(':')[1]+'.png'
                $sprite=$zip.GetEntry($spritePath)
                if(!$sprite){throw "Missing assembly intermediate sprite: $name"}
                $sha=[Security.Cryptography.SHA256]::Create();$stream=$sprite.Open()
                try{$packedHash=[BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-','')}
                finally{$sha.Dispose();$stream.Dispose()}
                if($packedHash -ne (Get-FileHash -Algorithm SHA256 -LiteralPath "$projectRoot/$loader/src/main/resources/$spritePath").Hash){
                    throw "Changed packaged intermediate sprite: $name"
                }
            }
            foreach($class in @('item/CatFilterItem','item/CatFilterRules','CatFilterMenu','client/CatFilterScreen')){
                if(!$zip.GetEntry("cn/laowu/mod/$class.class")){throw "Missing imported Wish filter class $class"}
            }
            Write-Output "PASS: $loader 15 exact assembly recipes and intermediate sprites, six new suit recipes, imported-filter classes"
            foreach ($clipName in @('cockroach_dash','cockroach_wings')) {
                $clip = (Read-ZipEntryText $zip.GetEntry("assets/laowu/cat_animation_clips/$clipName.json")) | ConvertFrom-Json
                if ($clip.length -ne 0.5 -or !$clip.tracks.group10 -or !$clip.tracks.group11) { throw "Incomplete split clip $clipName" }
                if ($clipName -eq 'cockroach_wings' -and @($clip.tracks.PSObject.Properties).Count -ne 2) { throw 'Hurt animation must only contain wings' }
            }
            foreach ($extension in @('json','vsh','fsh')) {
                if (!$zip.GetEntry("assets/laowu/shaders/core/medical_cross.$extension")) { throw 'Missing medical cross shader' }
                if (!$zip.GetEntry("assets/laowu/shaders/core/music_haste.$extension")) { throw 'Missing music haste shader' }
            }
            foreach ($particleName in @('cat_agent_smoke','cat_healing_smoke')) {
                $particleEntry=$zip.GetEntry("assets/laowu/particles/$particleName.json")
                $sprites=(Read-ZipEntryText $particleEntry | ConvertFrom-Json).textures
                if($sprites.Count -ne 12){throw "Missing campfire sprites in $particleName"}
                for($index=0;$index -lt 12;$index++){
                    if($sprites[$index] -ne "minecraft:big_smoke_$index" -or
                            $null -eq $vanilla.GetEntry("assets/minecraft/textures/particle/big_smoke_$index.png")){
                        throw "Invalid vanilla campfire sprite $particleName / $index"
                    }
                }
            }
            if($null -eq $zip.GetEntry('cn/laowu/mod/client/CatHealingSmokeParticle$SmokeProvider.class')){
                throw 'Missing normal translucent smoke provider'
            }
            foreach ($probe in @('MedicalVisualProbe','SpecialistVisualProbe','AgentWatchVisualProbe','CareerFeedbackVisualProbe','CareerAccessoryVisualProbe','SupportPresentationProbe','WishAdoptionVisualProbe','AssemblyVisualProbe','AccessoryTextVisualProbe')) {
                if ($zip.GetEntry("cn/laowu/mod/client/$probe.class")) { throw 'Client probe leaked into production JAR' }
            }
            $wishArt=(Get-Content -LiteralPath "$projectRoot/art/wish-adoption-box/manifest.json" -Raw | ConvertFrom-Json).assets
            foreach($asset in $wishArt){
                $entry=$zip.GetEntry("assets/laowu/$($asset.path)")
                if(!$entry){throw "Missing wish adoption asset: $($asset.path)"}
                $sha=[Security.Cryptography.SHA256]::Create();$stream=$entry.Open()
                try{$actual=[BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-','').ToLowerInvariant()}
                finally{$sha.Dispose();$stream.Dispose()}
                if($actual -ne $asset.sha256){throw "Wish adoption artist asset changed: $($asset.path)"}
            }
            foreach ($career in $specialistArt) {
                foreach ($asset in $career.assets) {
                    $entry = $zip.GetEntry("assets/laowu/$($asset.path)")
                    if (!$entry) { throw "Missing specialist asset $($asset.path)" }
                    $sha = [Security.Cryptography.SHA256]::Create()
                    $stream = $entry.Open()
                    try { $assetHash = [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-','').ToLowerInvariant() }
                    finally { $stream.Dispose(); $sha.Dispose() }
                    if ($assetHash -ne $asset.sha256) { throw "Specialist packaged art mismatch: $($asset.path)" }
                }
                foreach ($name in @("$($career.id)_suit", "incomplete_$($career.id)_suit")) {
                    $model = (Read-ZipEntryText $zip.GetEntry("assets/laowu/models/item/$name.json")) | ConvertFrom-Json
                    if ($model.parent -ne 'minecraft:item/generated' -or $model.textures.layer0 -ne "laowu:item/$name") {
                        throw "Wrong specialist item texture: $name"
                    }
                }
                $recipeDir = if ($loader.StartsWith('forge')) { 'recipes' } else { 'recipe' }
                foreach ($action in @('item_application','shearing')) {
                    $recipe = (Read-ZipEntryText $zip.GetEntry("data/laowu/$recipeDir/$($career.id)_suit_$action.json")) | ConvertFrom-Json
                    if ($recipe.type -ne 'create:item_application') { throw "Missing specialist Create application: $($career.id)" }
                }
            }
            Write-Output "PASS: $loader three specialist models, exact source PNGs, icons and Create wear/remove recipes; no test code"
            if ((Read-ZipEntryText $zip.GetEntry('kubejs.classfilter.txt')).Trim() -ne '+cn.laowu.mod.api') {
                throw 'Missing stable API whitelist'
            }
            $mixins = (Read-ZipEntryText $zip.GetEntry('laowu.mixins.json')) | ConvertFrom-Json
            if ($mixins.mixins -notcontains 'CatHandCrankPowerMixin') { throw 'Missing real crank capacity mixin' }
            $crankClip = (Read-ZipEntryText $zip.GetEntry('assets/laowu/cat_animation_clips/cat_engineering_crank.json')) | ConvertFrom-Json
            if ($crankClip.frames.Count -ne 181 -or $crankClip.bone_order.Count -ne 8) {
                throw 'Missing or incomplete engineer crank animation'
            }
            foreach ($crankFrame in $crankClip.frames) {
                if ([Math]::Abs($crankFrame[15]-1) -gt 0.00001 -or
                        [Math]::Abs($crankFrame[16]-0.72) -gt 0.00001 -or
                        [Math]::Abs($crankFrame[17]-1) -gt 0.00001) {
                    throw 'Engineer torso must not stretch during the work cycle'
                }
            }
            $crankHigh = $crankClip.frames[45]
            if ([Math]::Abs($crankHigh[10]-23.15) -gt 0.00001 -or $crankHigh[2] -ge 8 -or
                    $crankHigh[43] -ge 1.12 -or $crankHigh[52] -ge 1.12) {
                throw 'Missing rigid stand-up / independent torso retreat pose'
            }
            if (@($zip.Entries | Where-Object { $_.FullName -match '^cn/laowu/mod/test/' }).Count -gt 0) {
                throw 'GameTest probe must not ship in production jars'
            }
            $plugins = @((Read-ZipEntryText $zip.GetEntry('kubejs.plugins.txt')).Trim() -split '\r?\n')
            if ($plugins.Count -ne 2 -or ($plugins | Select-Object -Unique).Count -ne 2 -or
                    $plugins -notcontains 'cn.laowu.mod.compat.kubejs.CatAccessoriesKubePlugin' -or
                    $plugins -notcontains 'cn.laowu.mod.compat.kubejs.CatTraitsKubePlugin') {
                throw 'Expected exactly the two optional KubeJS plugins'
            }
            if ($zip.Entries.FullName -match '^dev/latvian/' -or $zip.Entries.FullName -match 'AccessoryIntegrationProbe' -or
                    $zip.GetEntry('data/laowu/cat_accessories/probe.json')) {
                throw 'Runtime dependency or GameTest probe leaked into production jar'
            }
            $definitions = @($zip.Entries | Where-Object FullName -Match '^data/laowu/cat_accessories/[^/]+\.json$')
            if ($definitions.Count -ne 36) { throw 'Expected 36 packaged accessories' }
            $accessoryNames = (Read-ZipEntryText $zip.GetEntry('assets/laowu/lang/zh_cn.json')) | ConvertFrom-Json
            if ($accessoryNames.'itemGroup.laowu.cat_accessories' -ne '猫咪饰品') { throw 'Missing dedicated accessory tab title' }
            foreach ($entry in $definitions) {
                $definition = (Read-ZipEntryText $entry) | ConvertFrom-Json
                $name = $definition.item.Split(':')[1]
                $model = (Read-ZipEntryText $zip.GetEntry("assets/laowu/models/item/$name.json")) | ConvertFrom-Json
                $texture = $model.textures.layer0.Split(':')
                $placeholder=@($careerArt | Where-Object { $_.id -eq $name -and !$_.source })
                if($placeholder.Count -eq 1) {
                    if($model.textures.layer0 -ne "minecraft:item/$($placeholder[0].icon)" -or
                            $accessoryNames."item.laowu.$name" -ne $placeholder[0].name -or
                            !$vanilla.GetEntry("assets/minecraft/textures/item/$($placeholder[0].icon).png")) {
                        throw "Wrong vanilla placeholder/name: $name"
                    }
                } else {
                    $sprite = $zip.GetEntry("assets/$($texture[0])/textures/$($texture[1]).png")
                    if (!$sprite) {
                        throw "Missing accessory texture $($model.textures.layer0)"
                    }
                    $reader = [IO.BinaryReader]::new($sprite.Open())
                    try { $header = $reader.ReadBytes([int]$sprite.Length) } finally { $reader.Dispose() }
                    $expectedArt = @($accessoryArt | Where-Object id -EQ $name)
                    if ($expectedArt.Count -ne 1 -or $accessoryNames."item.laowu.$name" -ne $expectedArt[0].name) {
                        throw "Missing / mismatched accessory art mapping or renamed item: $name"
                    }
                    $sha = [Security.Cryptography.SHA256]::Create()
                    try { $textureHash = [BitConverter]::ToString($sha.ComputeHash($header)).Replace('-','').ToLowerInvariant() }
                    finally { $sha.Dispose() }
                    if ($textureHash -ne $expectedArt[0].sha256) { throw "Packaged texture differs from the supplied redraw: $name" }
                    if ([Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($header, 16)) -ne 16 -or
                            [Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($header, 20)) -ne 16) {
                        throw "Accessory texture must be 16x16: $name"
                    }

                }
                $recipeDir = if ($loader.StartsWith('forge')) { 'recipes' } else { 'recipe' }
                $recipe = $zip.GetEntry("data/laowu/$recipeDir/accessory/$name.json")
                if ($recipe) { throw "Accessory must not be craftable: $name" }
            }
            $metadataPath = if ($loader.StartsWith('forge')) { 'META-INF/mods.toml' } else { 'META-INF/neoforge.mods.toml' }
            $metadata = Read-ZipEntryText $zip.GetEntry($metadataPath)
            if ($metadata -match '(?i)kubejs|rhino|photon|ldlib') { throw 'Unexpected new required mod dependency' }
            $hash = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash
            Write-Output "PASS: $loader package $version; 36 definitions/models, 36 original artist sprites, no accessory crafting recipes; 35 Wish rewards and one exclusive boss trophy, optional KubeJS API. SHA256 $hash"
        } finally { $zip.Dispose() }
    }
} finally { $vanilla.Dispose() }
