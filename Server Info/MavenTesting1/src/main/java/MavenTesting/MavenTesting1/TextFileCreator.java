//package MavenTesting.MavenTesting1;
//
//import org.springframework.web.bind.annotation.*; 
//import org.springframework.http.*;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileWriter;
//
//import javax.servlet.http.*;
//
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//
//import org.json.JSONObject;
//
//@RestController
//public class TextFileCreator {
//	@RequestMapping(value = "/writeFile", method = RequestMethod.POST) // <-- setup the endpoint URL at /hello with the HTTP POST method
//	public ResponseEntity<String> writeFile(@RequestBody String payload, HttpServletRequest request) {
//		
//		/*Creating http headers object to place into response entity the server will return.
//		This is what allows us to set the content-type to application/json or any other content-type
//		we would want to return */
//		HttpHeaders responseHeaders = new HttpHeaders();
//		
//		try {
//			FileWriter fw = new FileWriter(new File("textFile.txt"));
//			fw.write(payload);
//			fw.close();
//		} catch(FileNotFoundException e) {
//			
//		}
//		
//		JSONObject responseObj = new JSONObject();
//		responseObj.put("message", "File written");
//		return new ResponseEntity(responseObj.toString(), responseHeaders, Http.Status.OK);
//		
//
//		
//		
//	}
//	
//
//
//}
