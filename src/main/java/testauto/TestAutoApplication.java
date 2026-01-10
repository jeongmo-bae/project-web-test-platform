package testauto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TestAutoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestAutoApplication.class, args);
	}
}
