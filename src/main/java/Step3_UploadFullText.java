import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import org.json.JSONObject;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Step3_UploadFullText {

    private static final String TRANSCRIPTION_FOLDER = "tanscriptions";
    private static final String TRANSLATION_FOLDER = "translations";

    private static String getFolderID(String folderName) {

        Drive service = Utils.getService();

        // Print the names and IDs for up to 10 files.
        FileList result = null;
        try {
            result = service.files().list()
                    .setQ(String.format("name='%s'", folderName))
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();

        if (files == null || files.isEmpty()) {
            System.out.println("Error: No files found.");
            return "-1";
        } else {

            if (files.size() > 1) {
                System.out.println("Error: Multiple files found.");
                return "-1";
            } else
                return files.get(0).getId();
        }

    }

    public static String createFolder(String parantFolderID, String folderName) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setParents(Collections.singletonList(parantFolderID));
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        File folder = Utils.getService().files().create(fileMetadata)
                .setFields("id")
                .execute();
        System.out.println("Folder ID: " + folder.getId());

        return folder.getId();
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {

        System.out.println("Start uploading files");
        System.out.println();

        String folderID = getFolderID(Constants.DRIVE_ROOT_FOLDER);

        System.out.println("Creating folders on drive");
        String transcriptionFolderID = createFolder(folderID, TRANSCRIPTION_FOLDER);
        String translationFolderID = createFolder(folderID, TRANSLATION_FOLDER);


        ArrayList<File> uploadedFileList = new ArrayList<>();

        System.out.println();
        System.out.println("Uploading files to drive");
        //Upload files
        java.io.File files2UploadFolder = new java.io.File(Constants.TARGET_FOLDER);
        for (java.io.File file : files2UploadFolder.listFiles()) {
            File fileMetadata = new File();
            fileMetadata.setParents(Collections.singletonList(translationFolderID));
            fileMetadata.setName(file.getName());
            fileMetadata.setMimeType("application/vnd.google-apps.doc");

            java.io.File filePath = new java.io.File(file.getAbsolutePath());
            FileContent mediaContent = new FileContent("text/docx", filePath);
            File uploadedFile = Utils.getService().files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            System.out.println("Upload File name:" + file.getName() + " ID: " + uploadedFile.getId());

            uploadedFile.setName(file.getName());
            uploadedFileList.add(uploadedFile);
        }

        System.out.println();
        System.out.println("Granting permission");

        //Grant permission
        for (File file : uploadedFileList) {
            String fileId = file.getId();
            JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
                @Override
                public void onFailure(GoogleJsonError e,
                                      HttpHeaders responseHeaders)
                        throws IOException {
                    // Handle error
                    System.err.println(e.getMessage());
                }

                @Override
                public void onSuccess(Permission permission,
                                      HttpHeaders responseHeaders)
                        throws IOException {
                    System.out.println("File Name: " + file.getName()+" Permission ID: " + permission.getId());

                }
            };
            BatchRequest batch = Utils.getService().batch();
            Permission userPermission = new Permission()
                    .setType("anyone")
                    .setRole("writer");

            Utils.getService().permissions().create(fileId, userPermission)
                    .setFields("id")
                    .queue(batch, callback);

            batch.execute();


        }

        System.out.println();
        System.out.println("Getting share link");

        JSONObject jsonObject = new JSONObject();

        for(File file : uploadedFileList)
        {
            File tmpFile = Utils.getService().files().get(file.getId()).setFields("webViewLink").execute();
            System.out.println("File Name: " + file.getName()+" share link: " + tmpFile.getWebViewLink());

            String fileName = file.getName().substring(0,file.getName().lastIndexOf("."));
            jsonObject.put(fileName,tmpFile.getWebViewLink());
        }

        //Save share link info
        java.io.File file = new java.io.File(String.format("%s/%s",Constants.WORKING_FOLDER,"shareLinks.json"));
        FileWriter fileWriter = new FileWriter(file);
        String jsonString = jsonObject.toString();
        fileWriter.write(jsonString);
        fileWriter.flush();
        fileWriter.close();
        System.out.println();
        System.out.println(String.format("Save fileName: %s to filePath: %s fileContent: %s",file.getName(),file.getAbsolutePath(),jsonString));


    }

}