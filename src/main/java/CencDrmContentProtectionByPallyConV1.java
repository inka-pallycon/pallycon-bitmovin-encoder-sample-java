import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.common.BitmovinException;
import com.bitmovin.api.sdk.model.*;
import com.pallycon.cpix.CPixCommonModule;
import com.pallycon.cpix.CpixModule;
import com.pallycon.cpix.dto.CpixDTO;
import com.pallycon.cpix.dto.DRMSystemId;
import common.ConfigProvider;
import feign.Logger.Level;
import feign.slf4j.Slf4jLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

/**
 * This example shows how DRM content protection can be applied to a fragmented MP4 muxing. The
 * encryption is configured to be compatible with both FairPlay and Widevine, using the MPEG-CENC
 * standard.
 *
 * <p>The following configuration parameters are expected:
 *
 * <ul>
 *   <li>BITMOVIN_API_KEY - Your API key for the Bitmovin API
 *   <li>HTTP_INPUT_HOST - The Hostname or IP address of the HTTP server hosting your input files,
 *       e.g.: my-storage.biz
 *   <li>HTTP_INPUT_FILE_PATH - The path to your input file on the provided HTTP server Example:
 *       videos/1080p_Sintel.mp4
 *   <li>S3_OUTPUT_BUCKET_NAME - The name of your S3 output bucket. Example: my-bucket-name
 *   <li>S3_OUTPUT_ACCESS_KEY - The access key of your S3 output bucket
 *   <li>S3_OUTPUT_SECRET_KEY - The secret key of your S3 output bucket
 *   <li>S3_OUTPUT_BASE_PATH - The base path on your S3 output bucket where content will be written.
 *       Example: /outputs
 *   <li>DRM_KEY - 16 byte encryption key, represented as 32 hexadecimal characters Example:
 *       cab5b529ae28d5cc5e3e7bc3fd4a544d
 *   <li>DRM_FAIRPLAY_IV - 16 byte initialization vector, represented as 32 hexadecimal characters
 *       Example: 08eecef4b026deec395234d94218273d
 *   <li>DRM_FAIRPLAY_URI - URI of the licensing server Example:
 *       skd://userspecifc?custom=information
 *   <li>DRM_WIDEVINE_KID - 16 byte encryption key id, represented as 32 hexadecimal characters
 *       Example: 08eecef4b026deec395234d94218273d
 *   <li>DRM_WIDEVINE_PSSH - Base64 encoded PSSH payload Example: QWRvYmVhc2Rmc2FkZmFzZg==
 * </ul>
 *
 * <p>Configuration parameters will be retrieved from these sources in the listed order:
 *
 * <ol>
 *   <li>command line arguments (eg BITMOVIN_API_KEY=xyz)
 *   <li>properties file located in the root folder of the JAVA examples at ./examples.properties
 *       (see examples.properties.template as reference)
 *   <li>environment variables
 *   <li>properties file located in the home folder at ~/.bitmovin/examples.properties (see
 *       examples.properties.template as reference)
 * </ol>
 */
public class CencDrmContentProtectionByPallyConV1 {
    private static final Logger logger = LoggerFactory.getLogger(CencDrmContentProtectionByPallyConV1.class);

    private static BitmovinApi bitmovinApi;
    private static ConfigProvider configProvider;

    public static void main(String[] args) throws Exception {
        configProvider = new ConfigProvider(args);

        bitmovinApi =
                BitmovinApi.builder()
                        .withApiKey(configProvider.getBitmovinApiKey())
                        .withLogger(
                                new Slf4jLogger(), Level.BASIC) // set the logger and log level for the API client
                        .build();

        Encoding encoding =
                createEncoding("fMP4 muxing with CENC DRM", "Example with CENC DRM content protection");

        HttpInput input = createHttpInput(configProvider.getHttpInputHost());
        Output output =
                createS3Output(
                        configProvider.getS3OutputBucketName(),
                        configProvider.getS3OutputAccessKey(),
                        configProvider.getS3OutputSecretKey());

        H264VideoConfiguration h264Config = createH264VideoConfig();
        AacAudioConfiguration aacConfig = createAacAudioConfig();

        Stream videoStream =
                createStream(encoding, input, configProvider.getHttpInputFilePath(), h264Config);
        Stream audioStream =
                createStream(encoding, input, configProvider.getHttpInputFilePath(), aacConfig);

        Fmp4Muxing videoMuxing = createFmp4Muxing(encoding, videoStream);
        Fmp4Muxing audioMuxing = createFmp4Muxing(encoding, audioStream);


        /* dash cenc */
        createDrmConfigCencDash(encoding, videoMuxing, output, "video");
        createDrmConfigCencDash(encoding, audioMuxing, output, "audio");
        /* hls FairPlay */
//        createDrmConfigFairPlay(encoding, videoMuxing, output, "video");
//        createDrmConfigFairPlay(encoding, audioMuxing, output, "audio");
        /* dash cenc with speke api */
//        createDrmConfigSpekeDash(encoding, videoMuxing, output, "video");
//        createDrmConfigSpekeDash(encoding, audioMuxing, output, "audio");
        /* hls FairPlay with speke api */
//        createDrmConfigSpekeHls(encoding, videoMuxing, output, "video");
//        createDrmConfigSpekeHls(encoding, audioMuxing, output, "audio");
//        createFmp4Muxing(encoding, videoStream, output, "video");
//        createFmp4Muxing(encoding, audioStream, output, "audio");
        executeEncoding(encoding);

        /* generate dash */
        generateDashManifest(encoding, output, "/");
        /* generate hls */
//        generateHlsManifest(encoding, output, "/");
    }



