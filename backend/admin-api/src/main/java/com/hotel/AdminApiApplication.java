package com.hotel;

import com.hotel.bootstrap.AdminBootstrapCli;
import java.sql.SQLException;
import java.util.Arrays;
import org.hibernate.HibernateException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataAccessException;

@org.springframework.boot.autoconfigure.SpringBootApplication
public class AdminApiApplication {

    public static void main(String[] args) {
        if (isBootstrapMode(args)) {
            try (ConfigurableApplicationContext context =
                    new SpringApplicationBuilder(AdminApiApplication.class)
                            .web(webApplicationType(args))
                            .run(args)) {
                System.exit(context.getBean(AdminBootstrapCli.class).run());
            } catch (RuntimeException ex) {
                System.exit(startupExitCode(ex));
            }
        }
        SpringApplication.run(AdminApiApplication.class, args);
    }

    static int startupExitCode(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof SQLException
                    || cause instanceof DataAccessException
                    || cause instanceof HibernateException) {
                return 3;
            }
            cause = cause.getCause();
        }
        return 1;
    }

    static boolean isBootstrapMode(String[] args) {
        return Arrays.asList(args).contains("--bootstrap-admin");
    }

    static WebApplicationType webApplicationType(String[] args) {
        return isBootstrapMode(args) ? WebApplicationType.NONE : WebApplicationType.SERVLET;
    }
}
