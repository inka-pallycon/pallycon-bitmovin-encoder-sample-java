
# PallyCon Multi-DRM Bitmovin Encoder Integration Sample

This sample guide explains how to integrate Bitmovin Encoder with PallyCon Multi-DRM via CPIX or SPEKE API.

> This sample is based on [CencDrmContentProtection example](https://github.com/bitmovin/bitmovin-api-sdk-examples) from Bitmovin.

## 💡 Getting Started
### Settings
```
Only JDK 8

```


## Prerequisites and Known Issues

- You need a trial or commercial account of PallyCon Multi-DRM service.
- You need a Bitmovin Encoding service account which can access the DRM integration APIs.

## Configurations by Integration Types

### Type 1 - Using PallyCon CPIX Module

This type of integration uses a Java library pre-combiled for PallyCon CPIX specification.

First add the below items to the environment configuration of Bitmovin.

```
//required
PALLYCON_ENC_TOKEN= {{KMS token shown on PallyCon Console site}}
//required
PALLYCON_LICENSE_URL=https://license.pallycon.com/ri/licenseManager.do
//required
CONTENT_ID= {{ unique ID of your content for the packaging }}
```

Configure the integration using the below functions in the sample source

- For DASH Widevine DRM: createDrmConfigCencDash function
- For HLS FairPlay DRM: createDrmConfigFairPlay function

### Type 2 - Using SPEKE API

This type of integration uses SPEKE API instead of PallyCon CPIX module.

Fist add the below items to the environment configuration of Bitmovin.

```
//required
PALLYCON_KMS_URL=https://kms.pallycon.com/cpix/getKey?enc-token=
//required
PALLYCON_ENC_TOKEN= {{KMS token shown in PallyCon Console site}}
//required
CONTENT_ID= {{ unique ID of your content for the packaging }}
// FairPlay Required
DRM_FAIRPLAY_IV = {{ IV defined by PallyCon. Please contact us for it. }}
```

Configure the integration using the below functions in the sample source

- For DASH Widevine DRM: createDrmConfigSpekeDash function
- For HLS FairPlay DRM: createDrmConfigSpekeHls function

## How to Run The Sample

### Linux

Execute run_example.sh with the name of the java source file(`src/main/java`) as first parameter, followed by a list of configuration parameters if needed.

```bash
run-example.sh CencDrmContentProtectionByPallyConV2 BITMOVIN_API_KEY=your-api-key HTTP_INPUT_HOST=my-storage.biz
```

### Windows

Execute run_example.bat with the name of the java source file as first parameter, followed by a list of configuration parameters if needed.

```bash
run-example.bat CencDrmContentProtectionByPallyConV2 BITMOVIN_API_KEY=your-api-key HTTP_INPUT_HOST=my-storage.biz
```

***
