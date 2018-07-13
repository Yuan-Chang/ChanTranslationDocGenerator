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
import org.apache.commons.io.IOUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;

import java.io.*;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Step4_GenerateExcelSheet {

    static short bgColor = IndexedColors.GREY_25_PERCENT.getIndex();

    public static JSONObject readJsonFile(java.io.File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            String jsonTxt = IOUtils.toString(is, "UTF-8");
            return new JSONObject(jsonTxt);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    static class MyFile {
        private int order;
        private String name;
        private String link;
        private String wordCount;

        public MyFile(int order, String name, String link, String wordCount) {
            this.order = order;
            this.name = name;
            this.link = link;
            this.wordCount = wordCount;
        }

        @Override
        public String toString() {
            return "MyFile{" +
                    "order=" + order +
                    ", name='" + name + '\'' +
                    ", link='" + link + '\'' +
                    ", wordCount=" + wordCount +
                    '}';
        }
    }

    public static void setHyperLink(Workbook workbook, Cell cell, String url, String value)
    {
        CreationHelper createHelper = workbook.getCreationHelper();

        Hyperlink hyperlink = createHelper.createHyperlink(HyperlinkType.URL);
        hyperlink.setAddress(url);
        cell.setHyperlink(hyperlink);
        Font hlinkfont = workbook.createFont();
        hlinkfont.setUnderline(XSSFFont.U_SINGLE);
        hlinkfont.setColor(HSSFColor.BLUE.index);
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.RIGHT);
        cellStyle.setFont(hlinkfont);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(value);
        cell.setCellStyle(cellStyle);
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {

        System.out.println("Start creating sheet");
        System.out.println();

        JSONObject shareLinks = readJsonFile(new java.io.File(String.format("%s/%s", Constants.WORKING_FOLDER, "shareLinks.json")));
        JSONObject wordsCount = readJsonFile(new java.io.File(String.format("%s/%s", Constants.WORKING_FOLDER, "wordsCount.json")));

        String fullTextKey = Constants.FULL_TEXT_FILE_NAME.substring(0, Constants.FULL_TEXT_FILE_NAME.lastIndexOf("."));
        String fullTextSharedLink = shareLinks.getString(fullTextKey);

        ArrayList<MyFile> fileNameList = new ArrayList<>();
        for (String key : wordsCount.keySet()) {
            int sIndex = key.indexOf("-");
            Integer order = Integer.parseInt(key.substring(0, sIndex));
            String fileName = key.substring(sIndex + 1);
            String shareLink = shareLinks.getString(fileName);
            String wordCount = wordsCount.getString(key);

            MyFile myFile = new MyFile(order, fileName, shareLink, wordCount);
            fileNameList.add(myFile);
        }

        fileNameList.sort(new Comparator<MyFile>() {
            @Override
            public int compare(MyFile o1, MyFile o2) {
                return o1.order - o2.order;
            }
        });

        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("ExcelSheet");

        //Create font
        Font redFont = workbook.createFont();
        redFont.setColor(IndexedColors.RED.getIndex());

        Font whiteFont = workbook.createFont();
        whiteFont.setColor(IndexedColors.WHITE.getIndex());

        //row 0
        Row headerRow = sheet.createRow(0);
        Cell cell = headerRow.createCell(0);
        cell.setCellValue(Constants.DRIVE_ROOT_FOLDER);
        CellStyle cellHorizontalCenterStyle = workbook.createCellStyle();
        cellHorizontalCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        cellHorizontalCenterStyle.setFont(redFont);
        cellHorizontalCenterStyle.setFillForegroundColor(bgColor);
        cellHorizontalCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(cellHorizontalCenterStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        //row 1
        headerRow = sheet.createRow(1);

        //left
        cell = headerRow.createCell(0);
        cell.setCellValue("Status : Open");
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        cellStyle.setFont(redFont);
        cellStyle.setFillForegroundColor(bgColor);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(cellStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));

        //right
        cell = headerRow.createCell(3);
        setHyperLink(workbook,cell,fullTextSharedLink,"Original file link");

        cell.getCellStyle().setFillForegroundColor(bgColor);
        cell.getCellStyle().setFillPattern(FillPatternType.SOLID_FOREGROUND);

        sheet.addMergedRegion(new CellRangeAddress(1, 1, 3, 5));

        //row 2
        headerRow = sheet.createRow(2);
        cell = headerRow.createCell(0);

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = new Date();

        cell.setCellValue("start date : "+dateFormat.format(date));
        cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        cellStyle.setFillForegroundColor(bgColor);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cell.setCellStyle(cellStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 5));

        //row 3
        String[] headerTitle = {"Section", "Words", "Name", "Start date", "Status", "File link"};
        headerRow = sheet.createRow(3);
        for (int i = 0; i < headerTitle.length; i++) {
            cell = headerRow.createCell(i);
            cell.setCellValue(headerTitle[i]);

            cellStyle = workbook.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.LEFT);

            cellStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            cellStyle.setFont(whiteFont);

            cell.setCellStyle(cellStyle);
        }

        //data raw

        int rowCount = 4;
        for (int i = 0; i < fileNameList.size(); i++) {
            headerRow = sheet.createRow(rowCount++);
            MyFile file = fileNameList.get(i);
            for (int j = 0; j < headerTitle.length; j++) {
                cell = headerRow.createCell(j);
                cellStyle = workbook.createCellStyle();
                cellStyle.setAlignment(HorizontalAlignment.LEFT);
                cellStyle.setFillForegroundColor(bgColor);
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                cell.setCellStyle(cellStyle);

                if (j == 0) {
                    cell.setCellValue(file.name);
                } else if (j == 1) {
                    cell.setCellValue(file.wordCount);
                } else if (j == 5) {
                    setHyperLink(workbook,cell,file.link,"link");
                    cell.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
                    cell.getCellStyle().setFillForegroundColor(bgColor);
                    cell.getCellStyle().setFillPattern(FillPatternType.SOLID_FOREGROUND);
                } else
                    cell.setCellValue("");
            }

        }

        //extra raws

        for (int i = 0; i < 4; i++) {
            headerRow = sheet.createRow(rowCount++);
            for (int j = 0; j < headerTitle.length; j++) {
                cell = headerRow.createCell(j);
                cellStyle = workbook.createCellStyle();
                cellStyle.setAlignment(HorizontalAlignment.LEFT);
                cellStyle.setFillForegroundColor(bgColor);
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                cell.setCellStyle(cellStyle);
                cell.setCellValue("");
            }
        }

        //foot note
        headerRow = sheet.createRow(rowCount);
        cell = headerRow.createCell(0);
        cell.setCellValue("Status : In progress, Done, Help Needed");
        cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);

        cellStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        cell.setCellStyle(cellStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowCount, rowCount+1, 0, 5));


        java.io.File result = new java.io.File(String.format("%s/%s", Constants.WORKING_FOLDER, "ExcelSheet.xlsx"));

        // Write the output to a file
        FileOutputStream fileOut = new FileOutputStream(result);
        workbook.write(fileOut);
        fileOut.close();

        // Closing the workbook
        workbook.close();

        System.out.println("Excel sheet is created in path : "+result.getAbsolutePath());

    }

}