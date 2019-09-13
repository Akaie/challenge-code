package io.github.akaie.database_app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;



public class App {
	public static Connection connect(String s) throws SQLException, IOException {
		new File("database\\").mkdirs();
		String url = "jdbc:sqlite:database\\"+s+".db";
		return DriverManager.getConnection(url);
	}
	
	public static void createTable(Connection c, int columns) throws SQLException{
		Statement st = c.createStatement();
		st.executeUpdate("DROP TABLE IF EXISTS challenge;");
		String tableSQL = "CREATE TABLE challenge (\n";
		for(int i = 0; i < columns; i++) {
			if(i == columns-1)
				tableSQL += (char)(65+i)+" TEXT NOT NULL \n";
			else
				tableSQL += (char)(65+i)+" TEXT NOT NULL, \n";
		}
		tableSQL += ");";
		st.executeUpdate(tableSQL);
	}
	
	public static PreparedStatement createPreparedStatement(Connection c, int columns) throws SQLException {
		//Prepare the statement to be used for batch insert
		String preparedSQL = "INSERT INTO challenge VALUES(";
		for(int i = 0; i < columns - 1; i++) {
			preparedSQL += "?,";
		}
		preparedSQL += "?);";
		return c.prepareStatement(preparedSQL);
	}

	public static void main(String[] args) {
		
		try {
			//Checks to make sure we have at least 1 argument passed to the program
			if(args.length < 1) {
				System.out.println("A csv file must be provided!");
				return;
			}
			//Splits the passed args[0] along the .
			String[] hol = args[0].split("\\.");
			//Checks to make sure args[0] passed is a valid csv file
			if(hol.length < 2 || !hol[hol.length-1].equalsIgnoreCase("csv")) {
				System.out.println("File is invalid, must be a csv file!");
				return;
			}
			//Compiles the name of the file, done encase there are other periods in the name
			String name = "";
			for(int i = 0; i < hol.length - 1; i++ ) {
				name = name + hol[i];
			}
			//Parse the CSV file with Apache Common CSV parser
			System.out.println("Parsing CSV file.");
			CSVParser parser = CSVParser.parse(new FileReader(new File(args[0])), CSVFormat.DEFAULT.withHeader());
			//Get the number of columns by checking the number of headers
			System.out.println("Computing number of columns.");
			int columns = parser.getHeaderNames().size();
			//Database connection and setup
			System.out.println("Creating and establishing connection with database.");
			Connection c = connect(name);
			c.setAutoCommit(false);
			System.out.println("Creating table within database.");
			createTable(c, columns);
			PreparedStatement ps = createPreparedStatement(c, columns);
			//Create a bufferedreader because CSVparser can't tostring the original lines
			System.out.println("Opening file to parse for bad rows.");
			BufferedReader r = new BufferedReader(new FileReader(new File(args[0])));
			//Read the first line of the bufferedreader
			String l = r.readLine();
			//Create the BufferedWriter to write to the bad file
			System.out.println("Opening "+name+"-bad.csv to write bad rows to.");
			BufferedWriter w = new BufferedWriter(new FileWriter(new File(name+"-bad.csv")));
			//Variables for keeping track of good and bad counts
			int goodcount = 0;
			int badcount = 0;
			//For each CSVRecord, run code
			System.out.println("Sorting rows.");
			for(CSVRecord re : parser) {
				//If record size is not the column number, there are missing columns for this set, add it to the bad file
				if(re.size() != columns) {
					w.append(l+"\n");
					l = r.readLine();
					badcount++;
					continue;
				}
				//If any of the records are " " or "", there are missing pieces of data, add it to the bad file and continue the loop
				//Boolean flag is used because while loop cannot be continued inside for loop
				boolean breakflag = false;
				for(int i = 0; i < columns; i++) {
					if(re.get(i).equalsIgnoreCase(" ") || re.get(i).equalsIgnoreCase("")) {
						breakflag = true;
						break;
					}
				}
				if(breakflag) {
					w.append(l+"\n");
					l = r.readLine();
					badcount++;
					continue;
				}
				//If entry is header, add it to the bad count and continue
				if(re.get(0).equalsIgnoreCase("A")) {
					w.append(l+"\n");
					l = r.readLine();
					badcount++;
					continue;
				}
				//Set each record to corresponding column in data
				for(int i = 0; i < columns; i ++ ) {
					ps.setString(i+1, re.get(i));
				}
				//Add the PreparedStatement batch
				ps.addBatch();
				//We finished, increase the good row count
				goodcount++;
				//Increment the BufferedReader to keep it in line with the record parser
				l = r.readLine();
			}
			//Execute the batch insert then commit
			System.out.println("Executing batch insert of data.");
			ps.executeBatch();
			c.commit();
			System.out.println("Execution complete.");
			//Close the Bufferedreader, bufferedwriter, and database connection
			r.close();
			w.close();
			c.close();
			//Print out the records information
			System.out.println("Total records (without header): " + (badcount+goodcount));
			System.out.println("Good records: " + goodcount);
			System.out.println("Bad records: " + badcount);
			//Write the record information to .log file
			BufferedWriter log = new BufferedWriter(new FileWriter(new File(name+".log")));
			log.write("Total Records (without header): "+(badcount+goodcount)+"\n");
			log.write("Good Records: " + goodcount+"\n");
			log.write("Bad Records: " + badcount);
			log.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("File was not found or is being used by another program (this includes the list of bad rows file)!");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("File could not be loaded!");
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Something went wrong with the database connection!");
		}
	}
}
