package com.hotel;

import com.hotel.bootstrap.AdminBootstrapCli;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@org.springframework.boot.autoconfigure.SpringBootApplication
public class AdminApiApplication {

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--bootstrap-admin")) {
            try (ConfigurableApplicationContext context =
                    new SpringApplicationBuilder(AdminApiApplication.class)
                            .web(WebApplicationType.NONE)
                            .run(args)) {
                System.exit(context.getBean(AdminBootstrapCli.class).run());
            }
        }
        SpringApplication.run(AdminApiApplication.class, args);
    }
}
