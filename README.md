# Chan-Translation-Doc-Generator

There four automate steps in this project, to run the project
1. Setup env variables in Constants.java file
2. Run Step1_CreateFullText.java (It will popup an email verification window at the first time).
   This file download all files specified in Constants.java FILE_LIST_IN_ORDER, and merge them to one full-text file.
   * You will need to manually seperate the file to different sections by seperators 
    ```
    ex. (The third part is word count)
      //Intro part 1 200
      //q1 200
      //q1 part 1 200
    ```
3. Run Step2_CreateSeperateFiles. It divides the full-text file to seperated file by the seperator.
4. Run Step3_UploadFullText. It uploads the full-text, and all seperated files to google drive and grant access authorization to them.
5. Run Step4_GenerateExcelSheet. It generate the final Excel spreadsheet.
   
   ![Image of spreadsheet](https://s3-us-west-1.amazonaws.com/yuan-images/HeartChanTranslationProject.png)

* Tech Use:
1. Use Google Drive API v3 to download/upload files to google drive
2. Use Google Drive API v3 to grant access and get share link
3. User Java POI to create Word/Excel docs
4. Code is developed under IntelliJ editor.
