package se.trixzta.s3bucketspj;

import io.github.cdimascio.dotenv.Dotenv;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.transcribe.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.UUID;

@Service
public class transkribering {


    private void runAI() {
        System.out.println("Running AI...");

        Dotenv dotenv = Dotenv.load();

        String accessKey = dotenv.get("ACCESS_KEY");
        String secretKey = dotenv.get("SECRET_KEY");
        String bedrockModel = dotenv.get("BEDROCK_MODEL");

        BedrockRuntimeClient bedrockRuntimeClient = BedrockRuntimeClient.builder()
                .credentialsProvider(new AwsCredentialsProvider() {
                    @Override
                    public AwsCredentials resolveCredentials() {
                        return AwsBasicCredentials.builder()
                                .accessKeyId(accessKey)
                                .secretAccessKey(secretKey).build();
                    }
                })
                .region(Region.US_EAST_1) // OBS! Enable meta Llama 3 8B Instruct i US EAST!
                .build();


        var inputText = "Describe the purpose of a 'hello world' program in one line.";
        var message = Message.builder()
                .content(ContentBlock.fromText(inputText))
                .role(ConversationRole.USER)
                .build();


        try {
            // Send the message with a basic inference configuration.
            ConverseResponse response = bedrockRuntimeClient.converse(request -> request
                    .modelId(bedrockModel)
                    .messages(message)
                    .inferenceConfig(config -> config
                            .maxTokens(512)
                            .temperature(0.5F)
                            .topP(0.9F)));

            // Retrieve the generated text from Bedrock's response object.
            var responseText = response.output().message().content().get(0).text();
            System.out.println(responseText);

            System.out.println("AI response: " + responseText);

        } catch (SdkClientException e) {
            System.err.printf("ERROR: Can't invoke model. Reason: %s", e.getMessage());
            throw new RuntimeException(e);
        }


        // Här kan du implementera AI-funktionalitet
        // Exempelvis, skapa en klient för att interagera med en AI-tjänst

        // och utföra önskade operationer.
    }

    private void runPolly() {

        System.out.println("Running Polly...");
        Dotenv dotenv = Dotenv.load();

        String accessKey = dotenv.get("ACCESS_KEY");
        String secretKey = dotenv.get("SECRET_KEY");
        String bucketName = dotenv.get("BUCKET_NAME");

        S3Client s3Client = S3Client.builder()
                .credentialsProvider(new AwsCredentialsProvider() {
                    @Override
                    public AwsCredentials resolveCredentials() {
                        return AwsBasicCredentials.builder()
                                .accessKeyId(accessKey)
                                .secretAccessKey(secretKey).build();
                    }
                })
                .region(Region.EU_NORTH_1)
                .build();


        TranscribeClient transcribeClient = TranscribeClient.builder()
                .credentialsProvider(new AwsCredentialsProvider() {
                    @Override
                    public AwsCredentials resolveCredentials() {
                        return AwsBasicCredentials.builder()
                                .accessKeyId(accessKey)
                                .secretAccessKey(secretKey).build();
                    }
                })
                .region(Region.EU_NORTH_1)
                .build();


        Scanner scanner = new Scanner(System.in);

        // Implement Polly functionality here
        while (true) {
            System.out.println("1. Start transcription");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");
            String localFilePath = "src/main/resources/RecordingInEnglish.m4a";
            String keyName = "RecordingInEnglish.m4a";
            int choice = Integer.parseInt(scanner.nextLine());
            switch (choice) {
                case 1:
                    System.out.println("Ange filnamn för ljudfilen som ska transkriberas:");
                    // Skapa en unik jobbnamn för transkribering
                    String transcriptionJobName = "job-" + UUID.randomUUID().toString();
                    // 1. Ladda upp ljudfilen till S3
                    uploadFileToS3(s3Client, bucketName, keyName, localFilePath);
                    startTranscriptionJob(transcribeClient, transcriptionJobName, bucketName, keyName);
                    waitForTranscriptionToComplete(transcribeClient, transcriptionJobName);
                    String urlForResult = getTranscriptResultUri(transcribeClient, transcriptionJobName);
                    String transcriptText = getTranscriptTextFromUrl(urlForResult);
                    System.out.println("Transkriberad text: " + transcriptText);
                    break;
                case 4:
                    return;
            }
        }
    }

