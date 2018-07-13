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
import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.*;
import org.json.JSONObject;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Step2_CreateSeperateFiles {

    //Seperator
    //Intro part 1 200
    //q1 200
    //q1 part 1 200




    public static String extractFileName(String line) {

        int lastIndex = line.trim().lastIndexOf(" ");
        line = line.substring(0, lastIndex);

        return line;
    }

    public static String extractWordsCount(String line) {

        int lastIndex = line.trim().lastIndexOf(" ");
        line = line.substring(lastIndex+1);

        return line;
    }

    public static boolean isTitle(String line) {

        line = line.trim().toLowerCase();

        //Intro part 1 200
        //q1 200
        //q1 part 1 200
        return Pattern.matches("intro part [0-9]+ [0-9]+", line) ||
                Pattern.matches("q[0-9]+ [0-9]+", line) ||
                Pattern.matches("q[0-9]+ part *[0-9]+ [0-9]+", line);
    }

    public static boolean shouldSkip(String line) {
        return line.startsWith("====") ||
                line.startsWith("----");
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void addPreText(XWPFDocument document) {

        InputStream in = Step1_CreateFullText.class.getResourceAsStream("preText.txt");
        String text = convertStreamToString(in);

        String[] lines = text.split("\n");
        for (int i=0; i<lines.length; i++) {

            String line = lines[i];

            XWPFParagraph paragraph = document.getLastParagraph();
            XWPFRun runText = paragraph.createRun();
            runText.setColor("4169e1");

            runText.setText(line);
            runText.addBreak();
        }

    }

    public static void main(String... args) throws IOException, GeneralSecurityException {

        System.out.println("Start to seperate file");
        System.out.println();

        java.io.File fullTextFile = new java.io.File(String.format("%s/%s", Constants.WORKING_FOLDER, Constants.FULL_TEXT_FILE_NAME));

        java.io.File ioFile = new java.io.File(Constants.TARGET_FOLDER);
        FileUtils.deleteDirectory(ioFile);
        ioFile.mkdirs();
        System.out.println(String.format("Copy full text to folder %s", Constants.TARGET_FOLDER));
        FileUtils.copyFileToDirectory(fullTextFile, ioFile);

        JSONObject jsonObject = new JSONObject();
        int fileOrder = 1;

        java.io.File newFile = null;
        XWPFDocument document = null;

        try {
            XWPFDocument docx = new XWPFDocument(OPCPackage.open(new FileInputStream(fullTextFile)));
            List<XWPFParagraph> paragraphs = docx.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getParagraphText();
                String[] lines = text.split("\\n");

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];

//                    System.out.println(line+"**");

                    if (isTitle(line)) {
                        //write content to file
                        if (document != null) {
                            FileOutputStream out = new FileOutputStream(newFile);
                            document.write(out);
                            document.close();
                        }

                        String fileName = extractFileName(lines[i]);
                        fileName = fileName.replaceAll(" ","-");
                        String wordsCount = extractWordsCount(lines[i]);
                        newFile = new java.io.File(String.format("%s/%s.docx", Constants.TARGET_FOLDER ,fileName));

                        jsonObject.put(String.format("%d-%s",fileOrder++,fileName),wordsCount);

                        document = new XWPFDocument();
                        document.createParagraph();

                        addPreText(document);

                        System.out.println(String.format("Create file %s",newFile.getAbsolutePath()));
                        continue;
                    }

                    if (document == null)
                        continue;
                    if (shouldSkip(line))
                        continue;

                    XWPFRun runText = document.getLastParagraph().createRun();
                    runText.setText(line);
                    runText.addBreak();

                }


            }

            if (document != null) //write content to file
            {
                FileOutputStream out = new FileOutputStream(newFile);
                document.write(out);
                document.close();
            }

            //Save words count info
            java.io.File file = new java.io.File(String.format("%s/%s", Constants.WORKING_FOLDER,"wordsCount.json"));
            FileWriter fileWriter = new FileWriter(file);
            String jsonString = jsonObject.toString();
            fileWriter.write(jsonString);
            fileWriter.flush();
            fileWriter.close();
            System.out.println();
            System.out.println(String.format("Save fileName: %s to filePath: %s fileContent: %s",file.getName(),file.getAbsolutePath(),jsonString));

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}