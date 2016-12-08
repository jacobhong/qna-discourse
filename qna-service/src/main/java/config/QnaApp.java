package config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by jacobhong on 12/8/16.
 */
@SpringBootApplication
@ComponentScan("controller")
public class QnaApp
{
    public static void main(String[] args)
    {
        SpringApplication.run(QnaApp.class, args);
    }
}
