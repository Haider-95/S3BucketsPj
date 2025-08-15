package se.trixzta.s3bucketspj;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@SpringBootApplication
public class S3BucketsPjApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(S3BucketsPjApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        Dotenv dotenv = Dotenv.load();

        String bucketName = dotenv.get("S3_BUCKET_NAME");
        String accessKey = dotenv.get("ACCESS_KEY");
        String secretKey = dotenv.get("SECRET_KEY");

        Scanner scanner = new Scanner(System.in);


        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
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

        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName("PersonRegister")
                .key(Map.of("Personnummer" , AttributeValue.builder().s("20100101-0101").build()))
                        .build();

        GetItemResponse response = dynamoDbClient.getItem(getItemRequest);
        Map<String, AttributeValue> returnedItem = response.item();
        if (returnedItem.isEmpty()) {
            System.out.println("Objekt ej hittad");
        } else {
            String name = returnedItem.get("Namn" ).s();
            String Age = returnedItem.get("Age").n();
            String TelefonNummer = returnedItem.get("TelefonNummer").s();
            System.out.println("Namn: " + name + "\n " + TelefonNummer + "\n " + Age);


        }
    }
}

        /*

        while (true) {
            System.out.println("1.Lista alla filer");
            System.out.println("2.Ladda upp fil");
            System.out.println("3.Ladda ner");
            System.out.println("4.Avsluta");
            System.out.println("Choose an option: ");
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1:
                    System.out.println(" Nu listas alla filer! ");
                    ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(bucketName).build();
                    ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);
                    List<String> filnamnen = listRes.contents().stream().map(S3Object::key).collect(Collectors.toList());
                    for (String filnamn : filnamnen) {
                        System.out.println(filnamn);
                    }
                    break;

                case 2:
                    System.out.println("Vilken fil vill du ladda upp ? ");
                    Path pathFile = Paths.get(scanner.nextLine());

                    if (!pathFile.toFile().exists()) {
                        System.out.println("filen du har valt existerar inte, du går tillbaka tll menyn ");
                        break;
                    }
                    System.out.println("Vad ska filen heta i bucket ? ");
                    String keyToUpload = scanner.nextLine().trim().toLowerCase();

                    PutObjectRequest putReq = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key("S3B/" + keyToUpload).build();

                    s3Client.putObject(putReq, RequestBody.fromFile(pathFile.toFile()));

                    PutObjectResponse resp = s3Client.putObject(putReq, RequestBody.fromFile(pathFile.toFile()));
                    System.out.println("Din fil är uppladad till " + bucketName + "/S3B/" + keyToUpload);
                    System.out.println("Din Etag är :" + resp.eTag());

                    break;
                case 3:
                    ListObjectsV2Request listReqC3 = ListObjectsV2Request.builder().bucket(bucketName).build();
                    ListObjectsV2Response listResC3 = s3Client.listObjectsV2(listReqC3);
                    List<String> filnamnenC3 = listResC3.contents().stream().map(S3Object::key).filter(key ->!key.endsWith("/")).collect(Collectors.toList());
                    for (String filnamn : filnamnenC3) {
                        System.out.println(filnamn);
                    }
                    System.out.println("Vilken fil vill du ladda ner?");
                    String chosenKey = scanner.nextLine().trim();
                    boolean Matcher = filnamnenC3.contains(chosenKey);
                    if (!Matcher) {
                        System.out.println("Det du skrivit matchar inte filnamn");
                        break;
                    }
                    System.out.println("Vart vill du spara filen ? ");
                    Path downloadPath = Paths.get(scanner.nextLine());

                    if (!downloadPath.toFile().exists()) {
                        System.out.println("Filvägen existerar inte");
                        break;
                    }




                    break;
                case 4:
                    return;
                default:
            }

        }
    }
}
*/