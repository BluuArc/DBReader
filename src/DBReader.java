import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Scanner;
import org.json.*;
//source: http://theoryapp.com/parse-json-in-java/

public class DBReader {
	public static boolean debug = false;

	public static void main(String[] args) {
		String version = "v1.0";
		String editDate = "8/12/16";
		printMessage("Version: " + version + "\nBuild Date: " + editDate, false);
		
		if(SimpleInput.getYesNoOption("Would you like to enable debug mode?") == 1)
			debug = true;
		else
			debug = false;
		
		printMessage("Debug mode is on.", true);
		
		printMessage("Showing options dialog box.", true);
		Object[] options = {"Choose JSON Locally", "Download JSON", "Exit"};
		int answer = SimpleInput.getListOption("Choose one", options);
		
		//choose a file
		String file = "";
		//choose locally
		if(answer == 0){
			printMessage("Choose JSON file.", false);
			file = FileChooser.pickAFile();
			setDirectory(file);
		}else if(answer == 1){
			//download file
			//DL JSON file
			String url = SimpleInput.getString("Enter URL of JSON file. (Cancel to choose local JSON file)");
			
			printMessage("Choose directory to save JSON file", false);
			file = FileChooser.pickAFile();
			setDirectory(file);
			
			printMessage("Downloading JSON file.", false);
			try{
				file = downloadJSONFromURL(url);
				printMessage("Finished downloading JSON file.",false);
			} catch(IOException e){
				printMessage("IOException from downloadJSONFromURL",false);
			}
		}else if(answer == 2)
			System.exit(0);
		else{
			System.err.println("Error: Unkown input [" + answer + "]");
		}
		//end else
		String output = FileChooser.getMediaDirectory() + 
				"\\" + file.substring(file.lastIndexOf(File.separatorChar)+1) +"-output.tsv";
		
		printMessage("Output will be saved to [" + output + "]",false);
		
		//input file check
		if(!file.contains(".json")){
			printMessage("Error: [" + file.toString() + "] is not a .json file. Exiting program.",false);
			System.exit(1);
		}
		
		//output file check
		File f = new File(output);
		if(f.exists()){
			if(!f.delete()){
				System.err.println("Error: Deletion of [" + output + "] failed. Exiting program.");
				System.exit(1);
			}
			printMessage("Deleted old file [" + output + "]",false);
		}

		//get keys from input
		//ex: id,guide_id,name
		String keys = SimpleInput.getString("Enter the keys you'd like to get values for separated by commas\n"
				+ "Ex: id,guide_id,name for unit ID, guide ID, and unit name");
		
		//setParams
		//TODO: create options box to decide type of search
		String params = "normalSearch";//"deepSearchFind,10017,queryKey,ubb,printType";
		printMessage("Keys are [" + keys +"]\nParams are [" + params + "]", true);
		//read file
		slowParse(params, file, output, keys);
		System.exit(0);
	}//end main
	
	/**
	 * Set current directory for FileChooser.
	 * 
	 * @param fName a file path as a String to get directory from
	 */
	public static void setDirectory(String fName) {
		int pos = fName.lastIndexOf(File.separatorChar);
		String dir = fName.substring(0, pos);
		FileChooser.setMediaPath(dir);
	}// end setDirectory method
	
	/**
	 * Print a message to the console. Mainly for convenience purposes.
	 * 
	 * @param s			a String to print out
	 * @param isDebug	a conditional to print messages iff debug is true
	 */
	public static void printMessage(String s, boolean isDebug){
		if(isDebug && debug)
			System.out.println("[DEBUG]: " + s);
		else if (!isDebug)
			System.out.println(s);
	}
	
	/**
	 * Print something with a number of tabs. Usually
	 * for formatting output with tabs.
	 * 
	 * @param s			a String to print
	 * @param numTabs	the number of tabs that precedes the string
	 * @param isDebug	a conditional to print messages iff debug is true
	 */
	public static void printTabbedMessage(String s, int numTabs, boolean isDebug){
		String msg = "";
		for(int i = 0; i < numTabs; ++i){
			msg += "  ";	//tab is 2 spaces
		}
		printMessage(msg+s,false);
	}//end printTabbedMessage
	