    /**
     * Creates an Encoding object. This is the base object to configure your encoding.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/encodings#/Encoding/PostEncodingEncodings
     *
     * @param name A name that will help you identify the encoding in our dashboard (required)
     * @param description A description of the encoding (optional)
     */
    private static Encoding createEncoding(String name, String description) throws BitmovinException {
        Encoding encoding = new Encoding();
        encoding.setName(name);
        encoding.setDescription(description);
        encoding.setEncoderVersion("2.39.0");

        return bitmovinApi.encoding.encodings.create(encoding);
    }

    /**
     * Adds a video or audio stream to an encoding
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/encodings#/Encoding/PostEncodingEncodingsStreamsByEncodingId
     *
     * @param encoding The encoding to which the stream will be added
     * @param input The input resource providing the input file
     * @param inputPath The path to the input file
     * @param codecConfiguration The codec configuration to be applied to the stream
     */
    private static Stream createStream(
            Encoding encoding, Input input, String inputPath, CodecConfiguration codecConfiguration)
            throws BitmovinException {
        StreamInput streamInput = new StreamInput();
        streamInput.setInputId(input.getId());
        streamInput.setInputPath(inputPath);
        streamInput.setSelectionMode(StreamSelectionMode.AUTO);

        Stream stream = new Stream();
        stream.addInputStreamsItem(streamInput);
        stream.setCodecConfigId(codecConfiguration.getId());
        stream.setMode(StreamMode.STANDARD);

        return bitmovinApi.encoding.encodings.streams.create(encoding.getId(), stream);
    }

    /**
     * Creates a resource representing an AWS S3 cloud storage bucket to which generated content will
     * be transferred. For alternative output methods see <a
     * href="https://bitmovin.com/docs/encoding/articles/supported-input-output-storages">list of
     * supported input and output storages</a>
     *
     * <p>The provided credentials need to allow <i>read</i>, <i>write</i> and <i>list</i> operations.
     * <i>delete</i> should also be granted to allow overwriting of existings files. See <a
     * href="https://bitmovin.com/docs/encoding/faqs/how-do-i-create-a-aws-s3-bucket-which-can-be-used-as-output-location">creating
     * an S3 bucket and setting permissions</a> for further information
     *
     * <p>For reasons of simplicity, a new output resource is created on each execution of this
     * example. In production use, this method should be replaced by a <a
     * href="https://bitmovin.com/docs/encoding/api-reference/sections/outputs#/Encoding/GetEncodingOutputsS3">get
     * call</a> retrieving an existing resource.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/outputs#/Encoding/PostEncodingOutputsS3
     *
     * @param bucketName The name of the S3 bucket
     * @param accessKey The access key of your S3 account
     * @param secretKey The secret key of your S3 account
     */
    private static S3Output createS3Output(String bucketName, String accessKey, String secretKey)
            throws BitmovinException {
        S3Output s3Output = new S3Output();
        s3Output.setBucketName(bucketName);
        s3Output.setAccessKey(accessKey);
        s3Output.setSecretKey(secretKey);
        s3Output.setCloudRegion(AwsCloudRegion.AP_NORTHEAST_2);

        return bitmovinApi.encoding.outputs.s3.create(s3Output);
    }

