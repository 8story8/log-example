package io.saiden.exec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("io.saiden")
public class LogExampleServerLauncher {
  public static void main(final String[] args) {
    SpringApplication.run(LogExampleServerLauncher.class, args);
  }
}