	/**
	 * Append a string to the end of a text file.
	 * 
	 * @param outFile		a file (as a String) to append to
	 * @param s				a String to append to file	
	 * @throws IOException	If there is an error with the file
	 */
	public static void appendToFile(String outFile, String s) throws IOException{
		File f = new File (outFile);
		Path p = f.toPath();
		byte[] data = s.getBytes();
		
		if(!s.endsWith("\n"))
			s += '\n';
		
		//System.out.print("Appending:\t" + s);
		try (OutputStream out = new BufferedOutputStream(
	      Files.newOutputStream(p, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
	      out.write(data, 0, data.length);
	    } catch (IOException x) {
	      System.err.println(x);
	    }
	}//end appendToFile
	
	/**
	 * Get the filename of a given file path
	 * 
	 * @param fName	a file path (as a String) to get the file name of
	 * @return		the file name as a String
	 */
	public static String getFilename(String fName) {
		int pos = fName.lastIndexOf("/");
		fName = fName.substring(pos + 1, fName.length());
		return fName;
	}//end getFilename

	/**
	 * Download a JSON file from a URL string.
	 * 
	 * @param s				a string URL
	 * @return				the file path of the downloaded file
	 * @throws IOException	if there was a file error
	 */
	public static String downloadJSONFromURL(String s) throws IOException{
		URL url = null;
		try {
			url = new URL(s);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		String fName = FileChooser.getMediaDirectory() + "\\" 
				+ getFilename(url.getFile());
		fName = fName.substring(0, fName.lastIndexOf(".")) + ".json";
		printMessage("File will be saved to [" + fName +"]",false);
		Scanner scan = new Scanner(url.openStream());
		String str = new String();
		int i = 0;
		while(scan.hasNext()){
			str = scan.nextLine();
			appendToFile(fName, str + "\n");
			i++;
			if(i%10000 == 0){
				System.out.print(".");
			}
			if(i%250000 == 0){
				System.out.print("\n");
			}
		}
		scan.close();
		
		return fName;
	}//end readJSONFromURL
	
	
	public static void printUnitInfo(String unitID, JSONObject unitObject){
		//TODO: fix this
	}
	
	/**
	 * Output the values of the keys of a JSON unit object.
	 * Leave queryKey parameter blank to print all possible keys.
	 * 
	 * @param obj		the JSON object to read the keys from
	 * @param key		the string that indicates where the unit object is
	 * @param numTabs	the number of tabs to use when outputting to console
	 * @param printType	the boolean that decides whether or not to print the
	 * 					type of object of the key being printed
	 * @param queryKey	the specific key to print the value of			
	 */
	public static void deepSearch(JSONObject obj, String key, int numTabs, boolean printType, String queryKey){
		//get list of all things in current json string
		JSONObject inUnit = null;
		//try to get correct JSON object
		if(key.length() == 0){
			printMessage("Trying to get name automatically.",true);
			inUnit = obj.getJSONObject(getObjectNameFromJSONObject(obj));
			if(numTabs == 0)
				printTabbedMessage(getObjectNameFromJSONObject(obj) + " [JSONObject]: ", numTabs++, true);
		}else if(key.equals("use-this-object")){
			printMessage("Using this object", true);
			inUnit = obj;
		}else{
			printMessage("Getting object from key [" + key + "]",true);
			try{
				inUnit = (JSONObject) obj.get(key);
			} catch(JSONException e){
				deepSearchArray(obj.getJSONArray(key), numTabs, printType, queryKey);
				//return "";
			}
		}//end if else get JSON object
		
		String[] queries = new String[1];
		if(queryKey.contains("."))
			queries = queryKey.split(".");
		else
			queries[0] = queryKey;
		
		//modified from JsonUtil2.getListFromJSONObject
		Iterator<String> keys = inUnit.keys();
		String currKey = "";
		String msg = "";
		printMessage("Printing keys in " + getObjectNameFromJSONObject(obj), true);
		//print keys
		//TODO: Change print messages to output to a file instead
		while (keys.hasNext()) {
	        currKey = keys.next();
	        Object temp = inUnit.get(currKey);
	        String type = "";
	        msg = currKey;
        	printMessage("Checking " + currKey + ": " + temp,true);
	        //check type of temp object
	        if(temp instanceof String){
	        	type = "String";
	        } else if(temp instanceof Integer){
	        	type = "Integer";
			} else if(temp instanceof Boolean){
				type = "Boolean";
	        } else if(temp instanceof JSONArray){
	        	type = "JSONArray";
	        	if(printType)
	        		msg += " [" + type + "]";
	        	msg += ": ";
	        	if(queryKey.length() == 0)
	        		printTabbedMessage(msg, numTabs,true);
	        	
	        	if(!currKey.matches(queryKey)){
		        	JSONArray tempArray = (JSONArray)temp;
		        	deepSearchArray(tempArray, numTabs+1, printType, queryKey);
	        	}else{
	        		JSONArray tempArray = (JSONArray)temp;
		        	deepSearchArray(tempArray, numTabs+1, printType, "");
	        	}
	        } else if(temp instanceof JSONObject){
	        	type = "JSONObject";
	        	if(printType)
	        		msg += " [" + type + "]";
	        	msg += ": ";
	        	if(queryKey.length() == 0)
	        		printTabbedMessage(msg, numTabs,true);
	        	
	        	if(!currKey.matches(queryKey))
	        		deepSearch((JSONObject)temp, "use-this-object", numTabs+1, printType, queryKey);
	        	else
	        		deepSearch((JSONObject)temp, "use-this-object", numTabs+1, printType, "");
	        } else{
	        	type = "Other";
	        }//end if else JSON object check
	        
	        if(!(type.equals("JSONArray") || type.equals("JSONObject"))){
	        	if(printType)
	        		msg += " [" + type + "]";
	        	msg += ": ";
	        	if(queryKey.length() == 0)
	        		printTabbedMessage(msg + temp, numTabs,true);
	        	else{
	        		if(currKey.matches(queryKey)){
	        			printMessage(msg + temp,true);
	        			
	        		}
	        	}//end else queryKey search
	        }//end if not JSON
	        if(currKey.matches(queryKey))
	        	break;
		}//end while hasNext
		//printMessage("Finished printing keys");
		//return "Done";
	}//end deepSearch
	
	/**
	 * Output all possible keys of a JSON array.
	 * 
	 * @param jArray	the JSONArray whose keys are being printed
	 * @param numTabs	the number of tabs to use when outputting to console
	 * @param printType	the boolean that decides whether or not to print the
	 * 					type of object of the key being printed
	 * @param queryKey	the specific key to print the value of
	 * @see #deepSearch(JSONObject, String, int, boolean, String)
	 */
	public static void deepSearchArray(JSONArray jArray, int numTabs, boolean printType, String queryKey){
		for(int i = 0; i < jArray.length(); ++i){
			Object temp = jArray.get(i);
			String type = "";
			String msg = "";
			//check type of temp object
			//TODO: Change print messages to print to a file instead
			if(temp instanceof String){
	        	type = "String";
	        } else if(temp instanceof Integer){
	        	type = "Integer";
			} else if(temp instanceof Boolean){
				type = "Boolean";
	        } else if(temp instanceof JSONObject){
	        	type = "JSONObject";
	        	if(queryKey.length() == 0)
	        		printTabbedMessage("Index  " + i, numTabs+1,true);
	        	deepSearch((JSONObject)temp, "use-this-object", numTabs+2, printType, queryKey);
	        } else if(temp instanceof JSONArray){
	        	type = "JSONArray";
	        	if(queryKey.length() == 0)
	        		printTabbedMessage("Index  " + i, numTabs+1,true);
	        	JSONArray tempArray = (JSONArray)temp;
	        	deepSearchArray(tempArray, numTabs+2, printType, queryKey);
	        } else{
	        	type = "other";
	        }//end if else JSON object check
	        
	        if(!(type.equals("JSONArray") || type.equals("JSONObject"))){
	        	//printTabbedMessage("Index  " + i, numTabs+1);
	        	msg = "Index " + i;
	        	if(printType)
	        		msg += " [" + type + "]";
	        	msg += ": ";
	        	if(queryKey.length() == 0)
	        	printTabbedMessage(msg + temp, numTabs+2,true);
	        }
		}
	}//end deepSearchArray
	
	/**
	 * Get the specified property from current JSON object. Note: won't go 
	 * into other objects or arrays.
	 * <p>
	 * Possible Properties:
	 * category, cost, drop check count, element, exp_pattern, gender, getting type,
	 * guide_id, id, kind, lord damage range, name, rarity, sell caution
	 * 
	 * @param obj		the JSONObject that contains the unit data
	 * @param property	the property to return
	 * @return			the property's value
	 */
	public static String getPropertyFromJSONObject(JSONObject obj, String property){
		//put into JSON object
		
		String objName = getObjectNameFromJSONObject(obj);

		//query
		String msg = null;
		
		//get property
		try{
			if(obj.getJSONObject(objName).get(property) instanceof String){
				msg = obj.getJSONObject(objName).getString(property);
			}else if(obj.getJSONObject(objName).get(property) instanceof Integer){
				msg = Integer.toString(obj.getJSONObject(objName).getInt(property));
			}else if(obj.getJSONObject(objName).get(property) instanceof JSONObject){
				msg = obj.getJSONObject(objName).getJSONObject(property).toString();
			}else if(obj.getJSONObject(objName).get(property) instanceof JSONArray){
				msg = obj.getJSONObject(objName).getJSONArray(property).toString();
			}
		}catch(JSONException e){
			System.err.println(e);
		}finally{
			if(msg == null)
				return "Can't find property [" + property + "]";
		}
		return msg;
	}//end getPropertyFromJSONObject
	
	/**
	 * Gets name of a string of a JSON object. Also assumes that string has 1 JSON object.
	 * 
	 * @param input	the JSONObject as a string
	 * @return		the name of the JSON object
	 * @deprecated
	 * @see #getObjectNameFromJSONObject(JSONObject)
	 */
	public static String getObjectNameFromJSONString(String input){
		String objectName = "";
		JSONObject obj = new JSONObject(input);
		String temp = obj.toString();
		
		objectName = temp.substring(temp.indexOf("\"")+1, temp.indexOf(":")-1);
		
		return objectName;
	}//end getOjbectNameFromJSONString
	
	/**
	 * Gets name of a JSON object. Also assumes that string has 1 JSON object.
	 * 
	 * @param obj	the JSONObject to get the name of
	 * @return		the name of the JSON object
	 */
	public static String getObjectNameFromJSONObject(JSONObject obj){
		String objectName = "";
		String temp = obj.toString();
		//printMessage(temp);
		
		objectName = temp.substring(temp.indexOf("\"")+1, temp.indexOf(":")-1);
		
		return objectName;
	}//end getObjectNameFromJSONString
	
	/**
	 * Parse a JSON file and read/output data based on the parameters. Can print a property
	 * of all units and/or print the data of a specific unit.
	 * 
	 * @param params	a list of parameters separated by commas
	 * @param file		the JSON file to read
	 * @param outFile	the file to output data
	 * @param keyString	a list of keys to print the data of, separated by commas
	 */
	public static void slowParse(String params, String file, String outFile, String keyString){
		//declare variables
		BufferedReader br = null;	//file reader
		String line = " ";			//current line
		String out = "{\n";			//isolated JSON object
		boolean stillRead = false;	//flag to read lines
		String[] keys = keyString.split(","); 
		String[] paramsArray = params.split(",");
		JSONObject jsonUnit = null;
		
		//set parameters
		boolean deepSearchOp = false;
		boolean normalSearch = false;
		boolean printType = false;
		String unitID = "";
		String queryKey = "";
		
		//assume normalSearch if there are no parameters
		if(paramsArray.length == 0)
			normalSearch = true;
		for(int i = 0; i < paramsArray.length; ++i){
			if(paramsArray[i].equals("normalSearch")){//do a normal search
				printMessage("normalSearch enabled", true);
				normalSearch = true;
			}
			if(paramsArray[i].equals("deepSearchFirst")){//get info of first unit in list
				printMessage("deepSearch (first) enabled", true);
				deepSearchOp = true;
			}
			if(paramsArray[i].equals("deepSearchFind")){//get info of unit given
				printMessage("deepSeach (find) enabled", true);
				deepSearchOp = true;
				unitID = paramsArray[i+1];
			}
			if(paramsArray[i].equals("printType")){//print type of output in deepSearch (e.g. String, int, boolean, etc.)
				printMessage("printType enabled", true);
				printType = true;
			}
			if(paramsArray[i].equals("queryKey") && deepSearchOp){ //requires deepSearch to be enabled; find specific part of unit (bb, name, etc.)
				printMessage("queryKey search ready", true);
				queryKey = paramsArray[i+1];
			}
		}
		
		//open file
		printMessage("Opening [" + file + "]",false);
		File f = new File(file);
		//int i = 0;
		int j = 0;
		String temp = "";
		
		//read file
		try{
			printMessage("Reading file",false);
			br = new BufferedReader(new FileReader(f));
			//i = 1;
			//print columns
			if(normalSearch){
				printMessage("Adding columns to output file", true);
				try {
					for(j = 0; j < keys.length; ++j){
						temp = keys[j];
						if(j == (keys.length-1))
							temp += "\n";
						else
							temp += "\t";
						appendToFile(outFile, temp);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//while not at EOF
			while((line = br.readLine()) != null){
				//read file
				//printMessage("Line is [" + line + "]",true);
				
				//reach very end of database JSON
				//if at end of file and still reading into out String
				if(line.equals("}") && stillRead){
					//stop reading
					printMessage("Reach end of database", true);
					stillRead = false;
					
					//end with closing bracket
					out += line;
					
					//put string into JSON object
					jsonUnit = new JSONObject(out);
					printMessage("This unit is " + getObjectNameFromJSONObject(jsonUnit), true);
					
					if(deepSearchOp){
						if(unitID.length() == 0 || unitID.equals(getObjectNameFromJSONObject(jsonUnit))){
							printMessage("Attempting deepSearch on unit", true);
							//deepSearchFirst
							deepSearch(jsonUnit, "", 0, printType, queryKey);
							deepSearchOp = false;
						}
					}//end deepSearchOp
					
					String msg = "";
					if(normalSearch){
						//get unit info
						for(j = 0; j < keys.length; ++j){
							printMessage("Attempting to key [" + keys[j] + "] from unit", true);
							if(j != (keys.length-1))
								temp = "\t";
							msg += getPropertyFromJSONObject(jsonUnit, keys[j]) + temp; 
						}
						
						//append to file
						printMessage("Appending msg to file", true);
						try {
							printMessage(msg, false);
							appendToFile(outFile, msg + "\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}//end if normalSearch
					
					break;
				}//end if at last bracket before EOF
				
				//reach unit line
				if(line.startsWith("    \"")/* || line.startsWith("\t\"")*/){					
					if(!stillRead){
						//begin reading
						printMessage("Begin reading", true);
						stillRead = true;
					}else{
						//stop reading
						printMessage("Stop reading", true);
						stillRead = false;
						
						//end with closing bracket
						out += "\n}";
						
						//put string into JSON object
						jsonUnit = new JSONObject(out);
						
						printMessage("This unit is " + getObjectNameFromJSONObject(jsonUnit), true);
						
						if(deepSearchOp){
							printMessage("Attempting deepSearch", true);
							printMessage("unitID: [" + unitID + "]\t objName: [" + getObjectNameFromJSONObject(jsonUnit) + "]",true);
							if(unitID.length() == 0 || unitID.equals(getObjectNameFromJSONObject(jsonUnit))){
								//deepSearchFirst
								deepSearch(jsonUnit, "", 0, printType, queryKey);
								deepSearchOp = false;
							}
						}//end deepSearchOp
						
						String msg = "";
						if(normalSearch){
							//get unit info
							for(j = 0; j < keys.length; ++j){
								printMessage("Attempting to key [" + keys[j] + "] from unit", true);
								if(j != (keys.length-1))
									temp = "\t";
								else
									temp = "";
								msg += getPropertyFromJSONObject(jsonUnit, keys[j]) + temp; 
							}
							
							//append to file
							printMessage("Appending msg to file", true);
							try {
								printMessage(msg, false);
								appendToFile(outFile, msg + "\n");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}//end normalSearch
						
						//start over
						printMessage("Start reading",true);
						out = "{\n";
						stillRead = true;
					}//end else
					
					//increment for every unit
					//i++;
					/*
					//progress indicator
					if(i%100 == 0){
						System.out.print(".");
					}
					*/
				}//end if unit line
				
				//add line to out string if still reading
				if(stillRead)
					out += line;
				
				//stop early if done searching
				if(!normalSearch && !deepSearchOp){
					printMessage("Done searching", true);
					break;
				}
			}//end while
			System.out.print("Done!\n");
			//close scanner after successfully reading file
			//printMessage("Finished reading file");
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//end try/catch
	}// end slowParse method
}//end class
