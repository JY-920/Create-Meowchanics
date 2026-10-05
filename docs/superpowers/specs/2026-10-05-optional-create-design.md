# Optional Create support: compatibility design

Date: 2026-10-05. User authorizes work directly on develop after preserving the complete current version on remote main.

## Intended behavior

Keep one mod and the laowu namespace. With Create installed, existing machines, processing, filters, rendered interfaces and cat jobs retain their behavior. Without Create, provide cat gameplay: cats, attributes, traits, accessories, outfits, bosses, ordinary tools and interactions. Industrial machines, automatic laser marker, creature transmitter, filters and other Create-linked features do not need standalone replacements. Alternative crafting recipes are deferred.

## Compatibility requirements

Preserve cat data and all published accessory API v1-v3/schema 1 and trait API v1/schema 1 contracts. Never clean saved trait IDs, levels or scripting state merely because an optional mod is absent. Preserve existing IDs and behavior with Create present. The supported absence mode must be tested independently on Forge 1.20.1 and NeoForge 1.21.1, including dedicated server and client class loading. Removing Create from a world containing its machinery is not an implied lossless migration; do not alter user saves during development.

## Loading boundaries

Loader-only presence detection may not resolve a Create class. Core event registration must not contain event signatures from Create. Create mixins target both Create classes and the vanilla Entity class; skip the latter's contraption mixin as well when Create is absent. Registration, client subscribers, recipe serialization and optional integrations must be isolated in later steps. Keep required dependency metadata until the complete absent-Create startup and cat gameplay checks pass; changing metadata alone would misrepresent support.

## Repository state

Stable snapshot a4a56d174a936c01445a226f1055ffc50019d7fa is verified on remote main. Remote develop starts there. Only main and develop remain as branches. The merged migration checkout is detached and its files are retained. Snapshot upload used authenticated GitHub Git Data API because HTTPS Git transport failed; tree and commit hashes were verified identical to local objects.

## Current milestone

First compatibility milestone isolates mixin selection and stateful deployer events. This milestone is not an independently usable no-Create release. Subsequent work removes core registration, entity/job and client/UI loading dependencies, then guards resource loading and updates metadata only after full validation.