    /**
     * Creates a resource representing an HTTP server providing the input files. For alternative input
     * methods see <a
     * href="https://bitmovin.com/docs/encoding/articles/supported-input-output-storages">list of
     * supported input and output storages</a>
     *
     * <p>For reasons of simplicity, a new input resource is created on each execution of this
     * example. In production use, this method should be replaced by a <a
     * href="https://bitmovin.com/docs/encoding/api-reference/sections/inputs#/Encoding/GetEncodingInputsHttpByInputId">get
     * call</a> to retrieve an existing resource.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/inputs#/Encoding/PostEncodingInputsHttp
     *
     * @param host The hostname or IP address of the HTTP server e.g.: my-storage.biz
     */
    private static HttpInput createHttpInput(String host) throws BitmovinException {
        HttpInput input = new HttpInput();
        input.setHost(host);

        return bitmovinApi.encoding.inputs.http.create(input);
    }

    /**
     * Creates a fragmented MP4 muxing. This will split the output into continuously numbered segments
     * of a given length for adaptive streaming. However, the unencrypted segments will not be written
     * to a permanent storage as there's no output defined for the muxing. Instead, an output needs to
     * be defined for the DRM configuration resource which will later be added to this muxing.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/all#/Encoding/PostEncodingEncodingsMuxingsFmp4ByEncodingId
     *
     * @param encoding The encoding to which the muxing will be added
     * @param stream The stream to be muxed
     */
    private static Fmp4Muxing createFmp4Muxing(Encoding encoding, Stream stream)
            throws BitmovinException {
        Fmp4Muxing muxing = new Fmp4Muxing();
        muxing.setSegmentLength(4.0);

        MuxingStream muxingStream = new MuxingStream();
        muxingStream.setStreamId(stream.getId());
        muxing.addStreamsItem(muxingStream);

        return bitmovinApi.encoding.encodings.muxings.fmp4.create(encoding.getId(), muxing);
    }

    /**
     * Adds an MPEG-CENC DRM configuration to the muxing to encrypt its output. Widevine and PlayRead
     * specific fields will be included into DASH manifests to enable key retrieval using
     * either DRM method.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/encodings#/Encoding/PostEncodingEncodingsMuxingsFmp4SpekeByEncodingIdAndMuxingId
     *
     * @param encoding The encoding to which the muxing belongs to
     * @param muxing The muxing to apply the encryption to
     * @param output The output resource to which the encrypted segments will be written to
     * @param outputPath The output path where the encrypted segments will be written to
     */
    private static SpekeDrm createDrmConfigSpekeDash(Encoding encoding, Muxing muxing, Output output, String outputPath) throws BitmovinException {
        SpekeDrm spekeDrm = new SpekeDrm();
        try{
            spekeDrm.addOutputsItem(buildEncodingOutput(output, outputPath));

            spekeDrm.addSystemIdsItem(DRMSystemId.WIDEVINE.toLowerCase());
            spekeDrm.addSystemIdsItem(DRMSystemId.PLAYREADY.toLowerCase());
            spekeDrm.setContentId(configProvider.getContentId());

            SpekeDrmProvider spekeDrmProvider = new SpekeDrmProvider();
            spekeDrmProvider.setUrl(configProvider.getPallyconKmsUrl() + configProvider.getPallyconEncKey());

            spekeDrm.setProvider(spekeDrmProvider);

        }catch (Exception e){
            throw new BitmovinException(e.getMessage());
        }
        return bitmovinApi.encoding.encodings.muxings.fmp4.drm.speke.create(
                encoding.getId(), muxing.getId(), spekeDrm);
    }

    /**
     * Adds an HLS DRM configuration to the muxing to encrypt its output. FairPlay
     * specific fields will be included into HLS manifests to enable key retrieval using
     * either DRM method.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/encodings#/Encoding/PostEncodingEncodingsMuxingsFmp4SpekeByEncodingIdAndMuxingId
     *
     * @param encoding The encoding to which the muxing belongs to
     * @param muxing The muxing to apply the encryption to
     * @param output The output resource to which the encrypted segments will be written to
     * @param outputPath The output path where the encrypted segments will be written to
     */
    private static SpekeDrm createDrmConfigSpekeHls(Encoding encoding, Muxing muxing, Output output, String outputPath) throws BitmovinException {
        SpekeDrm spekeDrm = new SpekeDrm();
        try{
            spekeDrm.addOutputsItem(buildEncodingOutput(output, outputPath));

            spekeDrm.addSystemIdsItem(DRMSystemId.FAIRPLAY.toLowerCase());
            spekeDrm.setContentId(configProvider.getContentId());
            spekeDrm.setIv(configProvider.getDrmFairplayIv());

            SpekeDrmProvider spekeDrmProvider = new SpekeDrmProvider();
            spekeDrmProvider.setUrl(configProvider.getPallyconKmsUrl() + configProvider.getPallyconEncKey());
            spekeDrm.setProvider(spekeDrmProvider);

        }catch (Exception e){
            throw new BitmovinException(e.getMessage());
        }
        return bitmovinApi.encoding.encodings.muxings.fmp4.drm.speke.create(
                encoding.getId(), muxing.getId(), spekeDrm);
    }