    private static String getTranscriptTextFromUrl(String transcriptUri) {
        String transcriptText = null;
        try {
            URL url = new URL(transcriptUri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP-felkod: " + responseCode);
            }

            try (InputStream inputStream = conn.getInputStream()) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(inputStream);

                // Navigera i JSON-strukturen för att hitta transkriptionstexten
                transcriptText = rootNode.path("results").path("transcripts").get(0).path("transcript").asText();

                System.out.println("\n--- Transkriberad text ---");
                System.out.println(transcriptText);
                System.out.println("--------------------------");
            }

        } catch (IOException e) {
            System.err.println("Fel vid nedladdning eller parsning av transkriptionsfil: " + e.getMessage());
        }
        return transcriptText;
    }

    private String getTranscriptResultUri(TranscribeClient transcribeClient, String transcriptionJobName) {

        try {
            GetTranscriptionJobRequest getJobRequest = GetTranscriptionJobRequest.builder()
                    .transcriptionJobName(transcriptionJobName)
                    .build();

            GetTranscriptionJobResponse response = transcribeClient.getTranscriptionJob(getJobRequest);
            String transcriptUri = response.transcriptionJob().transcript().transcriptFileUri();
            System.out.println("\nTranskriptionen finns på: " + transcriptUri);

            return transcriptUri;

            // Härifrån kan du ladda ner JSON-filen och bearbeta den för att få ut den rena texten
            // (kod för nedladdning och parsing av JSON är utelämnad för att hålla exemplet kort)

        } catch (TranscribeException e) {
            System.err.println("Fel vid hämtning av transkriptionens URI: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }


    private static void waitForTranscriptionToComplete(TranscribeClient transcribeClient, String transcriptionJobName) {
        GetTranscriptionJobRequest getJobRequest = GetTranscriptionJobRequest.builder()
                .transcriptionJobName(transcriptionJobName)
                .build();

        try {
            TranscriptionJobStatus jobStatus;
            do {
                GetTranscriptionJobResponse response = transcribeClient.getTranscriptionJob(getJobRequest);
                jobStatus = response.transcriptionJob().transcriptionJobStatus();
                System.out.print(".");
                Thread.sleep(5000); // Vänta 5 sekunder
            } while (jobStatus == TranscriptionJobStatus.IN_PROGRESS);

            if (jobStatus == TranscriptionJobStatus.FAILED) {
                System.err.println("\nTranskriberingsjobbet misslyckades.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("\nFel under väntan på transkriberingsjobb: " + e.getMessage());
            System.exit(1);
        }


    }

    private static void startTranscriptionJob(TranscribeClient transcribeClient, String transcriptionJobName, String bucketName, String keyName) {

        StartTranscriptionJobRequest request = StartTranscriptionJobRequest.builder()
                .transcriptionJobName(transcriptionJobName)
                .languageCode(LanguageCode.EN_US) // Ange språk
                .media(Media.builder()
                        .mediaFileUri("s3://" + bucketName + "/" + keyName)
                        .build())
                .build();

        try {
            transcribeClient.startTranscriptionJob(request);
        } catch (TranscribeException e) {
            System.err.println("Fel vid start av transkriberingsjobb: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Transkriberingsjobb startat: " + transcriptionJobName);

    }


    private static void uploadFileToS3(S3Client s3Client, String bucketName, String keyName, String localFilePath) {

        try {
            File audioFile = new File(localFilePath);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(audioFile));
        } catch (Exception e) {
            System.err.println("Fel vid uppladdning till S3: " + e.getMessage());
            System.exit(1);
        }
    }
}
