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
// C3 lägga till så att listobjekten får varsin siffra om tid finnes
// C4 göra så att det kollar äger om filen man söker finns efter man valt bucket
@Service
public class S3Service implements CommandLineRunner {


    @Override
    public void run(String... args) throws Exception {


        Dotenv dotenv = Dotenv.load();

        String bucketName = dotenv.get("S3_BUCKET_NAME");
        String bucketName2 = dotenv.get("S3_BUCKET_NAME2");
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
            System.out.println("4.Söka bland filer");
            System.out.println("5.Avsluta");
            System.out.println("6.välj bucket");
            System.out.println("Choose an option:");

            String choice = scanner.nextLine();
            if (!choice.matches("[1-6]")) {
                System.out.println("Ogiltigt val, försök igen.\n-------\n");
                continue;
            }
            int intChoice = Integer.parseInt(choice);

            switch (intChoice) {
                case 1:
                    System.out.println("Nu listas alla filer!\n-------\n ");
                    ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(bucketName).build();
                    ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);
                    List<String> listc1 = listRes.contents().stream().map(S3Object::key).collect(Collectors.toList());
                    for (String filnamn : listc1) {
                        System.out.println(filnamn);
                    }
                    System.out.println("Det var alla filer\n-----\n");
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
                    System.out.println("Din fil är uppladdad till " + bucketName + "/S3B/" + keyToUpload);


                    break;
                case 3:


                    ListObjectsV2Request listReqC3 = ListObjectsV2Request.builder().bucket(bucketName).build();
                    ListObjectsV2Response listResC3 = s3Client.listObjectsV2(listReqC3);
                    List<String> listC3 = listResC3.contents().stream().map(S3Object::key).filter(key -> !key.endsWith("/")).collect(Collectors.toList());
                    for (String filnamn : listC3) {
                        System.out.println(filnamn);

                    }
                    System.out.println("\n Vilken fil vill du ladda ner?\n-------\n");

                    String downloadFile = scanner.nextLine().trim();

                    boolean Matcher = listC3.contains("S3B/" + downloadFile);
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

                    Path fullpath = downloadPath.resolve(Paths.get(downloadFile).getFileName());
                    if (!fullpath.toFile().exists()) {
                        System.out.println("filen existerar inte på vald adress, nedladdning fortsätter\n-----------\n");
                        break;
                    } else if (fullpath.toFile().exists()) {
                        System.out.println("filen existerar i vald filväg och går inte att ladda ner där");
                        break;
                    }
                    s3Client.getObject(request -> request
                                    .bucket(bucketName)
                                    .key(downloadFile),
                            ResponseTransformer.toFile(fullpath.toFile()));
                    break;
                case 4:

                    System.out.println("Vilken fil söker du?");
                    String searchFile = scanner.nextLine().trim().toLowerCase();
                    String FSF = ("S3B/" + searchFile);
                    ListObjectsV2Response listobjectSF = s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).build());
                    System.out.println(searchFile);
                    if (listobjectSF.contents().stream().anyMatch(obj -> obj.key().matches(FSF))) {
                        System.out.println("Filen " + FSF + " finns i din bucket\n-------\n");
                        System.out.println(searchFile);
                    } else {
                        System.out.println("Filen " + FSF + " finns inte i din bucket\n-------\n");
                        System.out.println(searchFile);

                    }
//lägga till så man kan söka med del av filnamn

                    break;


                case 5:
                    return;

                default:

            }
        }

    }
}