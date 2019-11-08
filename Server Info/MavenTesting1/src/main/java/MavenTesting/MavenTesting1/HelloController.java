package MavenTesting.MavenTesting1;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.*;

//@RestController line is needed so file knows we're listening to an endpoint
@RestController
public class HelloController {
	@RequestMapping(value = "/hello", method = RequestMethod.GET)
	public String hello(HttpServletRequest request) {
		String name = request.getParameter("name");
		String ageString = request.getParameter("age");
		
		int age = Integer.parseInt(ageString);
		if(age > 25) {
			return "You're old";
		}
		else {
			return "Not yet";
		}
		
		//return "Hi " + name;
	}
}
