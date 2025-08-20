package se.trixzta.s3bucketspj;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/*
@Service
public class S3Service implements CommandLineRunner {


    @Override
    public void run(String... args) throws Exception {


        Dotenv dotenv = Dotenv.load();

        String bucketName = dotenv.get("S3_BUCKET_NAME");
        String accessKey = dotenv.get("ACCESS_KEY");
        String secretKey = dotenv.get("SECRET_KEY");

        Scanner scanner = new Scanner(System.in);


        S3Client s3Client = S3Client.builder()
                .credentialsProvider(() -> AwsBasicCredentials.builder()
                        .accessKeyId(accessKey)
                        .secretAccessKey(secretKey)
                        .build())
                .region(Region.EU_NORTH_1)
                .build();


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
                    System.out.println("Din fil är uppladdad till " + bucketName + "/S3B/" + keyToUpload);
                    System.out.println("Din Etag är :" + resp.eTag());

                    break;
                case 3:
                    ListObjectsV2Request listReqC3 = ListObjectsV2Request.builder().bucket(bucketName).build();
                    ListObjectsV2Response listResC3 = s3Client.listObjectsV2(listReqC3);
                    List<String> filnamnenC3 = listResC3.contents().stream().map(S3Object::key).filter(key -> !key.endsWith("/")).collect(Collectors.toList());
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
                    Path fullpath = downloadPath.resolve(Paths.get(chosenKey).getFileName());
                    s3Client.getObject(request -> request
                                    .bucket(bucketName)
                                    .key(chosenKey),
                            ResponseTransformer.toFile(fullpath.toFile()));

                    break;
                case 4:
                    return;
                default:
            }

        }
    }
}
*/
