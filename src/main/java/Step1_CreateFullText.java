import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Step1_CreateFullText {









    private static List<File> getFolderFiles(String folderName){


        Drive service = Utils.getService();

        // Print the names and IDs for up to 10 files.
        FileList result = null;
        try {
            result = service.files().list()
                    .setQ(String.format("name='%s'",folderName))
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();

        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {

            for (File file : files) {

                FileList result2 = null;
                try {
                    result2 = service.files().list()
                            .setQ(String.format("'%s' in parents",file.getId()))
                            .execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return result2.getFiles();

            }
        }
        return new ArrayList<>();
    }

    public static void printListFiles(List<File> fileList){
        System.out.println("Found files in "+Constants.DRIVE_ROOT_FOLDER+":");
        for(File file : fileList)
        {
            System.out.printf("%s (%s)\n", file.getName(), file.getId());
        }
        System.out.println();
    }

    public static void mergeFiles(ArrayList<java.io.File> files, java.io.File mergedFile) {

        System.out.println("Merge files:");
        try{
            XWPFDocument document = new XWPFDocument();
            document.createParagraph();

            for(java.io.File file : files)
            {
                XWPFDocument docx = new XWPFDocument(OPCPackage.open(new FileInputStream(file)));
                XWPFRun runText = document.getLastParagraph().createRun();

                List<XWPFParagraph> paragraphs = docx.getParagraphs();
                for(XWPFParagraph paragraph : paragraphs)
                {
                    runText.setText(paragraph.getParagraphText());
                    runText.addBreak();
                }

                runText.addBreak();
                runText.addBreak();

                docx.close();

                System.out.println("merge files "+file.getName());
            }

            FileOutputStream out = new FileOutputStream(mergedFile);
            document.write(out);
            document.close();

        }catch (Exception e){
            e.printStackTrace();
        }



    }

    public static void main(String... args) throws IOException, GeneralSecurityException {

        System.out.println("Start to generate the full text file");
        System.out.println();

        List<File> fileList = getFolderFiles(Constants.DRIVE_ROOT_FOLDER);
        printListFiles(fileList);

        java.io.File ioFile = new java.io.File(Constants.WORKING_FOLDER);
        FileUtils.deleteDirectory(ioFile);
        ioFile.mkdirs();

        ArrayList<java.io.File> downloadedFiles = new ArrayList<>();

        //Download all the transcription files

        System.out.println("Download files :");
        for(String fileName : Constants.FILE_LIST_IN_ORDER)
        {
            for(int i=0; i<fileList.size(); i++)
            {
                File file = fileList.get(i);

                if(file.getName().contentEquals(fileName))
                {
                    fileName = fileName.replaceAll(" ","-");
                    fileName = fileName.replaceAll("/","-");

                    String filePath = String.format("%s/%s.docx",Constants.WORKING_FOLDER,fileName);

                    OutputStream outputStream = new FileOutputStream(filePath);
                    Utils.getService().files().export(file.getId(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                            .executeMediaAndDownloadTo(outputStream);

                    downloadedFiles.add(new java.io.File(filePath));

                    System.out.printf("Downloaded file %s (%s)\n", file.getName(), file.getId());

                    break;
                }
            }
        }
        System.out.println();

        java.io.File newFile = new java.io.File(String.format("%s/%s",Constants.WORKING_FOLDER,Constants.FULL_TEXT_FILE_NAME));
        newFile.createNewFile();

        mergeFiles(downloadedFiles,newFile);

        System.out.println();
        System.out.println("Full text file has been generated in "+newFile.getAbsolutePath());


    }

}