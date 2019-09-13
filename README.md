# Summary
The purpose of this repo is to parse a CSV file, check for completeness of rows, and output the completed rows to a SQLite database. The rows which don't meet the specifications of completeness are outputed to a new CSV file called <filename>-bad.csv. A log file is also created with the total number of rows, the number of good rows, and the number of bad rows.

# Developer Notes
The repo is a maven build and should function from the /target/ folder after the command "mvn package assembly:single" is ran. The dependencies are https://github.com/xerial/sqlite-jdbc version 3.27.2.1 and https://commons.apache.org/proper/commons-csv/ version 1.7. The first of the dependencies is to be able to succesfully read and write to an SQLite database while the second dependency is to parse the CSV file. Within the build is an already assembled .jar of the program, these instructions and notes are for the case that it does not work.

# Running the Program
The program is a console program and requires one argument to run: the name of the CSV file being parsed. Therefore, to run the jar file already existent in the target/ folder, the following command should be given "database_app-1.0-SNAPSHOT-jar-with-dependencies.jar "Entry Level Coding Challenge Page 2.csv"" assuming that Entry Level Coding Challenge Page 2.csv is contained within the same folder as the jar.

# Approach
While the original file was 10 columns, the assumption that other files may not have the same number of columns was made and thus it was chosen to make the program more versitle. This was done by assuming the file came with a header and that the header would be the proper number of columns before basing the rest of the file on that count.

Another strong focus was to bring the speed of the program down to a reasonable level. This was done by turning auto commit off and commiting only at the end of the batch of inserts created by the program. This means only one query is done to the database rather then one for each row that exists. Since querying the database is by far the largest chunk of time taken, this cut down the program runtime from several minutes to just a few seconds with 6002 rows including the header.