    /**
     * Adds an HLS DRM configuration to the muxing to encrypt its output. FairPlay
     * specific fields will be included into HLS manifests to enable key retrieval using
     * either DRM method.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/encodings#/Encoding/PostEncodingEncodingsMuxingsFmp4DrmCencByEncodingIdAndMuxingId
     *
     * @param encoding The encoding to which the muxing belongs to
     * @param muxing The muxing to apply the encryption to
     * @param output The output resource to which the encrypted segments will be written to
     * @param outputPath The output path where the encrypted segments will be written to
     */
    private static FairPlayDrm createDrmConfigFairPlay(Encoding encoding, Muxing muxing, Output output, String outputPath) throws BitmovinException {
        FairPlayDrm fairPlayDrm = new FairPlayDrm();
        try{
            CpixModule cpixModule = new CPixCommonModule();
            CpixDTO cpixDTO = cpixModule.getHlsKeyInfo(configProvider.getPallyconEncKey(), configProvider.getContentId());

            fairPlayDrm.addOutputsItem(buildEncodingOutput(output, outputPath));

            fairPlayDrm.setIv(cpixDTO.getContentIvToHex());
            fairPlayDrm.setKey(cpixDTO.getContentKeyToHex());
            fairPlayDrm.setUri(cpixDTO.getFairPlayUrl());

        }catch (Exception e){
            throw new BitmovinException(e.getMessage());
        }
        return bitmovinApi.encoding.encodings.muxings.fmp4.drm.fairplay.create(
        encoding.getId(), muxing.getId(), fairPlayDrm);
    }

    /**
     * Adds an MPEG-CENC DRM configuration to the muxing to encrypt its output. Widevine and PlayRead
     * specific fields will be included into DASH manifests to enable key retrieval using
     * either DRM method.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/encodings#/Encoding/PostEncodingEncodingsMuxingsFmp4DrmCencByEncodingIdAndMuxingId
     *
     * @param encoding The encoding to which the muxing belongs to
     * @param muxing The muxing to apply the encryption to
     * @param output The output resource to which the encrypted segments will be written to
     * @param outputPath The output path where the encrypted segments will be written to
     */
    private static CencDrm createDrmConfigCencDash(Encoding encoding, Muxing muxing, Output output, String outputPath) throws BitmovinException {
        CencDrm cencDrm = new CencDrm();
        CencWidevine widevineDrm = new CencWidevine();
        CencPlayReady playReadyDrm = new CencPlayReady();
        try{

            CpixModule cpixModule = new CPixCommonModule();
            CpixDTO cpixDTO = cpixModule.getDashKeyInfo(configProvider.getPallyconEncKey(), configProvider.getContentId());

            cencDrm.addOutputsItem(buildEncodingOutput(output, outputPath));

            cencDrm.setKey(cpixDTO.getContentKeyToHex());
            cencDrm.setKid(cpixDTO.getContentKeyIdToHex());

            widevineDrm.setPssh(cpixDTO.getBitmovinPssh(DRMSystemId.WIDEVINE));
            cencDrm.setWidevine(widevineDrm);

            playReadyDrm.setLaUrl(configProvider.getPallyconLicenseUrl());
            cencDrm.setPlayReady(playReadyDrm);
        }catch (Exception e){
            throw new BitmovinException(e.getMessage());
        }
        return bitmovinApi.encoding.encodings.muxings.fmp4.drm.cenc.create(
        encoding.getId(), muxing.getId(), cencDrm);
    }
    private static Fmp4Muxing createFmp4Muxing(
            Encoding encoding, Stream stream, Output output, String outputPath) throws BitmovinException {
        MuxingStream muxingStream = new MuxingStream();
        muxingStream.setStreamId(stream.getId());

        Fmp4Muxing muxing = new Fmp4Muxing();
        muxing.addOutputsItem(buildEncodingOutput(output, outputPath));
        muxing.addStreamsItem(muxingStream);
        muxing.setSegmentLength(4.0);

        return bitmovinApi.encoding.encodings.muxings.fmp4.create(encoding.getId(), muxing);
    }



