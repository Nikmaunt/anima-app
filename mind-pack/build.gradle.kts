// :mind-pack — Play Asset Delivery pack carrying the Gemma model (ADR-010).
// fast-follow: Play fetches it automatically right after install, and the
// pack lands as a real file MediaPipe can open directly (install-time packs
// stay locked inside split APKs and would force a permanent second copy).
// The .task file itself is NOT in git — see src/main/assets/README.md.
plugins {
    // No version: AGP is already on the build classpath via build-logic;
    // declaring one here trips Gradle's plugin-resolution conflict check.
    id("com.android.asset-pack")
}

assetPack {
    packName.set("mind_pack")
    dynamicDelivery {
        deliveryType.set("fast-follow")
    }
}
