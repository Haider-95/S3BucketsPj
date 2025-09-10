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


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;


@Service
public class S3Service implements CommandLineRunner {

    @Override
    public void run(String... args) throws IOException {

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

            System.out.println("Välkommen till S3 Bucket hanteraren\n");
            System.out.println("Vilken bucket vill du använda ? ");
            System.out.println("1. " + bucketName);
            System.out.println("2. " + bucketName2);
            System.out.println("3. Avsluta\n");
            String bucketChoice = scanner.nextLine();


            if (bucketChoice.equals("1")) {
                bucketChoice = bucketName;
            } else if (bucketChoice.equals("2")) {
                bucketChoice = bucketName2;
            } else if (bucketChoice.equals("3")) {
                System.out.println("avsluta");
            } else {
                System.out.println("Ogiltigt val, avslutar programmet");
                return;
            }


            while (bucketChoice == "1" || bucketChoice == "2" || !bucketChoice.isEmpty()) {
                System.out.println("\n1.Lista alla filer");
                System.out.println("2.Ladda upp fil");
                System.out.println("3.Ladda ner");
                System.out.println("4.Söka bland filer");
                System.out.println("5.Ta bort filer");
                System.out.println("6.Ladda upp mapp som zip");
                System.out.println("7.Tillbaka tll bucketval");
                System.out.println("Välj ett alternativ:\n");

                String choice = scanner.nextLine();
                if (!choice.matches("[1-7]")) {
                    System.out.println("Ogiltigt val, försök igen.\n-------\n");
                    continue;
                }
                int intChoice = Integer.parseInt(choice);

                switch (intChoice) {
                    case 1:
                        System.out.println("\nNu listas alla filer!\n-------\n ");
                        ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(bucketChoice).build();
                        ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);
                        List<String> listc1 = listRes.contents().stream().map(S3Object::key).collect(Collectors.toList());
                        for (String filnamn : listc1) {
                            System.out.println(filnamn);
                        }
                        System.out.println("\n-----Det var alla filer\n");
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
                                .bucket(bucketChoice)
                                .key(keyToUpload).build();

                        s3Client.putObject(putReq, RequestBody.fromFile(pathFile.toFile()));
                        System.out.println("Din fil är uppladdad till " + bucketChoice + "\\" + keyToUpload);


                        break;
                    case 3:

                        ListObjectsV2Request listReqC3 = ListObjectsV2Request.builder().bucket(bucketChoice).build();
                        ListObjectsV2Response listResC3 = s3Client.listObjectsV2(listReqC3);
                        List<String> listC3 = listResC3.contents().stream().map(S3Object::key).filter(key -> !key.endsWith("/")).toList();
                        for (String filnamn : listC3) {
                            System.out.println(filnamn);
                        }
                        System.out.println("\n Vilken fil vill du ladda ner?\n-------\n");

                        String downloadFile = scanner.nextLine().trim();

                        boolean Matcher = listC3.contains(downloadFile);

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
                            System.out.println("\nFilen existerar inte på vald adress, nedladdning fortsätter\n-----------\n");
                            break;
                        } else if (fullpath.toFile().exists()) {
                            System.out.println("\nFilen existerar i vald filväg och går inte att ladda ner där");
                            break;
                        }

                        GetObjectRequest getReq = GetObjectRequest.builder()
                                .bucket(bucketChoice)
                                .key(downloadFile).build();

                        s3Client.getObject(getReq, ResponseTransformer.toFile(fullpath));
                        break;

                    case 4:

                        System.out.println("Vilken fil söker du?");
                        String searchFile = scanner.nextLine().trim().toLowerCase();

                        ListObjectsV2Response listobjectSF = s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucketChoice).build());
                        listobjectSF.contents().stream().anyMatch(obj -> obj.key().contains(searchFile));

                        if (listobjectSF.contents().stream()
                                .anyMatch(obj -> obj.key().toLowerCase().contains(searchFile))) {

                            System.out.println("Följande filer har din sökning som en del av namnet\n-------");
                            listobjectSF.contents().stream()
                                    .map(S3Object::key)
                                    .filter(key -> key.toLowerCase().contains(searchFile))
                                    .forEach(System.out::println);


                            System.out.println("--------");

                        } else {
                            System.out.println(searchFile + "\nFinns inte i din bucket\n-------\n");
                        }


                        break;
                    case 5:
                        System.out.println("Här får du en lista med filer.\n-------\n");
                        ListObjectsV2Request listReqC6 = ListObjectsV2Request.builder().bucket(bucketChoice).build();
                        ListObjectsV2Response listResC6 = s3Client.listObjectsV2(listReqC6);
                        List<String> listC5 = listResC6.contents().stream().map(S3Object::key).collect(Collectors.toList());
                        for (String filnamn : listC5) {
                            System.out.println(filnamn);
                        }
                        System.out.println("Vilken fil vill du radera \n-------\n");
                        String deleteChoice = scanner.nextLine().trim();


                        DeleteObjectResponse deleteObjectsRes = s3Client.deleteObject(DeleteObjectRequest.builder()
                                .bucket(bucketChoice)
                                .key(deleteChoice).build());

                        if (listC5.contains(deleteChoice)) {
                            System.out.println("Filen " + deleteChoice + " finns inte din bucket längre");
                            break;

                        } else {
                            System.out.println("Fel input\n");
                        }
                        break;
                    case 6:
                        //mappens sökväg
                        System.out.println("Vilken mapp vill du ladda upp");
                        Path folderPath = Paths.get(scanner.nextLine().trim());

                        // if-satas för att kolla om den finns
                        if (Files.notExists(folderPath) || !Files.isDirectory(folderPath)) {
                            System.out.println("Mappen du har valt existerar inte, du går tillbaka tll menyn\n");
                            break;
                        } else {
                            System.out.println(folderPath + " existerar  redan  ");
                        }
                        //sc för att döpa filen i bucket
                        System.out.println("\nVad ska den heta?\n");
                        String zipNaming = scanner.nextLine().trim().toLowerCase();

                        //if-sats för att säkerställa filtypen blir .zip
                        if (!zipNaming.endsWith(".zip")) {
                            zipNaming += ".zip";
                        }

                        //skapar en temp fil .zip
                        Path tempZip = Files.createTempFile("upload-", ".zip");

                        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZip))) {
                            Files.walk(folderPath).filter(Files::isRegularFile).forEach(p -> {
                                String e = folderPath.relativize(p).toString().replace('\\','/');
                                try {
                                    zos.putNextEntry(new java.util.zip.ZipEntry(e));
                                    Files.copy(p, zos);
                                    zos.closeEntry();
                                } catch (IOException ex) { throw new RuntimeException(ex); }
                            });
                        }
                        //byggrequest i s3 bucket med zipNaming
                        PutObjectRequest putReqZip = PutObjectRequest.builder()
                                .bucket(bucketChoice)
                                .key(zipNaming)
                                .contentType("application/zip")
                                .build();
                        //skickar upp temp fil
                        s3Client.putObject(putReqZip, RequestBody.fromFile(tempZip));

                        // deeletear tempZip
                        try {
                            Files.deleteIfExists(tempZip);
                        } catch (IOException e) {
                        }

                        break;

                    case 7:
                        break;
                }
                break;
            }
        }
    }
}