    /**
     * Builds an EncodingOutput object which defines where the output content (e.g. of a muxing) will
     * be written to. Public read permissions will be set for the files written, so they can be
     * accessed easily via HTTP.
     *
     * @param output The output resource to be used by the EncodingOutput
     * @param outputPath The path where the content will be written to
     */
    private static EncodingOutput buildEncodingOutput(Output output, String outputPath) {
        AclEntry aclEntry = new AclEntry();
        aclEntry.setPermission(AclPermission.PUBLIC_READ);

        EncodingOutput encodingOutput = new EncodingOutput();
        encodingOutput.setOutputPath(buildAbsolutePath(outputPath));
        encodingOutput.setOutputId(output.getId());
        encodingOutput.addAclItem(aclEntry);
        return encodingOutput;
    }

    /**
     * Builds an absolute path by concatenating the S3_OUTPUT_BASE_PATH configuration parameter, the
     * name of this example class and the given relative path
     *
     * <p>e.g.: /s3/base/path/ClassName/relative/path
     *
     * @param relativePath The relative path that is concatenated
     * @return The absolute path
     */
    public static String buildAbsolutePath(String relativePath) {
        String className = CencDrmContentProtectionByPallyConV1.class.getSimpleName();
//        return Paths.get(configProvider.getS3OutputBasePath(), className, relativePath).toString();
        return (Paths.get(configProvider.getS3OutputBasePath(), className, relativePath).toString()).replaceAll("\\\\", "/"); // fix window server
    }

    /**
     * Creates a configuration for the H.264 video codec to be applied to video streams.
     *
     * <p>The output resolution is defined by setting the height to 1080 pixels. Width will be
     * determined automatically to maintain the aspect ratio of your input video.
     *
     * <p>To keep things simple, we use a quality-optimized VoD preset configuration, which will apply
     * proven settings for the codec. See <a
     * href="https://bitmovin.com/docs/encoding/tutorials/how-to-optimize-your-h264-codec-configuration-for-different-use-cases">How
     * to optimize your H264 codec configuration for different use-cases</a> for alternative presets.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/configurations#/Encoding/PostEncodingConfigurationsVideoH264
     */
    private static H264VideoConfiguration createH264VideoConfig() throws BitmovinException {
        H264VideoConfiguration config = new H264VideoConfiguration();
        config.setName("H.264 1080p 1.5 Mbit/s");
        config.setPresetConfiguration(PresetConfiguration.VOD_STANDARD);
        config.setHeight(1080);
        config.setBitrate(1_500_000L);

        return bitmovinApi.encoding.configurations.video.h264.create(config);
    }

    /**
     * Creates a configuration for the AAC audio codec to be applied to audio streams.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/configurations#/Encoding/PostEncodingConfigurationsAudioAac
     */
    private static AacAudioConfiguration createAacAudioConfig() throws BitmovinException {
        AacAudioConfiguration config = new AacAudioConfiguration();
        config.setName("AAC 128 kbit/s");
        config.setBitrate(128_000L);

        return bitmovinApi.encoding.configurations.audio.aac.create(config);
    }

    /**
     * Starts the actual encoding process and periodically polls its status until it reaches a final
     * state
     *
     * <p>API endpoints:
     * https://bitmovin.com/docs/encoding/api-reference/all#/Encoding/PostEncodingEncodingsStartByEncodingId
     * https://bitmovin.com/docs/encoding/api-reference/sections/encodings#/Encoding/GetEncodingEncodingsStatusByEncodingId
     *
     * <p>Please note that you can also use our webhooks API instead of polling the status. For more
     * information consult the API spec:
     * https://bitmovin.com/docs/encoding/api-reference/sections/notifications-webhooks
     *
     * @param encoding The encoding to be started
     */
    private static void executeEncoding(Encoding encoding)
            throws InterruptedException, BitmovinException {
        bitmovinApi.encoding.encodings.start(encoding.getId(), new StartEncodingRequest());

        Task task;
        do {
            Thread.sleep(5000);
            task = bitmovinApi.encoding.encodings.status(encoding.getId());
            logger.info("encoding status is {} (progress: {} %)", task.getStatus(), task.getProgress());
        } while (task.getStatus() != Status.FINISHED && task.getStatus() != Status.ERROR);

        if (task.getStatus() == Status.ERROR) {
            logTaskErrors(task);
            throw new RuntimeException("Encoding failed");
        }
        logger.info("encoding finished successfully");
    }

    /**
     * Creates an HLS default manifest that automatically includes all representations configured in
     * the encoding.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/PostEncodingManifestsHlsDefault
     *
     * @param encoding The encoding for which the manifest should be generated
     * @param output The output to which the manifest should be written
     * @param outputPath The path to which the manifest should be written
     */
    private static void generateHlsManifest(Encoding encoding, Output output, String outputPath)
            throws Exception {
        HlsManifestDefault hlsManifestDefault = new HlsManifestDefault();
        hlsManifestDefault.setEncodingId(encoding.getId());
        hlsManifestDefault.addOutputsItem(buildEncodingOutput(output, outputPath));
        hlsManifestDefault.setName("master.m3u8");
        hlsManifestDefault.setVersion(HlsManifestDefaultVersion.V1);

        hlsManifestDefault = bitmovinApi.encoding.manifests.hls.defaultapi.create(hlsManifestDefault);
        executeHlsManifestCreation(hlsManifestDefault);
    }

    /**
     * Creates a DASH default manifest that automatically includes all representations configured in
     * the encoding.
     *
     * <p>API endpoint:
     * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/PostEncodingManifestsDash
     *
     * @param encoding The encoding for which the manifest should be generated
     * @param output The output to which the manifest should be written
     * @param outputPath The path to which the manifest should be written
     */
    private static void generateDashManifest(Encoding encoding, Output output, String outputPath)
            throws Exception {
        DashManifestDefault dashManifestDefault = new DashManifestDefault();
        dashManifestDefault.setEncodingId(encoding.getId());
        dashManifestDefault.setManifestName("stream.mpd");
        dashManifestDefault.setVersion(DashManifestDefaultVersion.V1);
        dashManifestDefault.addOutputsItem(buildEncodingOutput(output, outputPath));
        dashManifestDefault =
                bitmovinApi.encoding.manifests.dash.defaultapi.create(dashManifestDefault);
        executeDashManifestCreation(dashManifestDefault);
    }

    /**
     * Starts the DASH manifest creation and periodically polls its status until it reaches a final
     * state
     *
     * <p>API endpoints:
     * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/PostEncodingManifestsDashStartByManifestId
     * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/GetEncodingManifestsDashStatusByManifestId
     *
     * @param dashManifest The DASH manifest to be created
     */
    private static void executeDashManifestCreation(DashManifest dashManifest)
            throws BitmovinException, InterruptedException {
        bitmovinApi.encoding.manifests.dash.start(dashManifest.getId());

        Task task;
        do {
            Thread.sleep(1000);
            task = bitmovinApi.encoding.manifests.dash.status(dashManifest.getId());
        } while (task.getStatus() != Status.FINISHED && task.getStatus() != Status.ERROR);

        if (task.getStatus() == Status.ERROR) {
            logTaskErrors(task);
            throw new RuntimeException("DASH manifest creation failed");
        }
        logger.info("DASH manifest creation finished successfully");
    }

    /**
     * Starts the HLS manifest creation and periodically polls its status until it reaches a final
     * state
     *
     * <p>API endpoints:
     * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/PostEncodingManifestsHlsStartByManifestId
     * https://bitmovin.com/docs/encoding/api-reference/sections/manifests#/Encoding/GetEncodingManifestsHlsStatusByManifestId
     *
     * @param hlsManifest The HLS manifest to be created
     */
    private static void executeHlsManifestCreation(HlsManifest hlsManifest)
            throws BitmovinException, InterruptedException {

        bitmovinApi.encoding.manifests.hls.start(hlsManifest.getId());

        Task task;
        do {
            Thread.sleep(1000);
            task = bitmovinApi.encoding.manifests.hls.status(hlsManifest.getId());
        } while (task.getStatus() != Status.FINISHED && task.getStatus() != Status.ERROR);

        if (task.getStatus() == Status.ERROR) {
            logTaskErrors(task);
            throw new RuntimeException("HLS manifest creation failed");
        }
        logger.info("HLS manifest creation finished successfully");
    }

    private static void logTaskErrors(Task task) {
        task.getMessages().stream()
                .filter(msg -> msg.getType() == MessageType.ERROR)
                .forEach(msg -> logger.error(msg.getText()));
    }
}
